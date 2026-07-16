# Proposal: Container Nesting Restrictions

Status: proposed
Target Version: 2.4.1

## 問題

可攜式容器互相巢狀會增加資料遞迴、容量放大、序列化成本、物品複製與死亡擷取 rollback 的風險。現有死亡背包只在死亡擷取時排除另一個死亡背包，尚未形成一致的雙向限制。

## 目標

- 禁止 DeadRecall 死亡背包放入 Bundle、Shulker Box 或其他設定為受限制的可攜式容器。
- 禁止 Bundle、Shulker Box 或其他受限制可攜式容器放入 DeadRecall 死亡背包。
- 所有玩家、容器、漏斗、投擲器、快捷移動與程式化轉移入口採用同一套 Server 驗證。
- 保護舊世界既有巢狀資料，不因載入而刪除物品。

## 非目標

- 不全面禁止所有容器物品彼此收納。
- 不修改原版 Bundle 與 Shulker Box 在一般箱子中的行為。
- 不在本變更中遞迴拆解或自動重排舊有巢狀內容。

## 相容性

既有非法巢狀物品可被讀取與取出，但不得再次放回受限制位置。系統不得在世界載入、物品同步或 Data Component 解碼時直接刪除內容。