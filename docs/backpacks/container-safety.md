# 可攜式容器安全規則

DeadRecall 禁止背包與其他可攜式儲物容器互相巢狀，避免產生不受控制的遞迴容量、資料回注錯誤與物品複製風險。

## 禁止的組合

- Bundle 放入一般背包或死亡背包。
- 任意顏色的 Shulker Box 放入一般背包或死亡背包。
- 一般背包或死亡背包放入 Bundle。
- 一般背包或死亡背包放入 Shulker Box。
- Datapack 或 addon 加入 `deadrecall:portable_containers` Item Tag 的物品放入 DeadRecall 背包。

## 套用路徑

限制由 Server 判斷，涵蓋：

- 背包 GUI 拖曳、Shift 點擊、游標與快速移動。
- Bundle 使用的原版 container-item 插入契約。
- Shulker Box GUI。
- 漏斗、漏斗礦車、投擲器、發射器等使用 Shulker Box sided insertion 的自動化。
- 銅傀儡分類與 Home 存放的直接 Container 寫入。
- 死亡背包擷取與暫存槽獨立掉落。

自動化反覆嘗試將背包放入 Shulker Box 時，Server 會以限速方式記錄診斷訊息，不會每 tick 洗滿 log。

## 舊世界資料

升級前已經存在的非法巢狀內容不會被自動刪除、攤平或改寫。

- 舊內容仍可正常載入。
- 玩家可以把非法巢狀物品取出。
- 取出後不能再次放回。
- 模組不會在開啟背包時把內容自動丟到地上。

此策略避免升級後直接遺失裝有物品的 Bundle、Shulker Box 或舊背包。

## Addon 擴充

Addon 或 datapack 可以將自己的可攜式容器加入：

```text
deadrecall:portable_containers
```

此 Tag 控制「該容器不可放入 DeadRecall 背包」。反方向應沿用 Minecraft 的 `Item#canFitInsideContainerItems()` 契約，或在自訂容器插入程式中呼叫 DeadRecall 的 portable-container policy。
