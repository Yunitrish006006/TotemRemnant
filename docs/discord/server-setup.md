# Discord Bridge 伺服器設定

## 設定檔

首次啟動後，伺服器會建立：

```text
config/discord-bridge.json
```

基本格式：

```json
{
  "enabled": true,
  "workerUrl": "https://your-worker.workers.dev",
  "apiKey": "your-api-key"
}
```

| 欄位 | 說明 |
| --- | --- |
| `enabled` | 是否啟用 Bridge |
| `workerUrl` | Cloudflare Worker URL，結尾不要加 `/` |
| `apiKey` | 必須與 Worker 的 `MC_API_KEY` 相同 |

## 遊戲內設定

OP 可使用：

```text
/discordbridgeui
/discordbridge reload
/discordbridge set <enabled> <url> <key>
```

設定 GUI 或指令都必須由伺服器再次驗證 OP 權限。

## 多頻道

支援的管理指令：

```text
/discordbridge channel add <id> <name>
/discordbridge channel remove <id>
/discordbridge channel list
```

頻道 ID 可在 Discord 開發者模式下，對頻道按右鍵後複製。

## 安全

- 不要把真實 API Key 提交到 Git。
- 不要在 issue 或截圖中公開完整設定檔。
- 若設定不完整，Bridge 應安全停用，不影響其他模組功能。