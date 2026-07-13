# 指令

| 指令 | 權限 | 用途 |
| --- | --- | --- |
| `/back` | 所有玩家 | 傳送回最近一次死亡位置；成功後清除記錄 |
| `/discordbridgeui` | OP | 開啟 Discord Bridge 設定 GUI |
| `/discordbridge reload` | OP | 重新載入 Discord Bridge 設定 |
| `/discordbridge set <enabled> <url> <key>` | OP | 直接設定 Bridge 狀態、Worker URL 與 API Key |
| `/discordbridge channel add <id> <name>` | OP | 新增 Discord 頻道設定 |
| `/discordbridge channel remove <id>` | OP | 移除 Discord 頻道設定 |
| `/discordbridge channel list` | OP | 顯示已設定頻道 |

Discord Bridge 的完整部署與安全注意事項見 [Discord Bridge](discord/README.md)。