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
8. `AFTER_DEATH` 不再執行任何附近掉落物收集。
9. Discord 與玩家通知在交易完成後執行，通知失敗只記錄警告，不得回滾背包或物品。

失敗流程：

1. 若已建立死亡節點，將節點設為 `DISABLED` 並移除該玩家的探索／收藏引用。
2. 若已生成死亡背包實體，立即 discard。
3. 將所有 CapturedSlot copy 還原到原槽位；若槽位已被其他流程占用，使用 Inventory 的安全回填流程。
4. 不寫入完成標記。
5. 原版 `Inventory.dropAll()` 正常掉落還原後的物品。
6. 不執行任何附近 ItemEntity 掃描或死亡背包 fallback，避免重新引入誤收與跨玩家競態。

## 4. Data integrity invariants

- 每個被捕獲 stack 只存在於玩家 Inventory 或死亡背包其中之一。
- 正常完成後，捕獲槽位必須為空，原版不得再次掉落同一 stack。
- 失敗時死亡背包實體不得留在世界，玩家槽位必須恢復。
- 失敗後不得留下可見／可傳送的 ACTIVE 死亡節點或探索引用。
- 背包類物品不得成為另一個死亡背包的內容。
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
- 同位置既有 ItemEntity 不得被修改或收集。
- 兩名玩家同位置、同 tick 死亡時，各自背包內容不得交叉。
- 直接擷取失敗時，原版掉落必須存在，且不得建立第二條死亡背包路徑。
- Legacy scanner 實體刪除後完整 Java 25 build 與 Server GameTests。

### Integration

- 岩漿、仙人掌、虛空、爆炸及死亡點高密度掉落物。
- 只持有一般／死亡背包時的原版掉落。
- 游標 stack、玩家 crafting slots、外部容器與第三方飾品槽。
- Server restart、斷線、重生及死亡節點回收。
- 與第三方墓碑、keep inventory 與飾品模組的事件優先序。
