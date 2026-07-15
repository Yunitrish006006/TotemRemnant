# Space Unit Lodestone Delta Specification

## MODIFIED Requirements

### Requirement: Friend player targets

線上雙向好友 SHALL 可作為 `PLAYER` Space Unit 目的地。雙向好友關係本身 SHALL 視為持續的玩家傳送授權，不得要求目標玩家在每次傳送時再次按鍵確認。

#### Scenario: Direct teleport to online friend

- **GIVEN** 玩家 A 與玩家 B 為雙向好友
- **AND** 玩家 B 在線
- **WHEN** 玩家 A 從 Space Unit 地圖選取玩家 B 並啟動傳送
- **THEN** Server SHALL 直接建立傳送 session
- **AND** 玩家 B SHALL NOT 需要手持羅盤右鍵玩家 A
- **AND** 玩家 B SHALL 收到好友正在傳送的通知

#### Scenario: Friendship removed during preparation

- **GIVEN** 玩家 A 正在準備傳送至玩家 B
- **WHEN** 任一方解除好友
- **THEN** 該傳送 session SHALL 被取消
- **AND** 玩家 A SHALL 收到明確取消原因

#### Scenario: Target becomes unavailable

- **GIVEN** 玩家 A 正在準備傳送至玩家 B
- **WHEN** 玩家 B 離線、死亡、被移除或不再是有效目標
- **THEN** 傳送 SHALL 被取消

#### Scenario: Target moves during preparation

- **GIVEN** 玩家 A 的 PLAYER 傳送倒數完成
- **AND** 玩家 B 在倒數期間移動
- **WHEN** Server 完成傳送
- **THEN** 安全落點 SHALL 以玩家 B 的最新 Server 位置計算
- **AND** Client SHALL NOT 收到玩家 B 的精確位置
