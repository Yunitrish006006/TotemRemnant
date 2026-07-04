# DeadRecall – Discord Bridge 部署指南

## 架構概覽

```
Minecraft 伺服器
  └─ DeadRecall Mod (DiscordBridge.java)
       └─ HTTP POST ──► Cloudflare Worker (worker.js)
                              └─ Discord Webhook ──► Discord 頻道
```

> **注意**：此整合使用的是 **Discord Webhook**（非傳統 Bot Token 方式）。  
> Webhook 只能「傳送」訊息到 Discord，不能接收 Discord 的訊息回 Minecraft。  
> 若需要雙向（Discord → Minecraft），需額外實作 Discord Bot，詳見最後章節。

---

## 步驟一：在 Discord 建立 Webhook

1. 開啟 Discord，進入要橋接的**頻道**
2. 點選頻道名稱旁的 **⚙️ 編輯頻道**
3. 左側選單選 **整合 (Integrations)**
4. 點選 **建立 Webhook**
5. 設定名稱（例如 `Minecraft Server`）與頭像（可選）
6. 點選 **複製 Webhook 網址**，格式為：
   ```
   https://discord.com/api/webhooks/<ID>/<TOKEN>
   ```
7. 儲存並關閉設定

> 可建立多個 Webhook 同時廣播到多個頻道，稍後在 Worker 環境變數中以陣列設定。

---

## 步驟二：部署 Cloudflare Worker

### 前置需求

