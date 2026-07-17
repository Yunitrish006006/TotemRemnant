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

## 管理員診斷

具有管理員指令權限的使用者可執行：

```text
/deadrecall containers scan
/deadrecall containers scan <player>
```

第一個指令掃描所有線上玩家；第二個只掃描指定線上玩家。掃描範圍包括：

- 玩家完整 Inventory。
- 玩家 2×2 crafting inputs。
- 目前游標上的 ItemStack。
- 玩家目前開啟 Menu 中已載入的 slots；若正在查看外部容器，報告可能包含該容器畫面中的背包。

報告會列出玩家、元件路徑、父容器 ID、子物品 ID、深度及違規方向。完整結果同時寫入 Server log。

診斷是唯讀操作：

- 不移動物品。
- 不清除或重寫 Data Components。
- 不自動修復舊資料。
- 不掃描未載入區塊或全世界所有箱子。

掃描設有 16 層深度、4,096 個 ItemStack 與 256 筆保留結果上限。超出限制時只回報結果已截斷，以避免異常資料拖慢 Server。

## Addon 擴充

Addon 或 datapack 可以將自己的可攜式容器加入：

```text
deadrecall:portable_containers
```

此 Tag 控制「該容器不可放入 DeadRecall 背包」。反方向應沿用 Minecraft 的 `Item#canFitInsideContainerItems()` 契約，或在自訂容器插入程式中呼叫 DeadRecall 的 portable-container policy。
