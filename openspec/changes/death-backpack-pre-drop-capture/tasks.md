# Tasks: Death Backpack Pre-Drop Capture

## 1. Specification and API verification

- [x] 1.1 確認 Minecraft 26.2 `Player.dropEquipment(ServerLevel)` 在 `keepInventory=false` 且消失詛咒處理後呼叫 `Inventory.dropAll()`。
- [x] 1.2 定義第一階段擷取範圍為原版 Inventory 與 Equipment slots。
- [x] 1.3 定義所有 DeadRecall 背包排除並交由原版掉落。
- [x] 1.4 確認 active menu 游標 stack、玩家 2×2 crafting slots 與外部持久容器槽位的死亡語意。
- [ ] 1.5 確認外部工作站暫存輸入槽與第三方飾品槽的死亡語意。

## 2. Direct capture implementation

- [x] 2.1 新增 `DeathBackpackCaptureService` 與不可變 CapturedSlot 快照。
- [x] 2.2 在 `Inventory.dropAll()` 前執行 Server-side 直接擷取。
- [x] 2.3 使用 stack copy 寫入 `DataComponents.CONTAINER`，不經世界 ItemEntity 回收。
- [x] 2.4 保留死亡背包唯一 ID、死亡節點 binding、拾取延遲與 unlimited lifetime。
- [x] 2.5 成功後只讓原版掉落排除的背包類物品。
- [x] 2.6 例外時 discard 未完成的死亡背包並還原玩家槽位。
- [x] 2.7 通知與 Discord 失敗不得回滾已完成的死亡背包交易。
- [x] 2.8 死亡節點建立後失敗時停用節點並移除探索引用。
- [x] 2.9 將 active menu 游標與玩家 2×2 crafting inputs 納入同一 transaction；暫存背包改為獨立掉落，暫存消失詛咒物品直接銷毀。
- [x] 2.10 移除 legacy scanner 刪除後已無消費者的 completed-capture Set。

## 3. Legacy compatibility

- [x] 3.1 直接擷取成功時取消舊 `handlePlayerDeath` 掃描器。
- [x] 3.2 死亡前的附近 ItemEntity UUID 掃描已在執行期停用。
- [x] 3.3 直接擷取失敗時只回到原版 `Inventory.dropAll()`，不再執行 nearby-drop fallback。
- [x] 3.4 舊半徑掃描器、雙重 server task 與 UUID 差集已從實際死亡路徑停用。
- [x] 3.5 已從 `Deadrecall` 刪除舊半徑常數、Map、Set、record、雙重 task、掃描方法與相容 Mixin。

## 4. Automated tests

- [x] 4.1 一般物品與空 stack 擷取 policy 單元測試；背包排除由 Server GameTest 驗證。
- [x] 4.2 64 格、耐久、自訂名稱、裝備與副手 Components 保存 GameTest。
- [ ] 4.3 第三方自訂 Components 保存測試。
- [x] 4.4 實體加入後失敗與死亡節點建立後失敗的槽位 rollback 測試。
- [x] 4.5 Inventory 與 Equipment 混合 GameTest；確認捕獲物不生成世界 ItemEntity、排除背包由原版掉落且只建立一個死亡背包。
- [x] 4.6 `keepInventory=true` 與消失詛咒 GameTest。
- [x] 4.7 Java 25 build、Fabric GameTest Server 與相關 Mixin 的實際套用驗證。
- [x] 4.8 rollback 後由原版生成掉落物，且 legacy 掃描器不會再次建立死亡背包。
- [x] 4.9 游標、2×2 crafting inputs、外部箱子隔離、暫存背包排除、暫存消失詛咒與 transient rollback GameTest。

## 5. Integration tests

- [x] 5.1 同位置存在其他 ItemEntity 時不得誤收。
- [x] 5.2 兩名玩家同 tick、同位置死亡不得互相收取。
- [x] 5.3 岩漿、仙人掌、虛空與爆炸死亡回歸。
- [x] 5.4 只持有一般／死亡背包時維持原版掉落且不建立巢狀背包或死亡節點。
- [x] 5.5 active menu 游標與玩家 2×2 crafting inputs 會被擷取；外部箱子實際儲存槽不得被擷取或修改。
- [ ] 5.6 外部工作站暫存輸入槽與第三方飾品模組相容測試。
- [ ] 5.7 Server restart、斷線、重生及死亡節點回收測試。

## 6. Documentation and delivery

- [x] 6.1 建立 proposal、design、tasks 與 delta spec。
- [x] 6.2 更新玩家文件，說明死亡背包在掉落生成前直接封裝與原版 fallback。
- [ ] 6.3 更新 changelog／版本變更清單。
- [x] 6.4 更新 Roadmap 狀態。
