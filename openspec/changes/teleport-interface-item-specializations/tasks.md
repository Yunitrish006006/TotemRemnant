# Tasks: Teleport Interface Item Specializations

## 1. Interface foundation

- [ ] 1.1 新增 `TeleportInterfaceType`，對應普通羅盤、回生羅盤、書本與已繪製地圖。
- [ ] 1.2 建立共用 Server-side 介面物品解析器，禁止 Client 自行決定介面類型。
- [ ] 1.3 新增短期 `TeleportInterfaceContext`，保存來源、使用手、介面類型、map ID 與過期時間。
- [ ] 1.4 `TeleportSession` 保存已驗證的介面類型與必要 map ID，並在倒數及完成前重新驗證。
- [ ] 1.5 將「仍持有普通羅盤」取消條件改為「仍持有啟動該 session 的有效介面物品」。

## 2. Interaction routing

- [ ] 2.1 四種介面物品右鍵空氣時可用玩家當下位置開啟傳送介面。
- [ ] 2.2 四種介面物品右鍵已註冊磁石時可用該磁石作為來源開啟傳送介面。
- [ ] 2.3 普通羅盤繼續獨占未註冊磁石註冊、左鍵探索、好友邀請／接受與磁石管理操作。
- [ ] 2.4 非普通羅盤操作未註冊磁石或管理功能時回傳明確翻譯訊息，不建立 pending registration。
- [ ] 2.5 解決書本、地圖與方塊原版互動優先序，避免重複開啟或吃掉必要原版行為。

## 3. Quote specialization

- [ ] 3.1 普通羅盤使用現有成本、時間、偏差與磨損值，作為回歸基準。
- [ ] 3.2 回生羅盤對玩家自己的 `DEATH` 目標套用 `floor(baseDeviation × 0.50)`。
- [ ] 3.3 回生羅盤對非死亡目標不提供任何數值加成。
- [ ] 3.4 書本對已探索且可見的 `LODESTONE` 目標套用 20% 準備時間降低，最低 30 ticks。
- [ ] 3.5 書本對本次路線涉及的固定石碑磨損機率套用 25% 降低。
- [ ] 3.6 書本不得降低食物、紫水晶或偏差。
- [ ] 3.7 實作共用整數捨入、最小值與最大值 policy，避免折扣後出現非法數值。

## 4. Filled map coverage

- [ ] 4.1 從 `minecraft:filled_map` 的 map ID 取得 Server 地圖資料、Dimension、中心與比例尺。
- [ ] 4.2 實作目標座標是否位於地圖覆蓋範圍的純函式與邊界測試。
- [ ] 4.3 地圖 Dimension 不符、資料不存在或目標在範圍外時仍可開介面，但不提供加成。
- [ ] 4.4 地圖覆蓋目標時，食物等價成本套用 `ceil(baseFoodCost × 0.80)`，最低為 1。
- [ ] 4.5 地圖覆蓋目標時，偏差套用 `floor(baseDeviation × 0.80)`。
- [ ] 4.6 地圖不得降低跨 Dimension 紫水晶成本，不得覆寫催化折抵最低成本 1。
- [ ] 4.7 動態 `PLAYER` 目標在開始或完成前移出地圖範圍時取消 session 並要求重新報價。
- [ ] 4.8 地圖覆蓋檢查不得向 Client 洩漏好友精確座標。

## 5. Networking and UI

- [ ] 5.1 報價 Payload 加入介面類型與加成是否有效。
- [ ] 5.2 第二階段 Payload 加入成本、時間、偏差與磨損的 base／final 明細。
- [ ] 5.3 Client／Server codec 同步並限制 enum、字串與數值範圍。
- [ ] 5.4 地圖資訊面板顯示目前介面物品、特化名稱、有效條件與最終效果。
- [ ] 5.5 非普通羅盤介面隱藏或停用註冊、探索、好友與磁石管理入口。
- [ ] 5.6 新增英文、繁體中文與簡體中文翻譯。

## 6. Tests

- [ ] 6.1 四種物品解析與不支援物品拒絕的單元測試。
- [ ] 6.2 普通羅盤報價與現有公式完全相同的回歸測試。
- [ ] 6.3 回生羅盤對 `DEATH`／非 `DEATH` 目標的偏差矩陣測試。
- [ ] 6.4 書本對固定／非固定目標的時間與磨損矩陣測試。
- [ ] 6.5 地圖中心、邊界內、邊界外、比例尺與 Dimension 不符測試。
- [ ] 6.6 地圖食物成本最低值、偏差捨入與紫水晶不變測試。
- [ ] 6.7 報價後更換介面物品、移除 map ID 或目標移出覆蓋範圍的 session 測試。
- [ ] 6.8 未探索、無權限與好友 `PLAYER` 目標隱私測試。
- [ ] 6.9 Java 25 完整 build 與 Dedicated Server 啟動／Mixin 煙霧測試。
- [ ] 6.10 兩名以上真實玩家的 UI、動態目標與多人回歸測試。

## 7. Delivery schedule

### Phase A — Common interface support

- [ ] 7.1 完成介面類型、Server context、四物品開啟 UI 與普通羅盤專屬能力分流。
- [ ] 7.2 暫時讓四種物品都使用普通羅盤基準報價，先驗證相容性。

### Phase B — Static specializations

- [ ] 7.3 實作回生羅盤死亡節點偏差降低。
- [ ] 7.4 實作書本固定磁石準備時間與磨損降低。
- [ ] 7.5 加入第一階段 UI 標示與單元測試。

### Phase C — Filled map specialization

- [ ] 7.6 實作 map ID、Dimension、中心、比例尺與覆蓋範圍解析。
- [ ] 7.7 實作地圖食物成本／偏差降低與動態目標重新驗證。
- [ ] 7.8 完成好友位置隱私與地圖邊界測試。

### Phase D — Quote details and release validation

- [ ] 7.9 擴充 Payload 與 GUI 顯示 base／final 明細。
- [ ] 7.10 完成 Dedicated Server、多人、跨 Dimension、死亡節點與舊世界回歸。
- [ ] 7.11 更新玩家文件與版本變更紀錄。