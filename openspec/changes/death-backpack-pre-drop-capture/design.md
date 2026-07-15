# Design: Death Backpack Pre-Drop Capture

## 1. Injection point

使用 `Player.dropEquipment(ServerLevel)` 中呼叫 `Inventory.dropAll()` 前的 Mixin 注入點。

此時原版已完成：

- 判斷 `keepInventory=false`。
- 移除具有原版防掉落／消失效果的物品。

但尚未：

- 生成任何玩家死亡 ItemEntity。
- 清空玩家 Inventory。

因此這是能同時維持原版語意並避免世界掉落競態的最窄切入點。

## 2. Capture plan

從 `player.getInventory()` 的 `0 .. getContainerSize()-1` 建立 `CapturedSlot(slot, stackCopy)`：

- 空 stack：忽略。
- `BackpackItemHelper.isBackpackItem(stack)`：忽略，保留在 Inventory，稍後由原版掉落。
- 其他 stack：加入死亡背包內容。

第一階段不直接處理：

- `AbstractContainerMenu#getCarried()` 游標 stack。
- 玩家 2×2 crafting slots。
- 外部容器 slots。
- Trinkets、Accessories 等第三方 inventory API。

## 3. Transaction order

正常流程：

1. 建立 CapturedSlot 快照。
2. 建立死亡背包 ItemStack，寫入唯一 ID 與 `DataComponents.CONTAINER`。
3. 暫時從玩家 Inventory 移除捕獲槽位。
4. 生成死亡背包 ItemEntity。
5. 建立死亡 Space Unit 節點並寫入背包 binding。
6. 將完成標記寫入短期 Server-side Set。
7. 原版 `Inventory.dropAll()` 繼續執行，只掉落未捕獲的背包類物品。
8. `AFTER_DEATH` 進入舊 `handlePlayerDeath` 時消耗完成標記並取消舊掃描器。

失敗流程：

1. 若已生成死亡背包實體，立即 discard。
2. 將所有 CapturedSlot copy 還原到原槽位；若槽位已被其他流程占用，使用 Inventory 的安全回填流程。
3. 不寫入完成標記。
4. 原版 `Inventory.dropAll()` 正常掉落還原後的物品。
5. 舊 post-drop collector 仍可作為相容 fallback。

## 4. Data integrity invariants

- 每個被捕獲 stack 只存在於玩家 Inventory 或死亡背包其中之一。
- 正常完成後，捕獲槽位必須為空，原版不得再次掉落同一 stack。
- 失敗時死亡背包實體不得留在世界，玩家槽位必須恢復。
- 背包類物品不得成為另一個死亡背包的內容。
- ItemStack 使用 `copy()` 保存完整 Data Components，不經 ItemEntity 合併或反序列化回注。
- Client 不得決定或修改捕獲內容。

## 5. Legacy migration

第一階段保留：

- `pendingDeathCollections`
- `scheduledDeathBackpackCollections`
- `rememberExistingDropsBeforeDeath`
- 舊 `handlePlayerDeath` 掃描器

新增 `DeadrecallDeathBackpackMixin`：

- 直接擷取成功時取消舊 handler。
- 清除該玩家的 pending drop snapshot。
- 直接擷取失敗時不取消，保留現有 fallback。

第二階段在多人與模組相容測試通過後，刪除以上 legacy 程式與 AABB 掃描。

## 6. Test strategy

### Automated

- Java 25 compile 與 Mixin annotation processing。
- Fabric Server GameTest 啟動，確認 `Player.dropEquipment` 與 private `handlePlayerDeath` 注入成功。
- Inventory capture policy：一般物品捕獲、背包排除、空 stack 忽略。
- 失敗注入測試：實體建立失敗時槽位完整還原。
- 同一 stack 不同 Components 必須原樣保存。

### Integration

- 主物品欄、快捷列、盔甲、副手混合死亡。
- 消失詛咒、keepInventory、空背包、只持有背包。
- 岩漿、仙人掌、虛空、爆炸及死亡點高密度掉落物。
- 兩名玩家同位置同 tick 死亡，不得互相誤收。
- 與第三方墓碑、keep inventory、飾品模組的事件優先序。
