# Design: Direct Friend Player Teleport

## Goals

- 雙向好友可直接互相傳送，不需要逐次確認。
- 維持 Server authoritative、粗略 Client 座標與安全落點。
- 好友關係失效時立即阻止或取消傳送。

## Flow

```text
Client 選取線上好友 PLAYER 目標
→ Server 解析來源
→ Server 驗證雙向好友與目標在線
→ Server 計算報價與安全落點可用性
→ 直接建立 TeleportSession
→ 每 tick 重驗來源、目標、好友關係與取消條件
→ 完成時使用目標最新位置搜尋安全落點
```

## Removal

刪除或停止使用：

- `PLAYER_TARGET_CONSENT_TICKS`
- `pendingPlayerTeleportConsents`
- `PendingPlayerTeleportConsent`
- `PlayerTeleportConsentKey`
- `requestPlayerTeleportConsent(...)`
- `acceptPendingPlayerTeleport(...)`
- `startTeleport(..., playerTargetConsentGranted)` 多載
- 羅盤右鍵玩家時優先接受傳送請求的分支

## Session validation

玩家目標 session 每次 tick 至少驗證：

- 目標玩家仍在線、存活且未移除。
- 雙方仍是雙向好友。
- 來源仍有效。
- 傳送者仍符合移動、受傷、Dimension、羅盤與成本條件。

完成傳送前再次執行同樣驗證。解除好友時可主動遍歷目前 session 並移除雙方互相指向的玩家目標 session，避免等待下一 tick。

## Privacy

- 地圖 payload 保持粗略座標與距離。
- Client 只提交目標 UUID。
- 目標精確位置只在 Server 解析 `ServerPlayer` 時取得。

## Notifications

- 傳送者：顯示倒數與取消原因。
- 目標玩家：收到某好友正在傳送過來的通知。
- 完成後可通知雙方，但目標不需要互動。
