# Tasks: Death Backpack Pre-Drop Capture

## 1. Specification and API verification

- [x] 1.1 確認 Minecraft 26.2 `Player.dropEquipment(ServerLevel)` 在 `keepInventory=false` 且消失詛咒處理後呼叫 `Inventory.dropAll()`。
- [x] 1.2 定義第一階段擷取範圍為原版 Inventory 與 Equipment slots。
- [x] 1.3 定義所有 DeadRecall 背包排除並交由原版掉落。
- [ ] 1.4 確認游標 stack、2×2 crafting slots 與第三方飾品槽的死亡語意。

## 2. Direct capture implementation

- [x] 2.1 新增 `DeathBackpackCaptureService` 與不可變 CapturedSlot 快照。
- [x] 2.2 在 `Inventory.dropAll()` 前執行 Server-side 直接擷取。
- [x] 2.3 使用 stack copy 寫入 `DataComponents.CONTAINER`，不經世界 ItemEntity 回收。
- [x] 2.4 保留死亡背包唯一 ID、死亡節點 binding、拾取延遲與 unlimited lifetime。
- [x] 2.5 成功後只讓原版掉落排除的背包類物品。
- [x] 2.6 例外時 discard 未完成的死亡背包並還原玩家槽位。

## 3. Legacy compatibility

- [x] 3.1 新增完成標記，讓直接擷取成功時取消舊 `handlePlayerDeath` 掃描器。
- [x] 3.2 直接擷取成功時清除 pending nearby-drop snapshot。
- [x] 3.3 直接擷取失敗時保留原版掉落與舊掃描 fallback。
- [ ] 3.4 多人與模組相容驗證後刪除舊半徑、Map、Set、雙重 server task 與 ItemEntity UUID 差集。

## 4. Automated tests

- [ ] 4.1 一般物品、空 stack 與背包排除 policy 單元測試。
- [ ] 4.2 64 格、耐久、附魔、自訂名稱與自訂 Components 保存測試。
- [ ] 4.3 實體建立失敗／死亡節點建立失敗的槽位 rollback 測試。
- [ ] 4.4 Inventory 與 Equipment 混合 GameTest。
- [ ] 4.5 `keepInventory=true` 與消失詛咒 GameTest。
- [ ] 4.6 Java 25 build、Fabric GameTest Server 與 Mixin 套用驗證。

## 5. Integration tests

- [ ] 5.1 同位置存在其他 ItemEntity 時不得誤收。
- [ ] 5.2 兩名玩家同 tick、同位置死亡不得互相收取。
- [ ] 5.3 岩漿、仙人掌、虛空與爆炸死亡回歸。
- [ ] 5.4 只持有一般／死亡背包時維持原版掉落且不建立巢狀背包。
- [ ] 5.5 游標、玩家 crafting slots、外部容器與第三方飾品模組相容測試。
- [ ] 5.6 Server restart、斷線、重生及死亡節點回收測試。

## 6. Documentation and delivery

- [x] 6.1 建立 proposal、design、tasks 與 delta spec。
- [ ] 6.2 更新玩家文件，說明死亡背包在掉落生成前直接封裝。
- [ ] 6.3 更新 changelog／版本變更清單。
- [ ] 6.4 完成第一階段後更新 Roadmap 狀態。
