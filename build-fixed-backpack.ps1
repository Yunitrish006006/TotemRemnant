# 背包存储修复 - PowerShell 构建脚本
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "背包存储修复 - 构建脚本" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 切换到项目目录
Set-Location D:\dev\minecraft\DeadRecall

Write-Host "[1/3] 清理旧构建文件..." -ForegroundColor Yellow
& .\gradlew.bat clean --no-daemon

Write-Host ""
Write-Host "[2/3] 构建项目..." -ForegroundColor Yellow
& .\gradlew.bat build --no-daemon

Write-Host ""
Write-Host "[3/3] 检查构建结果..." -ForegroundColor Yellow

if (Test-Path "build\libs\deadrecall-1.4.1.jar") {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "✅ 构建成功！" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""

    Write-Host "JAR 文件:" -ForegroundColor Yellow
    Get-ChildItem build\libs\*.jar | Format-Table Name, @{Name="大小(KB)";Expression={[math]::Round($_.Length/1KB,2)}}, LastWriteTime -AutoSize

    Write-Host ""
    Write-Host "下一步:" -ForegroundColor Cyan
    Write-Host "  1. 运行测试: " -NoNewline; Write-Host ".\gradlew.bat runClient" -ForegroundColor Yellow
    Write-Host "  2. 或将 JAR 复制到 mods 文件夹" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "❌ 构建失败" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "请检查上方的错误信息" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

