# Item Entity Transformations Delta Specification

## ADDED Requirements

### Requirement: Concrete powder item hardening

系統 SHALL 讓世界中的原版混凝土粉末 ItemEntity 在實際接觸 water-tagged fluid 時，轉換成相同顏色的混凝土 ItemStack。

#### Scenario: Single powder item enters water

- **GIVEN** 一個支援的混凝土粉末 ItemEntity
- **WHEN** 該實體實際接觸水源或流動水
- **THEN** Server SHALL 將其 ItemStack 轉成相同顏色的混凝土
- **AND** count SHALL 保持不變

#### Scenario: Full stack enters water

- **GIVEN** 一個 count 為 64 的混凝土粉末 ItemEntity
- **WHEN** 該實體接觸 water-tagged fluid
- **THEN** 系統 SHALL 產生 count 為 64 的對應混凝土 stack
- **AND** SHALL NOT 生成額外 ItemEntity

### Requirement: Complete color mapping

系統 SHALL 明確支援全部 16 種原版混凝土粉末，且每一種只能轉成相同顏色的原版混凝土。

#### Scenario: Every vanilla color is mapped

- **GIVEN** 16 種原版混凝土粉末的測試集合
- **WHEN** 映射完整性測試執行
- **THEN** 每個來源 SHALL 有且只有一個相同顏色目標
- **AND** 不得有缺少、重複或交叉顏色映射

### Requirement: Server-authoritative atomic replacement

系統 SHALL 只在伺服器端替換同一個 ItemEntity 的目前 ItemStack，不得以生成新實體再刪除舊實體的方式完成轉換。

#### Scenario: Entity state is preserved

- **GIVEN** 一個具有位置、速度、pickup delay、owner 與 age 的粉末 ItemEntity
- **WHEN** 轉換完成
- **THEN** ItemEntity identity SHALL 保持不變
- **AND** position、velocity、pickup delay、owner 與 age SHALL 不因轉換而重設

#### Scenario: Client observes conversion

- **GIVEN** Server 已完成 ItemStack 替換
- **WHEN** Client 收到同步
- **THEN** Client SHALL 顯示同一個 ItemEntity 持有對應混凝土
- **AND** Client SHALL NOT 自行建立或保留粉末幽靈副本

### Requirement: ItemStack data safety

系統 SHALL 以目前 Minecraft 版本支援的安全 API 保留 count 與可合法沿用的 Components。

#### Scenario: Named powder hardens

- **GIVEN** 混凝土粉末 stack 具有可沿用的自訂名稱
- **WHEN** stack 在水中轉換
- **THEN** 對應混凝土 SHALL 保留該自訂名稱
- **AND** count SHALL 保持不變

#### Scenario: Stack changed by another system

- **GIVEN** 其他模組已在同一更新週期修改 ItemEntity 的 stack
- **WHEN** DeadRecall 準備轉換
- **THEN** DeadRecall SHALL 重新讀取目前 stack
- **AND** 只有目前 stack 仍是支援的粉末時才可轉換

### Requirement: Strict water contact

系統 SHALL 只在 ItemEntity 實際接觸 water-tagged fluid 時觸發。

#### Scenario: Flowing water

- **GIVEN** 粉末 ItemEntity 位於流動水中
- **WHEN** Server 更新該實體
- **THEN** 粉末 SHALL 轉成對應混凝土

#### Scenario: Near water without contact

- **GIVEN** 粉末 ItemEntity 靠近水但碰撞體未接觸 water fluid
- **WHEN** Server 更新該實體
- **THEN** 粉末 SHALL NOT 轉換

#### Scenario: Rain exposure

- **GIVEN** 粉末 ItemEntity 位於雨中但沒有接觸 water fluid
- **WHEN** Server 更新該實體
- **THEN** 粉末 SHALL NOT 轉換

### Requirement: Scope isolation

系統 SHALL NOT 轉換不在世界 ItemEntity 中的粉末，也 SHALL NOT 改變原版混凝土粉末方塊硬化行為。

#### Scenario: Powder in inventory

- **GIVEN** 混凝土粉末位於玩家物品欄、容器、背包或銅傀儡庫存
- **WHEN** 相鄰世界位置存在水
- **THEN** 該粉末 SHALL NOT 因本功能轉換

#### Scenario: Placed concrete powder block

- **GIVEN** 一個放置在世界中的混凝土粉末方塊
- **WHEN** 方塊接觸水
- **THEN** 原版方塊硬化流程 SHALL 維持原有行為
- **AND** 本 ItemEntity 變更 SHALL NOT 重複介入

### Requirement: Bounded runtime cost

系統 SHALL 使用 ItemEntity 自身更新入口或等效事件，且 SHALL NOT 每 tick 全量掃描世界中的實體。

#### Scenario: Non-powder item tick

- **GIVEN** 一個不是支援混凝土粉末的 ItemEntity
- **WHEN** 更新入口執行
- **THEN** 系統 SHALL 在常數時間映射檢查後返回
- **AND** SHALL NOT 執行全世界 entity 查詢