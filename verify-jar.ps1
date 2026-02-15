# JAR 驗證腳本 - 檢查模組是否正確打包

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     DeadRecall 模組 JAR 驗證工具              ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$jarPath = "build\libs\deadrecall-1.1.0.jar"

# 檢查 JAR 是否存在
if (-not (Test-Path $jarPath)) {
    Write-Host "❌ 錯誤: 找不到 JAR 文件" -ForegroundColor Red
    Write-Host "路徑: $jarPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "請先構建模組:" -ForegroundColor Yellow
    Write-Host "  .\gradlew.bat build" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

Write-Host "✅ 找到 JAR 文件: $jarPath" -ForegroundColor Green
Write-Host ""

# 檢查文件大小
$fileSize = (Get-Item $jarPath).Length
$fileSizeKB = [math]::Round($fileSize / 1KB, 2)
$fileSizeMB = [math]::Round($fileSize / 1MB, 2)

Write-Host "檔案大小: $fileSizeKB KB ($fileSizeMB MB)" -ForegroundColor Cyan

if ($fileSizeKB -lt 100) {
    Write-Host "⚠️  警告: 檔案太小，可能缺少依賴" -ForegroundColor Yellow
} elseif ($fileSizeKB -gt 10240) {
    Write-Host "⚠️  警告: 檔案太大，可能包含不必要的依賴" -ForegroundColor Yellow
} else {
    Write-Host "✅ 檔案大小正常" -ForegroundColor Green
}
Write-Host ""

# 創建臨時目錄
$tempDir = "temp_jar_check"
if (Test-Path $tempDir) {
    Remove-Item $tempDir -Recurse -Force
}
New-Item -ItemType Directory -Path $tempDir | Out-Null

Write-Host "正在解壓 JAR 文件..." -ForegroundColor Yellow
try {
    # JAR 實際上是 ZIP 格式，但 PowerShell 的 Expand-Archive 只識別 .zip 擴展名
    # 所以先複製並重命名為 .zip
    $zipPath = $jarPath -replace '\.jar$', '.zip'
    Copy-Item $jarPath -Destination $zipPath -Force

    Expand-Archive -Path $zipPath -DestinationPath $tempDir -Force

    # 清理臨時 zip 文件
    Remove-Item $zipPath -Force -ErrorAction SilentlyContinue

    Write-Host "✅ 解壓成功" -ForegroundColor Green
} catch {
    Write-Host "❌ 解壓失敗: $($_.Exception.Message)" -ForegroundColor Red
    Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
    exit 1
}
Write-Host ""

# 檢查項目
Write-Host "══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  開始檢查 JAR 內容" -ForegroundColor Cyan
Write-Host "══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

$allPassed = $true

# 1. 檢查 fabric.mod.json
Write-Host "【檢查 1】fabric.mod.json" -ForegroundColor Yellow
if (Test-Path "$tempDir\fabric.mod.json") {
    Write-Host "  ✅ 存在" -ForegroundColor Green

    $fabricJson = Get-Content "$tempDir\fabric.mod.json" -Raw | ConvertFrom-Json
    Write-Host "  模組 ID: $($fabricJson.id)" -ForegroundColor Gray
    Write-Host "  版本: $($fabricJson.version)" -ForegroundColor Gray
    Write-Host "  名稱: $($fabricJson.name)" -ForegroundColor Gray

    # 檢查 mixins
    if ($fabricJson.mixins -contains "deadrecall.mixins.json") {
        Write-Host "  ✅ 註冊了 deadrecall.mixins.json" -ForegroundColor Green
    } else {
        Write-Host "  ❌ 缺少 deadrecall.mixins.json" -ForegroundColor Red
        $allPassed = $false
    }

    if ($fabricJson.mixins -contains "deadrecall.client.mixins.json") {
        Write-Host "  ✅ 註冊了 deadrecall.client.mixins.json" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  缺少 deadrecall.client.mixins.json（可能不影響功能）" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ 不存在" -ForegroundColor Red
    $allPassed = $false
}
Write-Host ""

# 2. 檢查主類
Write-Host "【檢查 2】主類文件" -ForegroundColor Yellow
$mainClass = "$tempDir\com\adaptor\deadrecall\Deadrecall.class"
if (Test-Path $mainClass) {
    Write-Host "  ✅ Deadrecall.class 存在" -ForegroundColor Green
} else {
    Write-Host "  ❌ Deadrecall.class 不存在" -ForegroundColor Red
    $allPassed = $false
}
Write-Host ""

# 3. 檢查 DiscordBridge
Write-Host "【檢查 3】DiscordBridge 類" -ForegroundColor Yellow
$discordBridge = "$tempDir\com\adaptor\deadrecall\DiscordBridge.class"
if (Test-Path $discordBridge) {
    Write-Host "  ✅ DiscordBridge.class 存在" -ForegroundColor Green
} else {
    Write-Host "  ❌ DiscordBridge.class 不存在" -ForegroundColor Red
    $allPassed = $false
}
Write-Host ""

# 4. 檢查 Gson (最重要!)
Write-Host "【檢查 4】Gson 依賴 ⭐ 重點" -ForegroundColor Yellow
$gsonDir = "$tempDir\com\google\gson"
if (Test-Path $gsonDir) {
    Write-Host "  ✅ Gson 已打包" -ForegroundColor Green

    # 統計 Gson 文件數量
    $gsonFiles = Get-ChildItem -Path $gsonDir -Recurse -File
    Write-Host "  包含 $($gsonFiles.Count) 個 Gson 文件" -ForegroundColor Gray

    # 檢查關鍵類
    $jsonParser = "$gsonDir\JsonParser.class"
    $jsonObject = "$gsonDir\JsonObject.class"

    if ((Test-Path $jsonParser) -and (Test-Path $jsonObject)) {
        Write-Host "  ✅ JsonParser 和 JsonObject 都存在" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Gson 可能不完整" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ Gson 未打包！" -ForegroundColor Red
    Write-Host "  這是主要問題！模組無法在生產環境運行" -ForegroundColor Red
    Write-Host ""
    Write-Host "  解決方法：" -ForegroundColor Yellow
    Write-Host "  1. 確認 build.gradle 中有：" -ForegroundColor Gray
    Write-Host "     include 'com.google.code.gson:gson:2.10.1'" -ForegroundColor Cyan
    Write-Host "  2. 執行：.\gradlew.bat clean build" -ForegroundColor Gray
    $allPassed = $false
}
Write-Host ""

# 5. 檢查 Mixin 文件
Write-Host "【檢查 5】Mixin 配置文件" -ForegroundColor Yellow
if (Test-Path "$tempDir\deadrecall.mixins.json") {
    Write-Host "  ✅ deadrecall.mixins.json 存在" -ForegroundColor Green
} else {
    Write-Host "  ❌ deadrecall.mixins.json 不存在" -ForegroundColor Red
    $allPassed = $false
}

if (Test-Path "$tempDir\deadrecall.client.mixins.json") {
    Write-Host "  ✅ deadrecall.client.mixins.json 存在" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  deadrecall.client.mixins.json 不存在" -ForegroundColor Yellow
}
Write-Host ""

# 6. 檢查 Mixin 類
Write-Host "【檢查 6】Mixin 類文件" -ForegroundColor Yellow
$mixinClass = "$tempDir\com\adaptor\deadrecall\mixin\ServerPlayerEntityMixin.class"
if (Test-Path $mixinClass) {
    Write-Host "  ✅ ServerPlayerEntityMixin.class 存在" -ForegroundColor Green
} else {
    Write-Host "  ❌ ServerPlayerEntityMixin.class 不存在" -ForegroundColor Red
    $allPassed = $false
}
Write-Host ""

# 清理
Write-Host "正在清理臨時文件..." -ForegroundColor Yellow
Remove-Item $tempDir -Recurse -Force
Write-Host "✅ 清理完成" -ForegroundColor Green
Write-Host ""

# 總結
Write-Host "══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  檢查結果" -ForegroundColor Cyan
Write-Host "══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

if ($allPassed) {
    Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║           ✅ 所有檢查通過！                   ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "你的模組已正確打包，可以在生產環境使用！" -ForegroundColor Green
    Write-Host ""
    Write-Host "下一步：" -ForegroundColor Yellow
    Write-Host "1. 將 JAR 複製到 Minecraft mods 資料夾" -ForegroundColor White
    Write-Host "   Copy-Item '$jarPath' -Destination `"`$env:APPDATA\.minecraft\mods\`" -Force" -ForegroundColor Cyan
    Write-Host "2. 啟動 Minecraft（使用 Fabric Loader）" -ForegroundColor White
    Write-Host "3. 測試功能" -ForegroundColor White
} else {
    Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Red
    Write-Host "║           ❌ 檢查失敗！                       ║" -ForegroundColor Red
    Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Red
    Write-Host ""
    Write-Host "你的模組缺少必要的文件或依賴。" -ForegroundColor Red
    Write-Host ""
    Write-Host "修復步驟：" -ForegroundColor Yellow
    Write-Host "1. 檢查上述失敗的項目" -ForegroundColor White
    Write-Host "2. 參考 '模組打包修復指南.md'" -ForegroundColor White
    Write-Host "3. 執行：" -ForegroundColor White
    Write-Host "   .\gradlew.bat clean" -ForegroundColor Cyan
    Write-Host "   .\gradlew.bat build" -ForegroundColor Cyan
    Write-Host "4. 重新執行此腳本驗證" -ForegroundColor White
}

Write-Host ""
Write-Host "按任意鍵退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
