# 重新構建模組腳本

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     DeadRecall 模組重新構建                   ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 檢查 gradlew.bat 是否存在
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "❌ 錯誤：找不到 gradlew.bat" -ForegroundColor Red
    Write-Host "請確認你在正確的目錄中" -ForegroundColor Yellow
    exit 1
}

# 檢查 Java 環境
Write-Host "檢查 Java 環境..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Out-String
    Write-Host "✅ Java 已設定" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ 找不到 Java！" -ForegroundColor Red
    Write-Host ""
    Write-Host "請執行以下腳本設定 Java 環境：" -ForegroundColor Yellow
    Write-Host ".\setup-java.ps1" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "或手動設定 JAVA_HOME 環境變數" -ForegroundColor Gray
    Write-Host ""
    exit 1
}

# 步驟 1: Clean
Write-Host "【步驟 1/3】清理舊的構建文件..." -ForegroundColor Yellow
Write-Host "執行: .\gradlew.bat clean" -ForegroundColor Gray
Write-Host ""

try {
    $output = & .\gradlew.bat clean 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 清理成功" -ForegroundColor Green
    } else {
        Write-Host "⚠️  清理完成（有警告）" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ 清理失敗：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Start-Sleep -Seconds 1

# 步驟 2: Build
Write-Host "【步驟 2/3】構建模組（包含 Gson 依賴）..." -ForegroundColor Yellow
Write-Host "執行: .\gradlew.bat build" -ForegroundColor Gray
Write-Host "這可能需要幾分鐘，請稍候..." -ForegroundColor Gray
Write-Host ""

try {
    $output = & .\gradlew.bat build 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 構建成功" -ForegroundColor Green
    } else {
        Write-Host "❌ 構建失敗" -ForegroundColor Red
        Write-Host "錯誤輸出：" -ForegroundColor Yellow
        Write-Host $output
        exit 1
    }
} catch {
    Write-Host "❌ 構建失敗：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Start-Sleep -Seconds 1

# 步驟 3: 驗證
Write-Host "【步驟 3/3】驗證 JAR 文件..." -ForegroundColor Yellow
Write-Host ""

$jarPath = "build\libs\deadrecall-1.1.0.jar"

if (Test-Path $jarPath) {
    $fileSize = (Get-Item $jarPath).Length
    $fileSizeKB = [math]::Round($fileSize / 1KB, 2)

    Write-Host "✅ JAR 文件已生成" -ForegroundColor Green
    Write-Host "路徑: $jarPath" -ForegroundColor Cyan
    Write-Host "大小: $fileSizeKB KB" -ForegroundColor Cyan
    Write-Host ""

    if ($fileSizeKB -lt 200) {
        Write-Host "⚠️  警告：文件大小可能不正常（應該 ~300KB）" -ForegroundColor Yellow
    } else {
        Write-Host "✅ 文件大小正常" -ForegroundColor Green
    }
} else {
    Write-Host "❌ 找不到 JAR 文件" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# 詢問是否執行驗證
Write-Host "是否要執行 JAR 驗證？(Y/N)" -ForegroundColor Yellow
$verify = Read-Host

if ($verify -eq "Y" -or $verify -eq "y") {
    Write-Host ""
    Write-Host "執行驗證腳本..." -ForegroundColor Yellow
    Write-Host ""
    & .\verify-jar.ps1
} else {
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║           ✅ 構建完成！                       ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "下一步：" -ForegroundColor Yellow
    Write-Host "1. 執行驗證：.\verify-jar.ps1" -ForegroundColor White
    Write-Host "2. 或直接複製到 Minecraft：" -ForegroundColor White
    Write-Host "   Copy-Item '$jarPath' -Destination `"`$env:APPDATA\.minecraft\mods\`" -Force" -ForegroundColor Cyan
    Write-Host ""
}
