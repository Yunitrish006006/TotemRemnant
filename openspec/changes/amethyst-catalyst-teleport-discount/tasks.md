# Tasks: Amethyst Catalyst Teleport Discount

## 1. Data and scanning

- [x] 1.1 新增 `space_unit_amethyst_catalysts` block tag，初版包含 `minecraft:amethyst_block`。
- [x] 1.2 `SpaceStructureSnapshot` 加入 optional `amethystCatalystBlocks`，並保留七參數相容建構子。
- [x] 1.3 石碑掃描器在既有 `5×3×5` 掃描期間計算有效催化方塊，不增加每 tick 全域掃描。
- [x] 1.4 舊 SavedData 缺少欄位時由 codec 預設為 0。

## 2. Quote and payment

- [x] 2.1 將原始紫水晶成本、催化數量、折抵與最終成本分離到正式報價模型。
- [x] 2.2 實作每 4 個催化方塊折抵 1、跨維度最終成本最低為 1。
- [x] 2.3 只讓固定磁石來源與固定磁石目標提供折抵；玩家、死亡及其他非石碑端點貢獻 0。
- [x] 2.4 沿用開始與完成傳送前的定向重新掃描及重新報價。
- [x] 2.5 現有扣款流程使用 Server 報價中的最終 `amethystCost`。

## 3. Networking and UI

- [x] 3.1 Payload 加入 base cost、catalyst count 與 discount。
- [x] 3.2 地圖資訊面板顯示「原始成本 - 石碑折抵 = 最終成本」。
- [ ] 3.3 Client／Server codec 同步並加上長度與範圍限制。
- [x] 3.4 第一階段沿用既有 `amethystCost` 欄位回傳最終成本，避免先破壞 Client／Server codec。

## 4. Tests

- [x] 4.1 0、3、4、8、12 以上催化方塊成本矩陣單元測試。
- [ ] 4.2 玩家來源、玩家目標與死亡節點不錯誤提供折抵的整合測試。
  - [x] 4.2a 報價端點資格單元矩陣：玩家來源、玩家目標與死亡節點的儲存催化數一律正規化為 0。
- [ ] 4.3 報價後拆除水晶，啟動或完成時成本更新的遊戲內測試。
- [ ] 4.4 舊世界 snapshot 實際載入 migration 測試。
  - [x] 4.4a 使用 legacy JSON 經正式 `SpaceStructureSnapshot.CODEC` 載入，確認缺少催化欄位時預設為 0。
- [x] 4.5 Java 25 `./gradlew build` 通過。
- [x] 4.6 Dedicated Server 成功啟動到 `Done`，催化相關 Mixin 設定沒有啟動期錯誤。
- [ ] 4.7 實際跨維度報價、扣款與兩端催化方塊整合測試。
