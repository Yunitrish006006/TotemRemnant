# 網路與執行緒

DeadRecall 的 GUI、銅魁儡設定、Totem Nexus 與 Discord Bridge 都涉及 client／server 或外部 HTTP 通訊。

## Client 與 Server

- 伺服器是銅魁儡模式、欄位、綁定、燃料與 LLM 設定的最終權威。
- Client GUI 只顯示同步資料並送出操作請求。
- 所有封包處理都要再次驗證玩家權限、距離、維度、實體存在與欄位限制。
- 不可信任 Client 傳入的 ItemStack、座標、模式或容器狀態。
- 修改世界、實體或物品欄必須回到伺服器主執行緒。

## Totem Nexus Payload

| Payload | 方向 | 用途 |
| --- | --- | --- |
| `RequestSpaceUnitMapPayload` | Client → Server | 重新整理指定來源型態的地圖 |
| `RefreshSpaceUnitQuotePayload` | Client → Server | 只重掃來源與選取目標，重新產生路線報價 |
| `StartSpaceUnitTeleportPayload` | Client → Server | 請求從來源傳送到目標 UUID；開始前再次重掃兩端 |
| `SpaceUnitMapPayload` | Server → Client | 傳送已過濾節點與 Server-side 報價 |

Nexus 的安全條件：

- Client 只能提交 `sourceType`、來源 UUID 與目標 UUID。
- `lodestone` 來源由 Server 查詢 SavedData、Dimension、方塊位置、權限、探索狀態與距離。
- `player` 來源只能使用請求玩家自己的 UUID，座標由 Server 使用玩家當下位置建立。
- Client 不得提交可信來源座標、目標座標、成本、穩定度、偏差、傷害機率或安全落點。
- 選取目標時只重新掃描本次來源與目標，不全量掃描所有 Space Unit。
- 啟動傳送與完成倒數時都要重新驗證來源、目標、成本與落點。
- 地圖 Payload 只包含玩家已探索且有權限查看的節點，不同步完整世界資料。

## Discord Bridge

- 一般聊天、死亡訊息、玩家加入／離開、首次加入、advancement、管理稽核、健康告警、死亡背包、公開 Space Unit、Boss、raid、村民升級及啟動狀態使用單執行緒 Executor 非同步傳送，避免阻塞 server tick。
- 伺服器關閉通知可在停止流程中同步傳送，降低程序結束前訊息尚未送出的機率。
- 聊天與伺服器狀態 endpoint 都必須支援 Bot Token 指定頻道與 Webhook fallback。
- 管理稽核不得送完整指令原文；死亡背包與 Space Unit 事件不得送座標或物品內容。
- HTTP 逾時與錯誤應安全失敗，不得造成伺服器崩潰。
- API Key、Webhook URL 與 Bot Token 不得寫入一般 log。

## LLM 請求

- LLM 請求不得在 server tick thread 上等待。
- 回應套用前重新確認銅魁儡、目的地、模式與請求版本仍有效。
- 過期回應不得覆寫玩家後來修改的 Prompt 或快取。
- 錯誤、逾時或格式無效時保留手動規則與既有快取。
