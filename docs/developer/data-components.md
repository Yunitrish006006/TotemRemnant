# Data Components 與背包資料

DeadRecall 背包內容使用原版 `DataComponents.CONTAINER` 與 `ItemContainerContents` 儲存。

## 必須維持的條件

- 開啟 GUI 時固定追蹤實際被開啟的背包 `ItemStack`。
- 關閉 GUI 或同步資料時，不可只依賴玩家目前主手。
- 寫回前確認追蹤的 Stack 仍是預期背包，且仍存在於玩家物品欄或合法容器位置。
- ItemStack 判斷應同時考慮 Item、Components 與必要的唯一識別資料。
- 死亡背包清空時，應等容器操作完成及 GUI 關閉後再移除背包本體。
- 背包 Menu 開啟期間，擁有該 Menu 的 ItemStack 必須以物件 identity 鎖定，不得被 Shift-click、拾取、丟棄、複製或快捷列交換移走。
- 玩家物品欄中的任何背包都不得 Shift-click 進目前開啟的背包；但舊資料中已經巢狀的背包應允許向外取出。

## Menu 安全

一般 `ChestMenu` 不知道哪個玩家物品是容器本體，因此不能直接用於可攜式容器。DeadRecall 使用 `BackpackMenu` 在 Server 端攔截交易：

- 玩家物品欄 → 背包的背包類 ItemStack quick-move 會被拒絕。
- 目前開啟的精確 ItemStack 會被鎖住，不只依物品類型或 Components 判斷。
- 背包內容 → 玩家物品欄的 quick-move 保持可用，方便回收舊版錯誤造成的巢狀背包。
- Client 仍可使用原版箱子畫面；所有防護由 Server Menu 執行。

## 常見錯誤

若使用 `player.getItemInHand(hand)` 作為關閉介面時的唯一寫回目標，玩家在 GUI 開啟期間換手，就可能把背包內容寫入銅鏟或其他物品。這類錯誤通常表現為物品圖示或 Components 融合，但物品原本功能仍部分存在。

若讓原版 `ChestMenu` 直接控制可攜式背包，玩家可對快捷列中的容器本體 Shift-click。來源 ItemStack 被清空後，`BackpackInventory` 會失去持久化目標，而被搬入虛擬容器的副本會在關閉 GUI 後消失。

## 修改檢查

調整背包資料格式或 Menu 操作時，至少測試：

1. 開啟背包後對容器本體 Shift+左鍵，確認沒有移動或消失。
2. 開啟背包後對容器本體左鍵、Q、數字鍵交換與雙擊，確認仍留在原位置。
3. 玩家物品欄中的另一個背包無法 Shift-click 進開啟中的背包。
4. 已存在於背包內容中的舊版巢狀背包可以 Shift-click 取出。
5. 開啟背包後換快捷列位置再關閉。
6. 開啟後交換主手與副手。
7. 從容器內開啟或移動背包。
8. 清空死亡背包最後一格。
9. 背包掉落、拾取、死亡與重新登入後的資料一致性。
