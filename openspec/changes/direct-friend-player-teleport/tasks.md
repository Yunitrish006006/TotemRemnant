# Tasks: Direct Friend Player Teleport

## 1. Remove per-teleport consent

- [x] 1.1 已移除核心檔案中的 pending consent 常數、Map、record、方法、五參數 overload 與過期清理。
- [x] 1.2 公開四參數傳送入口直接處理線上雙向好友 PLAYER 目標，不再轉入額外授權方法。
- [x] 1.3 羅盤右鍵玩家只處理好友邀請／接受，不再查詢或接受傳送請求。
- [x] 1.4 舊 `player_anchor_request_*` 翻譯 key 暫時保留作資源相容，但正式程式已無任何引用。

## 2. Session safety

- [x] 2.1 每 tick 透過 `resolveTeleportTarget` 重新驗證目標在線與雙向好友。
- [x] 2.2 完成傳送前透過 `completeTeleport` 重新解析目標與好友關係。
- [x] 2.3 解除好友時立即取消任一方向以對方為 PLAYER 目標的傳送 session，取消不再等待下一個 Server tick。
- [x] 2.4 PLAYER 目標失效會區分「離線」、「死亡／被移除」與「好友關係解除」；固定 Space Unit 仍使用一般目的地失效訊息。

## 3. UX and privacy

- [x] 3.1 只有好友傳送 session 實際建立成功且目標 UUID 確認為 PLAYER 端點後，目標玩家才收到「Teleport: requester → target」通知。
- [x] 3.2 保持 Client 粗略座標，不同步精確位置。
- [x] 3.3 新的傳送請求不再進入「等待同意」流程。

## 4. Tests

- [x] 4.1 雙向好友可直接啟動 PLAYER 傳送的多人整合測試。
  - `DirectFriendPlayerTeleportGameTest.bilateralFriendsStartDirectPlayerSession` 使用兩個已註冊於 Server `PlayerList` 的 mock `ServerPlayer`，經真實 Friend SavedData 建立雙向好友並驗證 PLAYER session 的 requester／target UUID。
- [x] 4.2 非好友、單向邀請與自己都無法作為 PLAYER 目標的多人整合測試。
  - 同一個 Server world 依序驗證沒有關係、只有 pending invite 與 self target，三者皆不得留下 session。
- [x] 4.3 雙方向好友關係 session 配對、無關 session 排除，以及 PLAYER 目標狀態優先序與翻譯 key 已有單元測試。
- [x] 4.4 完成時使用目標最新位置並找到安全落點。
  - 倒數期間移除舊落點、將目標移至新的唯一安全方塊，完成後 requester 必須落在最新座標；跨 Dimension 測試會跟隨目標進入 Nether 的最新安全落點。
  - 成功傳送的成本只扣除一次，完成後額外 Server ticks 不得再次扣款。
- [x] 4.5 Java 25 build 與 Dedicated Server 啟動／Mixin 套用煙霧測試通過。
- [x] 4.6 兩名以上真實玩家的多人回歸測試，包括解除好友、離線、死亡與切維度。
  - 解除好友同一呼叫內移除雙方向 session，且保留第三名玩家的無關 session。
  - 目標死亡或從 `PlayerList` 離線後，下一次 authoritative session tick 取消且不扣成本。
  - 目標在倒數期間切換 Dimension 後，完成階段重新解析其最新 Dimension、位置與安全落點。
