# Death Backpack Addon Inventory API

DeadRecall 2.4.1 提供公開的 Server-side transaction SPI，讓飾品模組或 addon 把「玩家死亡時應掉落」的自訂槽位納入死亡背包，而不需要讓 DeadRecall 掃描未知 Menu 或反射第三方私有 inventory。

## Public API

### `DeathBackpackAddonInventoryProvider`

每個整合註冊一個 provider：

```java
DeathBackpackAddonInventoryRegistry.register(provider);
```

Provider 必須：

- 回傳全域唯一且穩定的 `Identifier`。
- 只回傳屬於指定 `ServerPlayer`、且依 addon 自己規則應參與死亡掉落的槽位。
- 使用穩定順序回傳槽位。
- 不回傳 `null` list、`null` slot 或重複 source。
- 不在 `collectDroppableSlots` 中修改 inventory。

Registry 保留註冊順序並回傳 immutable snapshot。重複 provider ID 會直接拋出 `IllegalArgumentException`。

### `DeathBackpackAddonSlot`

每個 slot 提供四個 operation：

- `sourceKey()`：provider 內唯一且穩定的來源識別。
- `snapshot()`：回傳目前 stack 的 copy；不得暴露可變 live reference。
- `clearIfUnchanged(expected)`：只有目前 Item、count 與 Components 仍等於 snapshot 時才能原子清空。
- `restoreIfEmpty(stack)`：rollback 時只在來源仍空白時原位復原。

所有 callback 都在 Minecraft Server thread 執行。Addon 不應自行排程到其他 thread，也不應在 callback 中發送 Client-authoritative mutation。

## Transaction semantics

Addon slots 與原版 Inventory、Equipment、游標、玩家 crafting inputs 及白名單工作站 inputs 共用同一個死亡 transaction：

1. DeadRecall 收集所有 immutable snapshots。
2. 建立死亡背包內容。
3. 清除原版與 transient sources。
4. 依 provider 註冊順序執行 addon `clearIfUnchanged`。
5. 所有來源成功提交後，才建立死亡背包實體與 Death Node binding。

任一 addon slot 拒絕 commit 或拋出例外時：

- 未完成死亡背包實體會被移除。
- 已建立的 Death Node 會停用並移除 discovery reference。
- Inventory slots 還原。
- Transient stacks 回填 Inventory。
- 已清除的 addon slots 以反向順序呼叫 `restoreIfEmpty`。
- 原位復原失敗的 stack 回填 Inventory，交由原版 `Inventory.dropAll()` 生成世界掉落。

不得在 rollback 中靜默刪除物品。

Provider snapshot 階段拋例外時，只隔離該 provider並記錄警告；其他 provider 與原版 Inventory 擷取仍可繼續。

## Portable-container policy

所有 addon stack 都經過與原版 Inventory 相同的 `PortableContainerPolicy`：

- DeadRecall 一般背包。
- 死亡背包。
- 其他被標記為禁止巢狀的可攜式容器。

這些物品不會被塞入死亡背包。通用 SPI 不會擅自清除被排除的 addon slot；其後續死亡語意仍由擁有該 slot 的 addon 處理。

## Trinkets Updated 4.1.x

DeadRecall 內建 Minecraft 26.2 的 optional adapter：

- Fabric mod ID：`trinkets_updated`。
- 使用 `TrinketsApi.getAttachment(player).forEachDroppable(..., false)`。
- 只擷取 Trinkets 最終判定為 `DROP` 的 slots。
- `KEEP` 與 `DESTROY` 由 Trinkets 的 slot rule、callback、enchantment 與 keepInventory 邏輯處理，DeadRecall 不重新解釋。
- Commit 前比對 Item、count 與完整 Components。
- Adapter 只有在 Loader 偵測到 Trinkets Updated 後才反射載入，避免未安裝時形成 class-linkage 硬依賴。

`fabric.mod.json` 只以 `suggests` 宣告 Trinkets Updated。Trinkets Updated 與 Yumi 會出現在開發／GameTest runtime，但不會被 DeadRecall 宣告為正式伺服器必要模組。

## Other addons

Accessories 目前沒有 Minecraft 26.2 的直接編譯版本，因此 DeadRecall 不維護舊版 Accessories 反射 adapter。Accessories compatibility layer 或其他 addon 可直接實作公開 SPI，並依自己的死亡規則只暴露 droppable player-owned slots。

建議每個整合至少提供下列 GameTests：

- 成功提交並保存 count／Components。
- `KEEP`／`DESTROY` 或等效規則不被 provider 回傳。
- Commit 前來源改變時拒絕清除。
- 部分提交後的反向 rollback。
- Source 失效時回填 Inventory fallback。
- Portable container 排除。
- 同 tick 多玩家隔離。

## Built-in regression coverage

```text
DeathBackpackAddonInventoryRegistryTest
DeathBackpackAddonInventoryGameTest
DeathBackpackTrinketsGameTest
```

自動驗證包含：

- Registry order、immutable snapshot 與 duplicate ID 拒絕。
- 通用 SPI 的成功擷取、Components、部分 commit rollback、provider snapshot 例外隔離與可攜式容器排除。
- Trinkets Updated 實際 runtime、adapter 註冊、真實 player `DROP` slot、source 清空、Components 保存與 exactly-once 世界結果。