- [Cloudflare 帳號](https://dash.cloudflare.com/sign-up)（免費方案即可）
- Node.js 18+
- Wrangler CLI：`npm install -g wrangler`

### 方法 A：使用 Wrangler CLI（推薦）

```bash
# 進入 deploy 目錄
cd deploy/

# 登入 Cloudflare
npx wrangler login

# 部署 Worker
npx wrangler deploy
```

部署成功後會顯示 Worker URL，格式為：
```
https://deadrecall-discord-bridge.<你的帳號>.workers.dev
```

### 設定環境變數（Secret）

```bash
# 設定 API Key（自訂任意字串，用於驗證 Minecraft 的請求）
npx wrangler secret put MC_API_KEY
# 輸入提示時填入你的 API Key，例如：mc_ak_Xy7pQ9...

# 設定 Discord Webhook URL（JSON 陣列格式）
npx wrangler secret put DISCORD_WEBHOOK_URLS
# 輸入提示時填入（可含多個 webhook）：
# ["https://discord.com/api/webhooks/ID1/TOKEN1","https://discord.com/api/webhooks/ID2/TOKEN2"]
```

### 方法 B：Cloudflare Dashboard（無需 CLI）

1. 前往 [Cloudflare Dashboard](https://dash.cloudflare.com/) → **Workers & Pages**
2. 點選 **建立 Worker**
3. 將 `deploy/worker.js` 的內容貼入編輯器
4. 點選 **部署**
5. 進入 Worker 的 **Settings → Variables**：
   - 新增 **Secret** `MC_API_KEY`，值為自訂金鑰
   - 新增 **Secret** `DISCORD_WEBHOOK_URLS`，值為 JSON 陣列字串：
     ```json
     ["https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"]
     ```

### 測試 Worker

```bash
curl -X POST https://deadrecall-discord-bridge.<帳號>.workers.dev/api/mc/chat \
  -H "Content-Type: application/json" \
  -H "X-API-Key: 你的MC_API_KEY" \
  -d '{"username":"TestPlayer","message":"Hello from curl!"}'
```

回應應為：
```json
{"success":true,"data":{"sent":1,"failed":0,"total":1}}
```

---

## 步驟三：在 Minecraft 伺服器啟用 Discord Bridge

### 方式 A：遊戲內 UI（僅限 OP）

1. 確保你是伺服器 OP（需要權限等級 ≥ 2）
2. 加入伺服器後，在聊天欄輸入：
   ```
   /discordbridgeui
   ```
   或到**設定 → 按鍵綁定 → DeadRecall** 設定快捷鍵
3. 在開啟的 **Discord Bridge 設定**畫面填入：
   - **Worker URL**：`https://deadrecall-discord-bridge.<帳號>.workers.dev`
   - **API Key**：與 `MC_API_KEY` 相同的字串
   - **啟用**：切換為「啟用: 是」
4. 點選 **儲存到伺服器**

> ⚠️ 非 OP 玩家無法用此方式修改設定，會收到「你沒有權限修改 Discord Bridge 設定」的提示。

### 方式 B：直接編輯設定檔

在伺服器的 `config/discord-bridge.json`（首次啟動後自動生成）：

```json
{
  "enabled": true,
  "workerUrl": "https://deadrecall-discord-bridge.<帳號>.workers.dev",
  "apiKey": "你的MC_API_KEY"
}
```

修改後輸入以下指令重新載入（無需重啟伺服器）：
```
/discordbridge reload
```

### 方式 C：指令設定（OP 專用）

```
/discordbridge set true https://deadrecall-discord-bridge.<帳號>.workers.dev 你的MC_API_KEY
```

非 OP 玩家無法執行此指令。

---

## 設定檔說明

### `config/discord-bridge.json`（伺服器端，自動生成）

| 欄位        | 類型    | 說明                                  |
|------------|---------|---------------------------------------|
| `enabled`  | boolean | 是否啟用 Discord Bridge                |
| `workerUrl`| string  | Cloudflare Worker 的 URL              |
| `apiKey`   | string  | 與 Worker 環境變數 `MC_API_KEY` 相同   |

### Worker 環境變數

| 變數名                 | 類型          | 說明                                      |
|-----------------------|---------------|-------------------------------------------|
| `MC_API_KEY`          | Secret string | 驗證用 API Key，自訂任意字串              |
| `DISCORD_WEBHOOK_URLS`| Secret string | Discord Webhook URL 的 JSON 陣列字串      |

---

## Worker API 端點

| 端點                       | 方法 | 說明                    |
|---------------------------|------|-------------------------|
| `/api/mc/chat`            | POST | 轉發玩家聊天訊息到 Discord |
| `/api/mc/server/status`   | POST | 回報伺服器狀態（含 embed）|

### `/api/mc/chat` 請求格式

```json
{
  "username": "玩家名稱",
  "message": "訊息內容"
}
```

Header 需包含：`X-API-Key: <MC_API_KEY>`

---

## 遊戲內指令

### 基礎設定

| 指令                                       | 權限 | 說明                    |
|-------------------------------------------|------|-------------------------|
| `/discordbridgeui`                         | OP   | 開啟 Discord Bridge 設定 UI |
| `/discordbridge reload`                    | OP   | 重新載入設定檔            |
| `/discordbridge set <enabled> <url> <key>` | OP   | 直接用指令設定基本參數     |

### 頻道管理（新功能）

| 指令                                        | 權限 | 說明                    |
|---------------------------------------------|------|-------------------------|
| `/discordbridge channel add <id> <name>`   | OP   | 添加 Discord 頻道 ID    |
| `/discordbridge channel remove <id>`       | OP   | 移除 Discord 頻道 ID    |
| `/discordbridge channel list`              | OP   | 列表顯示所有頻道         |

### 其他

| 指令     | 權限   | 說明             |
|---------|--------|-----------------|
| `/back`  | 所有玩家 | 傳送回死亡地點   |

> ⚠️ **安全提醒**：只有 OP 才能修改 Discord Bridge 設定。不是 OP 的玩家無法用任何設定指令。

---

## 多頻道管理（新功能）

### 功能說明

DeadRecall 現已支援在單一 Minecraft 伺服器中管理多個 Discord 頻道。所有聊天訊息會自動廣播到配置的所有頻道。

### 配置流程

1. **添加頻道**（需 OP 權限）

```
/discordbridge channel add 1234567890 general
```

其中：
- `1234567890` 是 Discord 頻道 ID
- `general` 是自訂的頻道別名（用於在伺服器日誌中識別）

2. **查看所有頻道**

```
/discordbridge channel list
```

回應類似：
```
已配置 2 個頻道:
  - general (1234567890)
  - gaming (9876543210)
```

3. **移除頻道**

```
/discordbridge channel remove 1234567890
```

### 設定檔格式

配置檔 `config/discord-bridge.json` 現包含 `channels` 陣列：

```json
{
  "enabled": true,
  "workerUrl": "https://deadrecall-discord-bridge.xxx.workers.dev",
  "apiKey": "your_api_key",
  "channels": [
    { "id": "1234567890", "name": "general" },
    { "id": "9876543210", "name": "gaming" }
  ]
}
```

### 如何取得 Discord 頻道 ID

1. 在 Discord 開啟**開發者模式**（用戶設定 → 進階 → 開發者模式）
2. 右鍵點選要廣播的頻道 → **複製頻道 ID**
3. 在 Minecraft 伺服器中用指令添加

### 工作流程

```
玩家在 Minecraft 聊天
  ↓
DeadRecall Mod 捕獲訊息
  ↓
HTTP POST 到 Cloudflare Worker
  ├─ 若設定了 Bot Token：直接發送到所有配置的頻道 ID
  └─ 否則：發送到所有配置的 Webhook URLs
```

---

## 常見問題

### Q: 聊天訊息沒有出現在 Discord
1. 確認 `discord-bridge.json` 中 `enabled` 為 `true`
2. 確認 `workerUrl` 末尾沒有 `/`
3. 確認 `apiKey` 與 Worker 的 `MC_API_KEY` 完全一致
4. 查看伺服器 log 是否有 `[DiscordBridge]` 相關錯誤
5. 用 curl 直接測試 Worker（見上方測試指令）

### Q: Worker 回傳 401
API Key 不符，確認 `MC_API_KEY` Secret 已正確設定。

### Q: Worker 回傳 500 "No Discord webhooks configured"
`DISCORD_WEBHOOK_URLS` Secret 未設定或格式不正確，需為有效的 JSON 陣列字串。

### Q: 如何讓 Discord 訊息也傳回 Minecraft？（雙向橋接）

目前實作為**單向**（Minecraft → Discord）。  
實現雙向需要：
1. 在 Discord Developer Portal 建立 Bot Application
2. 賦予 Bot `Message Content Intent` 權限
3. 讓 Bot 監聽 Discord 頻道訊息
4. 透過 RCON 或自訂 WebSocket 將訊息注入 Minecraft

此功能目前不在 DeadRecall Mod 的範圍內。

---

## 部署檔案清單

```
deploy/
├── worker.js       ← Cloudflare Worker 主程式（部署此檔）
└── wrangler.toml   ← Wrangler CLI 設定檔
```
