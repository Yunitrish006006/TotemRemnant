# Space Unit Lodestone Delta Specification

## ADDED Requirements

### Requirement: Amethyst catalyst discount

固定磁石石碑中的有效紫水晶催化方塊 SHALL 降低跨 Dimension 傳送的紫水晶碎片成本。催化方塊類型 SHALL 由 block tag 定義，計數 SHALL 來自 Server 石碑結構掃描。

#### Scenario: Catalyst blocks reduce cross-dimension cost

- **GIVEN** 跨 Dimension 傳送原始成本為 4 個紫水晶碎片
- **AND** 來源與目標固定石碑合計有 8 個有效催化方塊
- **WHEN** Server 計算報價
- **THEN** 催化折抵 SHALL 為 2
- **AND** 最終成本 SHALL 為 2

#### Scenario: Cost never becomes free

- **GIVEN** 催化折抵大於或等於原始成本
- **WHEN** Server 計算跨 Dimension 報價
- **THEN** 最終紫水晶成本 SHALL 至少為 1

#### Scenario: Non-lodestone endpoint has no catalyst contribution

- **GIVEN** 傳送來源或目標為 `PLAYER`、`DEATH` 或其他沒有固定石碑的節點
- **WHEN** Server 計算催化折抵
- **THEN** 該端催化方塊數 SHALL 為 0

#### Scenario: Structure changes after quote

- **GIVEN** 玩家取得含水晶折抵的報價
- **WHEN** 傳送開始或完成前水晶結構已被拆除
- **THEN** Server SHALL 重新掃描相關固定石碑
- **AND** SHALL 使用更新後的最終成本驗證與扣款

### Requirement: Catalyst quote details

Server 報價 SHALL 分別提供原始紫水晶成本、催化方塊總數、折抵數量與最終紫水晶成本。Client SHALL NOT 自行推算或提交折抵。
