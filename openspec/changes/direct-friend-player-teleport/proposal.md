# Change: Direct Friend Player Teleport

## Why

目前線上好友作為 `PLAYER` 目的地時，傳送者選取好友後仍需等待目標玩家手持羅盤再次右鍵傳送者。好友邀請已經需要雙方各自操作才能建立雙向關係，再要求每次傳送重複確認，會讓人體磁石難以使用。

本變更將雙向好友關係視為持續的玩家傳送授權。只要雙方仍是好友且目標在線，傳送者即可直接啟動傳送 session。

## What Changes

- 選取線上雙向好友後直接啟動傳送倒數。
- 目標玩家不必再次手持羅盤右鍵傳送者。
- 移除 `PendingPlayerTeleportConsent`、確認期限與相關訊息流程。
- 開始、倒數與完成傳送時，Server 都必須重新驗證雙向好友關係與目標在線狀態。
- 解除好友後，尚未完成且目標為該玩家的傳送 session 必須取消。
- 目標精確位置仍只存在 Server；Client 地圖只取得粗略位置。
- 倒數完成時使用目標玩家最新位置並搜尋安全落點。
- 目標玩家收到傳送開始與完成通知，但不需要按鍵確認。

## Impact

### Affected code

- `SpaceUnitHandler`
- `TeleportTarget`
- 玩家目標傳送 session 驗證
- 好友解除流程
- `assets/deadrecall/lang/*.json`
- Nexus 玩家與傳送文件

### Compatibility

- 不新增永久資料，不需要 migration。
- 現有好友 SavedData 保持不變。
- 舊的待確認請求只存在記憶體中，更新或重啟後自然消失。

### Risks

- 好友解除後仍有舊 session 繼續執行。
- 目標離線、死亡或切換維度時 session 沒有取消。
- Client 取得目標精確座標。
- 羅盤右鍵玩家的好友邀請流程與舊傳送確認流程殘留衝突。
