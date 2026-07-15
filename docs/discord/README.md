# Discord Bridge

DeadRecall 可將 Minecraft 聊天、玩家動態、管理稽核、公開事件與伺服器狀態經 Cloudflare Worker 傳送到 Discord。

## 架構

```text
Minecraft Server
  → DeadRecall
  → Cloudflare Worker
  → Discord Webhook / configured channels
```

## 文件

- [伺服器設定](server-setup.md)
- [Cloudflare Worker 部署](worker.md)
- [故障排除](troubleshooting.md)
- [指令參考](../commands.md)

## 安全原則

- 只有 OP 可以修改 Bridge 設定。
- 只有 OP 或單人世界擁有者可以從 GUI 讀取 Bridge 設定；一般玩家不能取得 Worker URL、API Key 或頻道清單。
- GUI 不會顯示既有 API Key；API Key 欄位留空儲存時會保留目前設定，只有輸入新值才會覆寫。
- `MC_API_KEY` 應使用 Cloudflare Secret，不應提交到 Git。
- 不要在 issue、log 或截圖中公開完整 Webhook URL、Bot Token 或 API Key。
- 管理稽核不轉播完整指令原文；死亡背包與 Space Unit 通知不轉播座標或物品內容。
- 聊天與一般狀態通知應非同步送出，避免阻塞伺服器 tick。
