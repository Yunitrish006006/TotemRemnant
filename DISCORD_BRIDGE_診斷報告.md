# Discord Bridge 問題診斷報告

## 📊 問題分析

### 當前狀態
✅ **Minecraft 端**: 正常運作  
✅ **網路連接**: 正常  
✅ **Worker API**: 正常回應  
❌ **Discord 發送**: **失敗** (sent: 0, failed: 2)

### 日誌分析
```
[DiscordBridge] 發送成功 (HTTP 200): {"success":true,"data":{"sent":0,"failed":2}}
```

**解讀**:
- Worker 收到了請求
- Worker 嘗試發送到 2 個 Discord 頻道
- **兩個都失敗了**（sent: 0, failed: 2）

---

## 🔍 問題根源

問題在 **Cloudflare Worker** 端，可能原因：

### 1. Discord Webhook URL 無效 ⭐ **最可能**
- Webhook 可能被刪除或重新生成
- URL 格式錯誤
- Webhook 過期

### 2. Worker 環境變數錯誤
- `DISCORD_WEBHOOK_URLS` 沒有正確設定
- 環境變數格式錯誤（應該是 JSON 陣列）

### 3. Discord API 問題
- Discord 伺服器問題
- Rate limit（發送頻率限制）
- 權限問題

### 4. Worker 程式碼問題
- 錯誤處理不正確
- 請求格式錯誤

---

## 🛠️ 解決步驟

### 步驟 1: 檢查 Discord Webhook

1. 前往你的 Discord 伺服器
2. 進入要接收訊息的頻道設定
3. 前往「整合」(Integrations) > 「Webhooks」
4. 檢查 Webhook 是否存在且有效
5. 如果需要，**創建新的 Webhook**：
   - 點擊「新增 Webhook」
   - 設定名稱（例如：Minecraft Chat）
   - 設定頭像（可選）
   - **複製 Webhook URL**（格式：`https://discord.com/api/webhooks/...`）

### 步驟 2: 測試 Webhook（用 PowerShell）

```powershell
$webhookUrl = "你的_WEBHOOK_URL"

$body = @{
    content = "測試訊息"
    username = "測試機器人"
} | ConvertTo-Json

Invoke-WebRequest -Uri $webhookUrl -Method POST -Body $body -ContentType "application/json"
```

如果這個測試成功，表示 Webhook 是有效的。

### 步驟 3: 更新 Cloudflare Worker 環境變數

在 Cloudflare Dashboard：

1. 登入 Cloudflare
2. 前往 Workers & Pages
3. 選擇你的 Worker (`mc-discord-bot`)
4. 點擊「Settings」>「Variables」
5. 找到或新增 `DISCORD_WEBHOOK_URLS`
6. 設定值為 JSON 陣列格式：
   ```json
   ["https://discord.com/api/webhooks/xxx/yyy", "https://discord.com/api/webhooks/aaa/bbb"]
   ```
7. 點擊「Save」

### 步驟 4: 檢查 Worker 日誌

使用 Wrangler CLI 查看即時日誌：

```bash
# 安裝 wrangler (如果還沒安裝)
npm install -g wrangler

# 登入 Cloudflare
wrangler login

# 查看 Worker 日誌
wrangler tail mc-discord-bot
```

然後在 Minecraft 發送一條訊息，觀察日誌輸出。

### 步驟 5: 測試 Worker API

執行我提供的測試腳本：

```powershell
cd D:\dev\minecraft\DeadRecall
.\test-discord-api.ps1
```

---

## 📝 Worker 程式碼檢查清單

如果你有 Worker 的原始碼，檢查以下幾點：

### 1. 環境變數讀取
```javascript
// 確認這樣讀取環境變數
const webhookUrls = JSON.parse(env.DISCORD_WEBHOOK_URLS || '[]');
```

### 2. Discord Webhook 請求格式
```javascript
// 正確的 Discord Webhook 格式
const discordBody = {
    content: `**${username}**: ${message}`,
    username: "Minecraft Server",
    // avatar_url: "https://..." // 可選
};

const response = await fetch(webhookUrl, {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify(discordBody)
});
```

### 3. 錯誤處理
```javascript
// 應該記錄詳細的錯誤
if (!response.ok) {
    console.error(`Discord webhook failed: ${response.status} - ${await response.text()}`);
}
```

---

## 🔧 快速修復範例 (Worker 程式碼)

```javascript
export default {
    async fetch(request, env) {
        // API Key 驗證
        const apiKey = request.headers.get('X-API-Key');
        if (apiKey !== env.MC_API_KEY) {
            return new Response(JSON.stringify({ error: 'Invalid API key' }), {
                status: 401,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        const { username, message } = await request.json();
        
        // 讀取 Webhook URLs
        const webhookUrls = JSON.parse(env.DISCORD_WEBHOOK_URLS || '[]');
        
        if (webhookUrls.length === 0) {
            return new Response(JSON.stringify({ 
                success: false, 
                error: 'No webhooks configured' 
            }), {
                status: 500,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        // 發送到所有 Discord Webhooks
        const results = await Promise.allSettled(
            webhookUrls.map(async (webhookUrl) => {
                const discordBody = {
                    content: `**${username}**: ${message}`,
                    username: "Minecraft Server"
                };

                const response = await fetch(webhookUrl, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(discordBody)
                });

                if (!response.ok) {
                    const error = await response.text();
                    console.error(`Discord webhook failed (${response.status}):`, error);
                    throw new Error(`HTTP ${response.status}: ${error}`);
                }

                return response;
            })
        );

        // 統計結果
        const sent = results.filter(r => r.status === 'fulfilled').length;
        const failed = results.filter(r => r.status === 'rejected').length;

        // 記錄失敗的詳細信息
        if (failed > 0) {
            console.error('Failed webhooks:', 
                results
                    .filter(r => r.status === 'rejected')
                    .map(r => r.reason.message)
            );
        }

        return new Response(JSON.stringify({ 
            success: true, 
            data: { sent, failed } 
        }), {
            headers: { 'Content-Type': 'application/json' }
        });
    }
};
```

---

## ✅ 驗證步驟

修復後，執行以下步驟驗證：

1. ✅ 執行測試腳本：`.\test-discord-api.ps1`
2. ✅ 在 Minecraft 發送訊息
3. ✅ 檢查 Discord 頻道是否收到訊息
4. ✅ 檢查 `run/logs/latest.log`，應該看到 `"sent":2,"failed":0`

---

## 📞 需要的資訊

如果問題持續，請提供：

1. Discord Webhook 測試結果（步驟 2）
2. Worker 日誌輸出（步驟 4）
3. Worker 環境變數設定截圖
4. Worker 原始碼（如果可以分享）

---

## 總結

✅ **Minecraft 端沒有問題** - 程式碼運作正常  
❌ **問題在 Cloudflare Worker** - 無法成功發送到 Discord  
🔧 **解決方向** - 檢查並更新 Discord Webhook URLs
