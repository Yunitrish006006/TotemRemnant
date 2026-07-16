# Design: Admin Death Node Manager

## 權限

- 預設要求 Minecraft permission level 3。
- 開啟 GUI、查詢、傳送、停用、刪除與批次操作都由 Server 重新驗證。
- Client 不得提交節點內容、Owner UUID、座標或狀態作為權威資料，只能提交 node UUID 與操作類型。

## GUI

使用 `Menu` / `AbstractContainerScreen` 或等價的 Server-authoritative 分頁介面。

篩選條件：

- 玩家 UUID／名稱。
- Dimension。
- `ACTIVE`、`DISABLED`、`ORPHANED`、`MISSING_BACKPACK`、`INVALID`。
- 建立時間範圍。
- 僅顯示目前有效節點。

每列至少顯示：

- Owner 名稱與 UUID 簡寫。
- 節點 UUID 簡寫。
- Dimension 與座標。
- 建立時間、狀態及綁定死亡背包 UUID。
- 診斷旗標。

## 操作

- `Inspect`：查看完整 Server 資料與診斷結果。
- `Teleport`：傳送管理員至安全鄰近位置，不直接傳入危險方塊。
- `Disable`：保留歷史資料但從一般 Nexus 地圖隱藏。
- `Delete`：永久移除 SavedData 記錄。
- `Batch Disable/Delete`：只作用於 Server 重新執行相同篩選後所得的節點，不能信任 Client 提交 UUID 清單。

永久刪除與批次操作需要二次確認，確認 token 必須短效、綁定管理員 UUID、操作與篩選摘要。

## 資料一致性

- 刪除節點不刪除死亡背包實體。
- 停用或刪除節點後，存活死亡背包仍可被回收；回收流程找不到節點時必須視為冪等成功。
- 節點綁定不存在的背包不得自動判定可刪除，僅標記 `MISSING_BACKPACK`。
- 同一背包綁定多個活動節點時標記衝突，預設禁止一鍵自動刪除。
- 所有變更在同一 Server tick 內更新主要資料與索引，失敗不得留下半套狀態。

## 稽核

記錄操作者、操作、節點數、篩選摘要、成功／失敗數及時間。若 Discord Bridge 管理稽核啟用，送出摘要，但通知失敗不得回滾已完成的資料操作。