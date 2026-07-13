# Data Components 與背包資料

DeadRecall 背包內容使用原版 `DataComponents.CONTAINER` 與 `ItemContainerContents` 儲存。

## 必須維持的條件

- 開啟 GUI 時固定追蹤實際被開啟的背包 `ItemStack`。
- 關閉 GUI 或同步資料時，不可只依賴玩家目前主手。
- 寫回前確認追蹤的 Stack 仍是預期背包，且仍存在於玩家物品欄或合法容器位置。
- ItemStack 判斷應同時考慮 Item、Components 與必要的唯一識別資料。
- 死亡背包清空時，應等容器操作完成及 GUI 關閉後再移除背包本體。

## 常見錯誤

若使用 `player.getItemInHand(hand)` 作為關閉介面時的唯一寫回目標，玩家在 GUI 開啟期間換手，就可能把背包內容寫入銅鏟或其他物品。這類錯誤通常表現為物品圖示或 Components 融合，但物品原本功能仍部分存在。

## 修改檢查

調整背包資料格式時，至少測試：

1. 開啟背包後換快捷列位置再關閉。
2. 開啟後交換主手與副手。
3. 從容器內開啟或移動背包。
4. 清空死亡背包最後一格。
5. 背包掉落、拾取、死亡與重新登入後的資料一致性。