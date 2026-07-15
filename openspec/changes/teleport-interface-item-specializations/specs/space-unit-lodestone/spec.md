# Space Unit Lodestone Delta Specification

## ADDED Requirements

### Requirement: Supported teleport interface items

普通羅盤、回生羅盤、書本與已繪製地圖 SHALL 可作為 Space Unit 傳送介面入口。Server SHALL 從玩家實際手持 ItemStack 解析介面類型，Client SHALL NOT 提交可信的介面類型或特化數值。

#### Scenario: Open from player source

- **GIVEN** 玩家手持任一支援的傳送介面物品
- **WHEN** 玩家右鍵空氣
- **THEN** Server SHALL 以玩家當下位置建立 `PLAYER` 來源 context
- **AND** SHALL 開啟傳送介面

#### Scenario: Open from registered lodestone

- **GIVEN** 玩家手持任一支援的傳送介面物品
- **AND** 玩家位於已註冊磁石的有效互動距離內
- **WHEN** 玩家右鍵該磁石
- **THEN** Server SHALL 以該 `LODESTONE` 作為來源開啟傳送介面

#### Scenario: Unsupported item

- **GIVEN** 玩家手持不支援的物品
- **WHEN** 玩家嘗試開啟傳送介面
- **THEN** Server SHALL NOT 建立傳送介面 context

### Requirement: Compass-exclusive management capabilities

只有普通羅盤 SHALL 可註冊未註冊磁石、左鍵探索磁石、右鍵玩家處理好友邀請，以及執行磁石管理操作。其他傳送介面物品 SHALL 只取得傳送介面能力，不得因可開啟介面而取得管理權限。

#### Scenario: Book on unregistered lodestone

- **GIVEN** 玩家手持書本
- **WHEN** 玩家右鍵未註冊磁石
- **THEN** Server SHALL NOT 建立 pending registration
- **AND** SHALL 提示需要普通羅盤

#### Scenario: Non-compass management request

- **GIVEN** 玩家以回生羅盤、書本或已繪製地圖開啟介面
- **WHEN** Client 提交磁石改名、可見性、名單或校準操作
- **THEN** Server SHALL 拒絕該操作
- **AND** SHALL NOT 只依玩家原本的 Owner 或 Administrator 身分放行

### Requirement: Interface-bound teleport session

Server SHALL 在介面開啟、報價、傳送開始與完成前重新驗證啟動物品。傳送 session SHALL 保存介面類型；已繪製地圖 SHALL 額外保存並驗證 map ID。同一趟傳送 SHALL NOT 疊加另一隻手中的介面物品效果。

#### Scenario: Item changed after quote

- **GIVEN** 玩家使用書本取得特化報價
- **WHEN** 玩家在開始傳送前改持普通羅盤
- **THEN** Server SHALL NOT 沿用書本加成
- **AND** SHALL 要求重新報價或重新建立介面 context

#### Scenario: Interface removed during countdown

- **GIVEN** 玩家已使用回生羅盤開始傳送 session
- **WHEN** 玩家不再持有相符的有效介面物品
- **THEN** Server SHALL 取消傳送
- **AND** SHALL NOT 完成傳送或扣除資源

### Requirement: Ordinary compass baseline

普通羅盤 SHALL 使用既有 Space Unit 成本、準備時間、偏差與結構磨損公式，不提供數值折扣。

#### Scenario: Compass quote regression

- **GIVEN** 相同來源、目標、玩家狀態與石碑結構
- **WHEN** Server 分別使用變更前公式與普通羅盤介面計算報價
- **THEN** 成本、準備時間、偏差與磨損率 SHALL 相同

### Requirement: Recovery compass death specialization

回生羅盤對玩家自己的有效 `DEATH` 目的地 SHALL 將最終傳送偏差設為 `floor(baseDeviation × 0.50)`。此特化 SHALL NOT 改變成本、準備時間、抵達傷害或結構磨損。

#### Scenario: Recovery compass to death node

