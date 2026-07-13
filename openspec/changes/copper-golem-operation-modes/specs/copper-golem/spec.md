# Copper Golem Delta Specification

## ADDED Requirements

### Requirement: Copper golem operation modes

系統 SHALL 新增 `SORTING` 與 `GATHERING` 模式，並維持 mode 與 running 狀態分離。

#### Scenario: Existing golem defaults to sorting

- **GIVEN** 舊銅傀儡沒有 mode
- **WHEN** 資料遷移執行
- **THEN** mode SHALL 設為 `SORTING`
- **AND** 現有分類資料 SHALL 保留

### Requirement: Safe mode switching

系統 SHALL 在模式切換前，由伺服器驗證目前模式的物品欄位與進行中工作已清空。

#### Scenario: Sorting cargo is not empty

- **GIVEN** 分類主手仍有貨物或來源返回尚未完成
- **WHEN** 玩家要求切換至 GATHERING
- **THEN** 切換 SHALL 被拒絕

#### Scenario: Gathering fields are not empty

- **GIVEN** 工具欄或採集倉庫非空
- **WHEN** 玩家要求切換至 SORTING
- **THEN** 切換 SHALL 被拒絕

### Requirement: Gathering mode inventory

系統 SHALL 提供一個工具欄及一個單一 ItemStack、最多 16 個物品的採集倉庫。

#### Scenario: Gathering storage accepts matching drops

- **GIVEN** 預期掉落與倉庫為相同 Item＋Components
- **AND** 完整數量不超過 16
- **WHEN** 採集成功
- **THEN** 掉落 SHALL 完整加入倉庫

#### Scenario: Gathering storage rejects unsafe drops

- **GIVEN** 預期掉落會溢出、混合多種 ItemStack 或與倉庫不相同
- **WHEN** 銅傀儡評估候選方塊
- **THEN** 方塊 SHALL NOT 被破壞

### Requirement: Gathering area and home

系統 SHALL 允許玩家設定 Corner A、Corner B 及一個 Home 銅箱。

#### Scenario: Configure work area

- **GIVEN** 銅傀儡處於 GATHERING
- **WHEN** 玩家右鍵方塊 A 並 Shift＋右鍵方塊 B
- **THEN** 系統 SHALL 保存正規化工作區

#### Scenario: Copper chest interaction sets home

- **GIVEN** 銅傀儡處於 GATHERING
- **WHEN** 玩家右鍵或 Shift＋右鍵銅箱
- **THEN** 該銅箱 SHALL 成為 Home
- **AND** 工作區角點 SHALL NOT 改變

### Requirement: Shared source copper chest

系統 SHALL 允許每隻銅傀儡綁定至多一個來源銅箱。來源銅箱在 SORTING 模式為待分類來源，在 GATHERING 模式為採集 Home。右鍵銅箱 SHALL 設定或替換來源銅箱，左鍵已綁定來源銅箱 SHALL 解除來源銅箱。

#### Scenario: Copper chest binds as source

- **GIVEN** 板手已綁定有效銅傀儡
- **WHEN** 玩家右鍵銅箱
- **THEN** 銅箱 SHALL 寫入來源銅箱欄位
- **AND** SHALL NOT 加入分類目的地清單

#### Scenario: Sorting requires source

- **GIVEN** 銅傀儡處於 SORTING 且沒有來源銅箱
- **WHEN** running 為 true
- **THEN** 銅傀儡 SHALL NOT 從其他銅箱取物
- **AND** activity SHALL 顯示缺少來源銅箱

#### Scenario: Mode-specific GUI

- **GIVEN** GUI 顯示銅傀儡設定
- **WHEN** mode 為 SORTING
- **THEN** GUI SHALL 顯示來源銅箱與分類目的地
- **WHEN** mode 為 GATHERING
- **THEN** GUI SHALL 顯示來源銅箱與採集目標設定

### Requirement: Manual and LLM gathering targets

系統 SHALL 允許板手左鍵切換 Block ID 手動規則，並允許 LLM Prompt 分類未知方塊類型；手動與安全規則 SHALL 優先於 LLM。

#### Scenario: Wrench selects a block type

- **GIVEN** 板手已綁定處於 GATHERING 的銅傀儡
- **WHEN** 玩家左鍵普通方塊
- **THEN** 該 Block ID 的手動允許狀態 SHALL 被切換
- **AND** 原版方塊攻擊 SHALL 被取消

#### Scenario: Stale LLM result is ignored

- **GIVEN** LLM 回應屬於舊 Prompt revision
- **WHEN** 回應返回
- **THEN** 系統 SHALL NOT 寫入目前快取

### Requirement: Atomic gathering and deposit

系統 SHALL 在破壞前驗證工具、區域、安全規則、權限、掉落及倉庫容量，並在成功後把資源帶回 Home。任何失敗 SHALL 保留物品。

#### Scenario: Successful gathering transaction

- **GIVEN** 所有驗證通過
- **WHEN** 銅傀儡破壞目標
- **THEN** 世界、工具耐久、燃料與倉庫 SHALL 以單一成功交易更新

#### Scenario: Home is full

- **GIVEN** Home 無法接收倉庫內容
- **WHEN** 銅傀儡存放
- **THEN** 倉庫 SHALL 保持不變
- **AND** 銅傀儡 SHALL 進入 blocked
- **AND** 物品 SHALL NOT 被丟棄

### Requirement: Automatic wrench visualization

玩家手持已綁定板手時，客戶端 SHALL 自動顯示目前模式資訊。

#### Scenario: Hold a bound wrench

- **GIVEN** 玩家與綁定銅傀儡同維度
- **WHEN** 玩家手持該板手
- **THEN** SORTING SHALL 顯示容器連線
- **AND** GATHERING SHALL 顯示工作區、Home 與目標

## MODIFIED Requirements

### Requirement: Copper golem selection and GUI

原本普通左鍵銅傀儡的選擇流程 SHALL 改為 Shift＋右鍵銅傀儡時，直接綁定觸發互動的板手並開啟 GUI。

#### Scenario: Shift-right-click binds and opens

- **GIVEN** 玩家手持銅板手
- **WHEN** 玩家 Shift＋右鍵銅傀儡
- **THEN** 板手 SHALL 綁定該銅傀儡
- **AND** GUI SHALL 開啟

#### Scenario: Normal left-click does not select

- **GIVEN** 玩家手持銅板手
- **WHEN** 玩家普通左鍵銅傀儡
- **THEN** 板手綁定 SHALL 不變

### Requirement: Sorting transport behavior

現有自訂 `TransportItemsBetweenContainers` 行為 SHALL 僅在 `SORTING` 模式作用，並保留現有綁定、LLM、燃料、最多 16 個搬運、來源返回及 blocked 行為。

#### Scenario: Gathering bypasses sorting mixin

- **GIVEN** 銅傀儡處於 GATHERING
- **WHEN** 原版搬運行為執行
- **THEN** DeadRecall 分類 Mixin SHALL 不介入
