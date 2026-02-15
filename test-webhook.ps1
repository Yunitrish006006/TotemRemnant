# Discord Webhook 測試腳本
# 用於快速測試 Discord Webhook 是否有效

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     Discord Webhook 測試工具                  ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 輸入 Webhook URL
Write-Host "請貼上你的 Discord Webhook URL：" -ForegroundColor Yellow
Write-Host "（格式：https://discord.com/api/webhooks/...）" -ForegroundColor Gray
Write-Host ""
$webhookUrl = Read-Host "Webhook URL"

if ([string]::IsNullOrWhiteSpace($webhookUrl)) {
    Write-Host ""
    Write-Host "❌ 錯誤：未輸入 Webhook URL" -ForegroundColor Red
    Write-Host ""
    exit 1
}

# 驗證 URL 格式
if ($webhookUrl -notmatch '^https://discord\.com/api/webhooks/\d+/[\w-]+$') {
    Write-Host ""
    Write-Host "⚠️  警告：URL 格式可能不正確" -ForegroundColor Yellow
    Write-Host "正確格式應該是：https://discord.com/api/webhooks/數字/字串" -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "是否繼續測試？(Y/N)"
    if ($continue -ne "Y" -and $continue -ne "y") {
        exit 0
    }
}

Write-Host ""
Write-Host "準備測試..." -ForegroundColor Yellow
Write-Host ""

# 測試 1: 簡單訊息
Write-Host "【測試 1】發送簡單訊息..." -ForegroundColor Cyan
try {
    $body1 = @{
        content = "✅ 測試訊息：Webhook 連接成功！"
    } | ConvertTo-Json

    $response1 = Invoke-RestMethod -Uri $webhookUrl -Method POST -Body $body1 -ContentType "application/json" -ErrorAction Stop
    Write-Host "✅ 測試 1 成功！請檢查 Discord 頻道" -ForegroundColor Green
    Start-Sleep -Seconds 2
} catch {
    Write-Host "❌ 測試 1 失敗：$($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""

    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "錯誤原因：Webhook 不存在或已被刪除 (404)" -ForegroundColor Red
        Write-Host "解決方法：請在 Discord 創建新的 Webhook" -ForegroundColor Yellow
    } elseif ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "錯誤原因：權限不足 (401)" -ForegroundColor Red
        Write-Host "解決方法：檢查 Webhook URL 是否完整" -ForegroundColor Yellow
    } elseif ($_.Exception.Response.StatusCode.value__ -eq 429) {
        Write-Host "錯誤原因：發送頻率過高 (429)" -ForegroundColor Red
        Write-Host "解決方法：請稍後再試" -ForegroundColor Yellow
    }

    Write-Host ""
    exit 1
}

Write-Host ""

# 測試 2: 模擬 Minecraft 訊息
Write-Host "【測試 2】發送 Minecraft 格式訊息..." -ForegroundColor Cyan
try {
    $body2 = @{
        content = "**TestPlayer**: 這是一條測試訊息！"
        username = "Minecraft Server"
    } | ConvertTo-Json

    $response2 = Invoke-RestMethod -Uri $webhookUrl -Method POST -Body $body2 -ContentType "application/json" -ErrorAction Stop
    Write-Host "✅ 測試 2 成功！請檢查 Discord 頻道" -ForegroundColor Green
    Start-Sleep -Seconds 2
} catch {
    Write-Host "❌ 測試 2 失敗：$($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host ""

# 測試 3: 帶表情符號
Write-Host "【測試 3】發送表情符號訊息..." -ForegroundColor Cyan
try {
    $body3 = @{
        content = "🎮 **玩家123**: 哈囉世界！ 👋"
        username = "Minecraft Server"
        avatar_url = "https://mc-heads.net/avatar/Steve/64"
    } | ConvertTo-Json

    $response3 = Invoke-RestMethod -Uri $webhookUrl -Method POST -Body $body3 -ContentType "application/json" -ErrorAction Stop
    Write-Host "✅ 測試 3 成功！請檢查 Discord 頻道" -ForegroundColor Green
} catch {
    Write-Host "❌ 測試 3 失敗：$($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║           ✅ 所有測試通過！                   ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "你的 Webhook 運作正常！" -ForegroundColor Green
Write-Host ""
Write-Host "接下來的步驟：" -ForegroundColor Yellow
Write-Host "1. 複製這個 Webhook URL" -ForegroundColor White
Write-Host "2. 前往 Cloudflare Workers Dashboard" -ForegroundColor White
Write-Host "3. 更新 DISCORD_WEBHOOK_URLS 環境變數為：" -ForegroundColor White
Write-Host "   [""$webhookUrl""]" -ForegroundColor Cyan
Write-Host "4. 儲存並重新部署 Worker" -ForegroundColor White
Write-Host "5. 在 Minecraft 測試聊天功能" -ForegroundColor White
Write-Host ""
Write-Host "JSON 格式（複製使用）：" -ForegroundColor Yellow
Write-Host "[""$webhookUrl""]" -ForegroundColor Cyan
Write-Host ""

# 自動複製到剪貼簿（如果可能）
try {
    $jsonFormat = "[""$webhookUrl""]"
    Set-Clipboard -Value $jsonFormat
    Write-Host "✅ 已自動複製 JSON 格式到剪貼簿！" -ForegroundColor Green
    Write-Host ""
} catch {
    # 無法複製到剪貼簿，忽略錯誤
}

Write-Host "按任意鍵退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