- **GIVEN** 基礎偏差為 12 格
- **AND** 目標為請求玩家自己的有效 `DEATH` Space Unit
- **WHEN** 玩家使用回生羅盤取得報價
- **THEN** 最終偏差 SHALL 為 6 格

#### Scenario: Recovery compass to lodestone

- **GIVEN** 目標為 `LODESTONE`
- **WHEN** 玩家使用回生羅盤取得報價
- **THEN** 偏差與其他報價欄位 SHALL 使用普通基準值

### Requirement: Book route codex specialization

書本對已探索且有權限查看的 `LODESTONE` 目標 SHALL 降低 20% 準備時間，且 SHALL 降低本次路線固定石碑結構 25% 磨損機率。書本 SHALL NOT 降低食物、紫水晶或偏差。

#### Scenario: Book to known lodestone

- **GIVEN** 基礎準備時間為 100 ticks
- **AND** 基礎結構磨損機率為 20%
- **AND** 目標為玩家已探索且可見的 `LODESTONE`
- **WHEN** 玩家使用書本取得報價
- **THEN** 最終準備時間 SHALL 為 80 ticks
- **AND** 最終結構磨損機率 SHALL 為 15%

#### Scenario: Book to player target

- **GIVEN** 目標為 `PLAYER`
- **WHEN** 玩家使用書本取得報價
- **THEN** 準備時間與磨損率 SHALL 使用普通基準值

### Requirement: Filled map coverage specialization

已繪製地圖的 Dimension 與目標 Dimension 相同，且目標 Server 座標位於該地圖覆蓋範圍內時，Server SHALL 將食物等價成本與偏差各降低 20%。地圖 SHALL NOT 降低跨 Dimension 紫水晶成本。

#### Scenario: Target inside map coverage

- **GIVEN** 基礎食物等價成本為 10
- **AND** 基礎偏差為 15 格
- **AND** 目標位於手持已繪製地圖的 Dimension 與覆蓋範圍內
- **WHEN** Server 計算報價
- **THEN** 最終食物等價成本 SHALL 為 8
- **AND** 最終偏差 SHALL 為 12 格

#### Scenario: Target outside map coverage

- **GIVEN** 玩家使用有效已繪製地圖開啟傳送介面
- **AND** 目標不在該地圖覆蓋範圍內
- **WHEN** Server 計算報價
- **THEN** 傳送介面 SHALL 仍可使用
- **AND** 成本與偏差 SHALL 使用普通基準值

#### Scenario: Cross-dimension amethyst remains unchanged

- **GIVEN** 目標位於地圖覆蓋範圍內
- **AND** 傳送為跨 Dimension
- **WHEN** Server 計算紫水晶成本
- **THEN** 地圖 SHALL NOT 額外降低紫水晶碎片成本
- **AND** 既有紫水晶催化折抵與最低成本規則 SHALL 繼續生效

#### Scenario: Player target moves outside coverage

- **GIVEN** 線上好友 `PLAYER` 目標在報價時位於地圖範圍內
- **WHEN** 目標在傳送開始或完成前移出範圍
- **THEN** Server SHALL 取消該 session 並要求重新報價
- **AND** SHALL NOT 在未告知玩家時改用較高成本扣款

### Requirement: Interface quote display and privacy

傳送介面 SHALL 顯示目前介面物品、特化名稱、加成是否有效及 Server 計算的最終報價。對 `PLAYER` 目標的地圖覆蓋判斷 SHALL NOT 向 Client 洩漏目標精確位置。

#### Scenario: Map bonus for friend target

- **GIVEN** 好友 `PLAYER` 目標位於地圖覆蓋範圍內
- **WHEN** Server 回傳報價
- **THEN** Client MAY 收到地圖加成有效狀態與最終成本／偏差
- **BUT** Client SHALL NOT 收到用於覆蓋判斷的精確座標

#### Scenario: Hidden or unexplored target

- **GIVEN** 目標未探索、無權限或為 `HIDDEN`
- **WHEN** 玩家使用任何傳送介面物品
- **THEN** 目標 SHALL NOT 因介面特化而出現在列表中
- **AND** 特化 SHALL NOT 繞過既有探索與權限規則