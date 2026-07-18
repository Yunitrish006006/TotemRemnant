# 好友玩家直接傳送測試

此文件記錄 `openspec/changes/direct-friend-player-teleport/` 的多人伺服器回歸證據。

## 測試入口

```text
src/gametest/java/com/adaptor/deadrecall/space/DirectFriendPlayerTeleportGameTest.java
```

測試使用 Fabric Server GameTest 建立多個真實 `ServerPlayer`。這些玩家會註冊於同一個 `MinecraftServer.getPlayerList()`，Friend SavedData、TeleportSession、Server tick、Dimension 切換與世界方塊碰撞皆使用正式程式路徑。

## 直接啟動與拒絕矩陣

- 兩名玩家透過兩次 `inviteOrAccept` 建立雙向好友後，可直接建立以目標 UUID 為端點的 PLAYER `TeleportSession`。
- 沒有好友關係時不得建立 session。
- 只有單向 pending invite 時不得建立 session。
- 玩家不得以自己的 UUID 建立 PLAYER session。

Client 只提交來源類型、來源 UUID 與目標 UUID。目標在線、存活、未移除及雙向好友關係由 Server 解析。

## 最新位置與安全落點

完成流程測試會：

1. 在舊目標位置提供一個安全落點並啟動倒數。
2. 倒數期間移除舊落點。
3. 將目標移到另一個只有單一安全落點的位置。
4. 等待正式 `END_SERVER_TICK` session controller 完成傳送。

Requester 必須落在目標最新位置，而不是 session 建立時的快照位置。這證明 `completeTeleport` 會重新執行 `resolveTeleportTarget` 與安全落點搜尋。

跨 Dimension 測試會在倒數期間把目標移至 Nether 高空平台。Requester 完成後必須位於 Nether 且站在最新安全落點，不能留在原 Dimension 或使用舊座標。

## 成本 exactly-once

同 Dimension PLAYER 傳送使用 Survival requester：

- 初始飢餓值為 20、飽和度為 0。
- 該測試距離的 PLAYER 報價為 5 點飢餓成本。
- 完成後飢餓值必須為 15。
- 再經過額外 Server ticks 後仍必須為 15。

目標死亡或離線造成取消時，Requester 的飢餓值保持 20，確認失敗 session 不會扣款。

## 關係與目標失效

- 第一名與第二名玩家互相建立 session，第三名玩家另建立一條無關 session。
- 第一名移除第二名好友時，Mixin 必須在 `removeFriend` 同一呼叫內移除前兩條雙方向 session。
- 第三名玩家的無關 session 必須保留。
- 目標死亡後，下一次 authoritative tick 取消 session。
- 目標從 Server `PlayerList` 移除後，下一次 authoritative tick 以 offline 原因取消 session。

## 邊界

這些 GameTests 使用同一個 Dedicated GameTest Server 中的多個真實 `ServerPlayer`，足以驗證 Server authority、資料關係、世界碰撞及 deterministic tick 順序。

它們不模擬真實網路延遲、封包遺失、不同地理區域 Client 或人工 GUI 操作。這類驗收仍可作為發布前補充，但不能取代上述自動回歸。
