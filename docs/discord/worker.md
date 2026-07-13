# Cloudflare Worker 部署

Discord Bridge 使用 Cloudflare Worker 接收 Minecraft 伺服器的 HTTP 請求，再轉送到 Discord。

## 必要 Secret

| 變數 | 用途 |
| --- | --- |
| `MC_API_KEY` | 驗證 Minecraft 請求 |
| `DISCORD_WEBHOOK_URLS` | Discord Webhook URL 的 JSON 陣列 |
| `DISCORD_BOT_TOKEN` | 可選；用頻道 ID 發送多頻道訊息 |

Wrangler 範例：

```bash
npx wrangler secret put MC_API_KEY
npx wrangler secret put DISCORD_WEBHOOK_URLS
npx wrangler secret put DISCORD_BOT_TOKEN
npx wrangler deploy
```

`DISCORD_WEBHOOK_URLS` 格式：

```json
["https://discord.com/api/webhooks/ID/TOKEN"]
```

若遊戲內設定了頻道 ID 且 Worker 有 `DISCORD_BOT_TOKEN`，聊天與伺服器狀態會優先送到指定頻道；未設定 Bot Token 或未指定頻道時，會回退使用 `DISCORD_WEBHOOK_URLS`。

Worker 只接受 Discord snowflake 格式的頻道 ID，會移除重複或無效值，且每次請求最多轉送 10 個頻道。若清理後沒有有效頻道，會回退使用 Webhook。

## API 端點

| 端點 | 方法 | 用途 |
| --- | --- | --- |
| `/api/mc/chat` | POST | 轉送玩家聊天、玩家動態、管理稽核、公開事件 |
| `/api/mc/server/status` | POST | 轉送伺服器狀態 |

`/api/mc/chat` 可帶 `event` 欄位，例如 `chat`、`player_death`、`player_first_join`、`player_join`、`player_leave`、`advancement`、`admin_action`、`server_health_alert`、`death_backpack_created`、`death_backpack_recovered`、`space_unit_public_update`、`boss_defeated`、`raid_started`、`raid_ended`、`difficulty_changed`、`gamerule_changed` 或 `villager_level_up`，Worker 會依事件類型套用 Discord 顯示格式。

所有請求都應包含：

```text
X-API-Key: <MC_API_KEY>
```

## 測試

```bash
curl -X POST https://your-worker.workers.dev/api/mc/chat \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-key" \
  -d '{"event":"chat","username":"TestPlayer","message":"Hello"}'
```

Worker 不應把 Secret、Webhook token、完整 Webhook URL 或完整授權標頭寫入 log，也不應在 API response 的 `results` 中回傳完整 Webhook URL；Webhook 結果只會以 `webhook:1` 這類代號呈現。
