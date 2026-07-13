# DeadRecall Discord Bridge 規格

## 1. 目標

Discord Bridge 將伺服器端可公開的 Minecraft 事件轉送到 Discord，經由 Cloudflare Worker 統一處理 API Key 驗證、Webhook 或 Bot Token 發送、多頻道路由與錯誤隔離。

Bridge 是管理與觀測功能，不得影響遊戲邏輯。Discord API、Worker 或網路失敗時，Minecraft Server 必須繼續運作。

## 2. 事件範圍

目前轉播事件：

- 玩家聊天訊息。
- 玩家死亡訊息。
- 玩家第一次加入伺服器。
- 玩家加入伺服器。
- 玩家離開伺服器。
- 會公告聊天的重要 advancement。
- 村民職業等級提升。
- 管理稽核事件：op、deop、ban、pardon、kick、白名單啟用／停用／增減、difficulty、gamerule。
- 伺服器健康告警：低 TPS、TPS 恢復、Discord Bridge 連續傳送失敗。
- 死亡背包建立與回收。
- 公開 Space Unit 磁石公開、取消公開、重新命名、移除或失效。
- Boss 擊敗：終界龍、凋零。
- 襲擊開始與結束。
- Dedicated Server 開啟狀態。
- Server 關閉狀態。

不在目前範圍：

- 任意系統通知。
- Discord 到 Minecraft 的反向聊天。
- 完整指令原文或敏感管理參數。
- 私人 Space Unit、好友可見 Space Unit、死亡節點座標、背包內容、玩家 IP。

新增事件前必須先更新本規格，避免 Discord 訊息過量或洩漏非預期資訊。

## 3. Server 端規則

所有轉播來源必須由 Server event 產生，不得信任 Client payload。

玩家聊天使用 Fabric server message event 取得已裝飾文字；死亡訊息使用 Server `DamageSource` 產生的原生 death message；玩家加入／離開使用 Server play connection event；玩家首次加入以原版統計資料判斷；advancement 僅轉播 display 設定會公告聊天且 gamerule 允許的完成事件；村民升級由 Server side villager level change 偵測；死亡背包與 Space Unit 事件由 DeadRecall server-side 流程產生；Boss 與 raid 使用 server-side entity／raid event 或 mixin 偵測；管理稽核由 server command 或 PlayerList 結果產生；開關服狀態由 Server lifecycle event 產生。

一般聊天、死亡、玩家加入／離開、首次加入、advancement、管理稽核、健康告警、死亡背包、公開 Space Unit、Boss、raid、村民升級與開服狀態必須非同步送出，避免阻塞 server tick。關服狀態可同步送出，降低程序結束前訊息尚未送出的機率。

Discord Bridge 關閉、設定不完整或傳送失敗時必須安全 no-op 或記錄非敏感錯誤，不得中止事件流程。

## 4. Worker 與路由

Minecraft Server 只呼叫 Cloudflare Worker，不直接呼叫 Discord API。

Worker endpoint：

```text
POST /api/mc/chat
POST /api/mc/server/status
```

`/api/mc/chat` 用於玩家聊天、死亡訊息、玩家加入／離開、首次加入、advancement、管理稽核、健康告警、死亡背包、公開 Space Unit、Boss、raid、村民升級等文字型 Minecraft 事件。Payload SHOULD 帶 `event` 欄位，用於 Worker 選擇 Discord 顯示格式；未帶 `event` 時 Worker MUST 以一般聊天格式處理。

所有 Minecraft → Worker 請求必須帶 `X-API-Key`，並由 Worker 與 `MC_API_KEY` 比對。

頻道路由：

- 若請求帶有 `channels` 且 Worker 設定 `DISCORD_BOT_TOKEN`，Worker 必須優先以 Bot Token 發送到指定頻道。
- 若沒有指定頻道，或 Bot Token 不可用，Worker 使用 `DISCORD_WEBHOOK_URLS` 作為 fallback。
- 聊天與伺服器狀態 endpoint 都必須支援同一套路由規則。
- Worker 必須驗證 `channels` 為 Discord snowflake 格式、移除重複與無效值，並限制單次請求可轉送的頻道數；清理後沒有有效頻道時，必須回退 Webhook。

## 5. 安全

不得在一般 log 中輸出完整 API Key、Webhook URL 或 Bot Token。

Worker response 的診斷資料不得包含完整 Webhook URL 或 token；Webhook 目標只能以不含 secret 的代號表示。API Key 比對不得使用容易外洩 timing 差異的直接字串比對。

死亡訊息、加入／離開、advancement、管理稽核與聊天訊息可以包含玩家名稱及公開聊天內容。

管理稽核不得轉播完整指令原文；只允許動作、操作者顯示名稱與目標玩家名稱。不得附加 ban/kick 詳細理由中的敏感資訊。

死亡背包與 Space Unit 通知不得附加未公開座標、背包內容、玩家 IP、私有 Space Unit 座標、好友可見 Space Unit 座標、死亡節點座標或 Discord secret。

HTTP 逾時、Discord rate limit、Worker misconfiguration 或單一頻道失敗不得造成 Minecraft Server crash。

## 6. 驗收條件

- 玩家聊天會送到 Discord。
- 玩家死亡會送出原版死亡訊息。
- 玩家第一次加入會送出首次加入通知。
- 玩家加入會送出加入通知。
- 玩家離開會送出離開通知。
- 重要 advancement 完成會送出進度通知。
- 村民升級會送出系統訊息。
- op、deop、ban、pardon、kick、白名單、difficulty 與 gamerule 變更會送出管理稽核訊息。
- TPS 持續偏低會送出健康告警；恢復後會送出恢復通知。
- Discord Bridge 連續傳送失敗會記錄 warning 並嘗試送出健康告警。
- 死亡背包建立與回收會送出通知，但不包含物品內容或座標。
- 公開 Space Unit 磁石公開、取消公開、重新命名、移除或失效會送出通知，但不包含座標。
- 終界龍或凋零被擊敗會送出 Boss 擊敗通知。
- 襲擊開始與結束會送出通知。
- Dedicated Server 開啟後會送出開服狀態。
- Server 停止流程會送出關服狀態。
- 使用 Bot Token 與頻道 ID 時，聊天與狀態訊息都能送到指定頻道。
- 只有 Webhook 設定時，聊天與狀態訊息都能送到 webhook。
- Worker response 與 log 不包含完整 Webhook URL、Webhook token、Bot Token 或 API Key。
- 無效或重複頻道 ID 不會被送往 Discord API，且不會破壞 Webhook fallback。
- Worker 或 Discord 失敗時，Minecraft Server 不崩潰。
