# Discord Bridge 故障排除

## Discord 沒有收到訊息

1. 確認 `enabled` 為 `true`。
2. 確認 `workerUrl` 正確且結尾沒有 `/`。
3. 確認伺服器 `apiKey` 與 Worker `MC_API_KEY` 完全一致。
4. 用 curl 直接測試 Worker。
5. 檢查伺服器 log 中的 `[DiscordBridge]` 錯誤。

## Worker 回傳 401

API Key 不一致。重新設定 `MC_API_KEY`，並同步更新伺服器設定。

## Worker 回傳 500 或沒有 Webhook

確認 `DISCORD_WEBHOOK_URLS` 是合法 JSON 陣列，而且每個 URL 都是完整 Discord Webhook URL。

## 指令沒有權限

`/discordbridgeui` 與所有設定指令需要 OP 權限。一般玩家只能使用 `/back`。

## 多頻道未生效

- 確認頻道 ID 正確。
- 確認 Worker 部署版本支援目前的頻道設定格式。
- 執行 `/discordbridge channel list` 檢查伺服器保存的資料。
- 修改設定後執行 `/discordbridge reload`。

回報問題時，請遮蔽 API Key、Webhook token、Bot Token 與伺服器私人網址。