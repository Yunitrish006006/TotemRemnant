# Tasks: Concrete Powder Item Hardening

## 1. Version and API verification

- [x] 1.1 確認 Minecraft 26.2、modern-yarn、Fabric Loader、Fabric API 與 Java 25 版本。
- [x] 1.2 使用目前 mappings 的 `ItemEntity.tick` 與 `Entity.isInWater()` 判定實際浸水。
- [x] 1.3 使用 `ItemStack.transmuteCopy` 保留可相容 Components。
- [x] 1.4 沿用現有 `ItemEntityMixin`，並與死亡背包保護／內容掉落流程使用獨立注入點。

## 2. Concrete mapping

- [x] 2.1 建立 16 種原版混凝土粉末到同色混凝土的不可變映射。
- [x] 2.2 加入映射完整性單元測試，確認全部 16 色對應。
- [x] 2.3 非支援物品使用 Map 查詢後以常數時間返回。

## 3. Server-side transformation

- [x] 3.1 在單一 ItemEntity 的 Server-side tick 尾端執行，不做世界全量掃描。
- [x] 3.2 每次轉換前重新讀取目前 stack，只有支援粉末才繼續。
- [x] 3.3 使用原版 `isInWater()`，只在實體實際接觸水時觸發。
- [x] 3.4 使用同一 ItemEntity 原子替換成對應混凝土 stack。
- [x] 3.5 使用 `transmuteCopy` 保留 count 與合法 Components；ItemEntity 本身保留位置、速度、Owner、pickup delay 與 age。
- [x] 3.6 只有 `ServerLevel` 執行 ItemStack 修改，Client side 不轉換。
- [x] 3.7 不建立第二個 ItemEntity，不產生複製或遺失窗口。

## 4. Edge cases

- [x] 4.1 水源已由 Fabric Server GameTest 驗證。
- [x] 4.2 非水源流動水已由 Fabric Server GameTest 驗證。
- [x] 4.3 隔著方塊靠近水但未接觸時不轉換，已由 GameTest 驗證。
- [x] 4.4 有天空且世界正在下雨、但實體未浸水時不轉換，已由 GameTest 驗證。
- [x] 4.5 只掛在世界 ItemEntity tick，物品欄、容器、背包與銅傀儡內不轉換。
- [x] 4.6 已轉成混凝土後映射查詢失敗，不會重複執行。
- [x] 4.7 每 tick 重新讀取目前 stack，其他模組先修改後可安全退出或依新值轉換。

## 5. Tests

- [x] 5.1 16 色對應單元測試。
- [x] 5.2 完整 64 個 stack 數量保存 GameTest。
- [x] 5.3 自訂名稱 Component 保存 GameTest；其他可相容 Components 由 `transmuteCopy` 契約保留。
- [x] 5.4 水源、流動水、非接觸水與雨天 Server GameTest。
- [x] 5.5 同一 ItemEntity identity、age、位置範圍、速度與 pickup delay GameTest。
- [x] 5.6 Dedicated Server 可成功啟動到 `Done`，`ItemEntityMixin` 沒有啟動期套用錯誤。
- [x] 5.7 Fabric Loom `gametest` source set、測試模組 entrypoint 與 `runGameTest` 已接入 `build`；5 個 required tests 全部通過。
- [ ] 5.8 兩名以上真實玩家同時將物品丟入水流的多人測試。
- [ ] 5.9 大量 ItemEntity 壓力測試，確認沒有全世界 entity 掃描及明顯 tick 延遲。

## 6. Documentation and release

- [x] 6.1 更新玩家功能文件。
- [ ] 6.2 更新 changelog／版本變更清單。
- [x] 6.3 記錄水鍋不在第一版範圍內。
- [x] 6.4 執行 `./gradlew build`；Java 25 CI、JUnit 與 Server GameTests 通過。
- [x] 6.5 執行 Dedicated Server 與 Fabric GameTest 回歸；只剩真人多人與大量實體壓力測試。
- [x] 6.6 將自動驗證結果與剩餘風險同步到 `openspec/roadmap.md`。
