# Design: Death Backpack Pre-Drop Capture

## 1. Injection point

使用 `Player.dropEquipment(ServerLevel)` 中呼叫 `Inventory.dropAll()` 前的 Mixin 注入點。

此時原版已完成：

- 判斷 `keepInventory=false`。
- 移除 Inventory／Equipment 中具有原版防掉落／消失效果的物品。

但尚未：

- 生成任何玩家 Inventory 死亡 ItemEntity。
- 清空玩家 Inventory。

因此這是能同時維持原版語意並避免世界掉落競態的最窄切入點。游標與玩家 2×2 crafting inputs 不屬於 Inventory，服務會在此切點以相同的 `PREVENT_EQUIPMENT_DROP` 判斷補上消失詛咒處理。

## 2. Capture plan

### Inventory 與 Equipment

從 `player.getInventory()` 的 `0 .. getContainerSize()-1` 建立 `CapturedSlot(slot, stackCopy)`：

- 空 stack：忽略。
- `BackpackItemHelper.isBackpackItem(stack)`：忽略，保留在 Inventory，稍後由原版掉落。
- 其他 stack：加入死亡背包內容。

### Transient player-owned stacks

另外建立 transient 快照：

- `player.containerMenu.getCarried()`：目前 active menu 的游標 stack。
- `player.inventoryMenu.getCraftSlots()`：玩家固定 2×2 crafting inputs。

處理規則：

- 一般物品：加入同一個死亡背包 transaction。
- `PREVENT_EQUIPMENT_DROP`／消失詛咒：直接從 transient source 清除，不保留也不掉落。
- DeadRecall 一般背包或死亡背包：不得巢狀，直接生成獨立世界掉落物；若實體建立失敗則安全回填 Inventory，交給原版 `dropAll()`。
- 外部箱子等持久容器的實際 storage slots：不讀取、不修改、不擷取。

目前仍不直接處理：

- crafting table、anvil、smithing table 等外部工作站自己的暫存 input slots。
- Trinkets、Accessories 等第三方 inventory API。

## 3. Transaction order

正常流程：

1. 建立 Inventory／Equipment、active cursor 與玩家 2×2 crafting inputs 的不可變快照。
2. 清除 transient 消失詛咒物品；將 transient DeadRecall 背包改為獨立世界掉落。
3. 建立死亡背包 ItemStack，寫入唯一 ID 與 `DataComponents.CONTAINER`。
4. 暫時從玩家 Inventory 與 transient sources 移除捕獲 stack。
5. 生成死亡背包 ItemEntity。
6. 建立死亡 Space Unit 節點並寫入背包 binding。
7. 原版 `Inventory.dropAll()` 繼續執行，只掉落未捕獲的 Inventory 背包類物品。
8. `AFTER_DEATH` 不再執行任何附近掉落物收集，也不保留 completed-capture 狀態 Set。
9. Discord 與玩家通知在交易完成後執行，通知失敗只記錄警告，不得回滾背包或物品。

失敗流程：

1. 若已建立死亡節點，將節點設為 `DISABLED` 並移除該玩家的探索／收藏引用。
2. 若已生成死亡背包實體，立即 discard。
3. 將 Inventory CapturedSlot copy 還原到原槽位；若槽位已被占用，使用 Inventory 安全回填。
4. 將捕獲的 cursor／2×2 crafting stack 回填 Inventory，而不是放回 transient source，確保接續的原版 `Inventory.dropAll()` 能生成世界掉落物。
5. 原版 `Inventory.dropAll()` 正常掉落還原後的物品。
6. 不執行任何附近 ItemEntity 掃描或死亡背包 fallback，避免重新引入誤收與跨玩家競態。

## 4. Data integrity invariants

- 每個被捕獲 stack 只存在於原始 player-owned source、rollback Inventory 或死亡背包其中之一。
- 正常完成後，捕獲的 Inventory、游標與 2×2 crafting sources 必須為空，不得再次掉落同一 stack。
- 失敗時死亡背包實體不得留在世界，所有捕獲 stack 必須可由接續的原版 `dropAll()` 找到或已安全生成為世界掉落。
- 失敗後不得留下可見／可傳送的 ACTIVE 死亡節點或探索引用。
- 背包類物品不得成為另一個死亡背包的內容。
- 外部持久容器 storage slots 不屬於玩家死亡交易，不得被讀取、移除或寫回。
- ItemStack 使用 `copy()` 保存完整 Data Components，不經 ItemEntity 合併或反序列化回注。
- Client 不得決定或修改捕獲內容。
- 通知、Discord 或記錄輸出不是交易的一部分，不得影響物品提交狀態。

## 5. Legacy migration

以下舊流程已從 `Deadrecall` 程式碼與 Mixin 設定中完整刪除：

- `rememberExistingDropsBeforeDeath`。
- 舊 `handlePlayerDeath` nearby-drop collector。
- 半徑 10 格 AABB 掃描。
- 既有 ItemEntity UUID 差集。
- 雙重 `server.execute` 排程。
- 掃描後 `ItemEntity.discard()` 回收。
- `pendingDeathCollections`、`scheduledDeathBackpackCollections`、`PendingDeathCollection` 與相關常數。
- 僅用於停用上述死碼的 `DeadrecallDeathBackpackMixin`。
- Legacy Mixin 刪除後已無消費者的 `COMPLETED_CAPTURES` Set 與 `consumeCompletedCapture`。

死亡背包現在只有 `Player.dropEquipment` 前的直接擷取路徑。直接擷取失敗時只保證物品回到原版掉落，不再嘗試從世界掉落物建立替代死亡背包。

## 6. Test strategy

### Automated

- Java 25 compile 與 Mixin annotation processing。
- Fabric Server GameTest 啟動，確認 `Player.dropEquipment` 注入成功套用。
- Inventory capture policy：一般物品捕獲、背包排除、空 stack 忽略。
- 失敗注入測試：實體加入後失敗時，槽位完整還原且未完成 ItemEntity 被丟棄。
- 死亡節點後失敗測試：節點停用、探索引用移除、槽位還原。
- `keepInventory=true` 與消失詛咒原版語意。
- 64 格、自訂名稱、耐久、盔甲、副手等 Components 保存。
- active menu cursor 與玩家 2×2 crafting inputs 擷取。
- 外部箱子持久 storage 不得被擷取或修改。
- transient 背包獨立掉落、transient 消失詛咒銷毀。
- transient transaction 故障後必須回到原版世界掉落。
- 同位置既有 ItemEntity 不得被修改或收集。
- 兩名玩家同位置、同 tick 死亡時，各自背包內容不得交叉。
- 直接擷取失敗時，原版掉落必須存在，且不得建立第二條死亡背包路徑。
- Legacy scanner 實體刪除後完整 Java 25 build 與 Server GameTests。

### Integration

- 岩漿、仙人掌、虛空、爆炸及死亡點高密度掉落物。
- 只持有一般／死亡背包時的原版掉落。
- 外部工作站暫存 input slots 與第三方飾品槽。
- Server restart、斷線、重生及死亡節點回收。
- 與第三方墓碑、keep inventory 與飾品模組的事件優先序。
