# Discord Bridge API 測試腳本
# 用於測試 Cloudflare Worker API

$apiUrl = "https://mc-discord-bot.yunitrish0419.workers.dev/api/mc/chat"
$apiKey = "mc_ak_7Xp9Qm3vKsW2nF8jRtYb6LdA4eHcZu"

Write-Host "=== Discord Bridge API 測試 ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "API URL: $apiUrl" -ForegroundColor Yellow
Write-Host "API Key: $($apiKey.Substring(0, 10))..." -ForegroundColor Yellow
Write-Host ""

# 準備請求
$headers = @{
    "Content-Type" = "application/json"
    "X-API-Key" = $apiKey
}

$body = @{
    username = "測試玩家"
    message = "這是一條測試訊息 from PowerShell"
} | ConvertTo-Json

Write-Host "發送的 JSON:" -ForegroundColor Yellow
Write-Host $body
Write-Host ""

try {
    Write-Host "正在發送請求..." -ForegroundColor Yellow
    $response = Invoke-WebRequest -Uri $apiUrl -Method POST -Headers $headers -Body $body -UseBasicParsing

    Write-Host "✅ 請求成功!" -ForegroundColor Green
    Write-Host "HTTP 狀態碼: $($response.StatusCode)" -ForegroundColor Green
    Write-Host ""
    Write-Host "回應內容:" -ForegroundColor Yellow
    Write-Host $response.Content
    Write-Host ""

    # 解析 JSON 回應
    $jsonResponse = $response.Content | ConvertFrom-Json

    if ($jsonResponse.success) {
        Write-Host "✅ Worker 回應成功" -ForegroundColor Green

        if ($jsonResponse.data) {
            $sent = $jsonResponse.data.sent
            $failed = $jsonResponse.data.failed

            Write-Host ""
            Write-Host "發送統計:" -ForegroundColor Cyan
            Write-Host "  成功發送: $sent" -ForegroundColor $(if ($sent -gt 0) { "Green" } else { "Yellow" })
            Write-Host "  發送失敗: $failed" -ForegroundColor $(if ($failed -gt 0) { "Red" } else { "Green" })

            if ($failed -gt 0) {
                Write-Host ""
                Write-Host "⚠️  警告: 有 $failed 個 Discord 頻道發送失敗!" -ForegroundColor Red
                Write-Host ""
                Write-Host "可能的原因:" -ForegroundColor Yellow
                Write-Host "  1. Discord Webhook URL 無效或已過期"
                Write-Host "  2. Discord Webhook 被刪除或停用"
                Write-Host "  3. Worker 環境變數設定錯誤"
                Write-Host "  4. Discord API 權限問題"
                Write-Host "  5. 網路連接問題"
                Write-Host ""
                Write-Host "建議檢查 Cloudflare Worker 的:" -ForegroundColor Cyan
                Write-Host "  - 環境變數 (DISCORD_WEBHOOK_URLS)"
                Write-Host "  - Worker 日誌 (wrangler tail)"
                Write-Host "  - Discord Webhook 狀態"
            }
        }
    } else {
        Write-Host "❌ Worker 回應失敗" -ForegroundColor Red
        if ($jsonResponse.error) {
            Write-Host "錯誤訊息: $($jsonResponse.error)" -ForegroundColor Red
        }
    }

} catch {
    Write-Host "❌ 請求失敗!" -ForegroundColor Red
    Write-Host "錯誤: $($_.Exception.Message)" -ForegroundColor Red

    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "HTTP 狀態碼: $statusCode" -ForegroundColor Red

        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorBody = $reader.ReadToEnd()
            Write-Host "錯誤詳情: $errorBody" -ForegroundColor Red
        } catch {
            Write-Host "無法讀取錯誤詳情" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "=== 測試完成 ===" -ForegroundColor Cyan
