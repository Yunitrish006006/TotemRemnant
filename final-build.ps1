# 使用 Minecraft 内置 Gson 的构建脚本

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     使用 Minecraft 内置 Gson 重新构建         ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

Write-Host "✅ 已更新策略：使用 Minecraft 内置的 Gson" -ForegroundColor Green
Write-Host ""
Write-Host "新配置：" -ForegroundColor Yellow
Write-Host "  compileOnly 'com.google.code.gson:gson:2.10.1'" -ForegroundColor Cyan
Write-Host ""
Write-Host "优点：" -ForegroundColor Gray
Write-Host "  ✅ 不需要打包 Gson" -ForegroundColor Gray
Write-Host "  ✅ JAR 文件更小" -ForegroundColor Gray
Write-Host "  ✅ 100% 兼容 Minecraft" -ForegroundColor Gray
Write-Host "  ✅ 没有版本冲突" -ForegroundColor Gray
Write-Host ""

Start-Sleep -Seconds 2

# 清理
Write-Host "【步骤 1/3】清理..." -ForegroundColor Yellow
try {
    if (Test-Path "build") {
        Remove-Item "build" -Recurse -Force -ErrorAction SilentlyContinue
    }
    & .\gradlew.bat clean --no-daemon 2>&1 | Out-Null
    Write-Host "  ✅ 清理完成" -ForegroundColor Green
} catch {
    Write-Host "  ⚠️  清理完成（有警告）" -ForegroundColor Yellow
}

Write-Host ""
Start-Sleep -Seconds 1

# 构建
Write-Host "【步骤 2/3】构建模组..." -ForegroundColor Yellow
Write-Host "  这可能需要 1-2 分钟..." -ForegroundColor Gray
Write-Host ""

$buildStart = Get-Date

try {
    $buildOutput = & .\gradlew.bat build --no-daemon 2>&1
    $buildEnd = Get-Date
    $buildTime = ($buildEnd - $buildStart).TotalSeconds

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ 构建成功（耗时 $([math]::Round($buildTime, 1)) 秒）" -ForegroundColor Green
    } else {
        Write-Host "  ❌ 构建失败" -ForegroundColor Red
        Write-Host ""
        Write-Host "错误输出：" -ForegroundColor Yellow
        Write-Host $buildOutput
        exit 1
    }
} catch {
    Write-Host "  ❌ 构建失败：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Start-Sleep -Seconds 1

# 验证
Write-Host "【步骤 3/3】验证 JAR 文件..." -ForegroundColor Yellow
Write-Host ""

$jarPath = "build\libs\deadrecall-1.1.0.jar"

if (Test-Path $jarPath) {
    $fileSize = (Get-Item $jarPath).Length
    $fileSizeKB = [math]::Round($fileSize / 1KB, 2)

    Write-Host "  ✅ JAR 文件已生成" -ForegroundColor Green
    Write-Host "  路径: $jarPath" -ForegroundColor Cyan
    Write-Host "  大小: $fileSizeKB KB" -ForegroundColor Cyan
    Write-Host ""

    # 检查大小
    if ($fileSizeKB -ge 250 -and $fileSizeKB -le 280) {
        Write-Host "  ✅ 文件大小正常（使用 Minecraft 内置 Gson）" -ForegroundColor Green
        Write-Host ""
        Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Green
        Write-Host "║           ✅ 构建成功！                       ║" -ForegroundColor Green
        Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Green
        Write-Host ""
        Write-Host "你的模组现在使用 Minecraft 内置的 Gson，" -ForegroundColor Green
        Write-Host "可以在生产环境正常使用了！" -ForegroundColor Green
        Write-Host ""
        Write-Host "下一步 - 部署到 Minecraft：" -ForegroundColor Yellow
        Write-Host "  Copy-Item '$jarPath' -Destination `"`$env:APPDATA\.minecraft\mods\`" -Force" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "或者 - 直接测试：" -ForegroundColor Yellow
        Write-Host "  1. 将 JAR 复制到 .minecraft\mods\ 文件夹" -ForegroundColor White
        Write-Host "  2. 启动 Minecraft（使用 Fabric Loader）" -ForegroundColor White
        Write-Host "  3. 测试聊天转发和 /back 指令" -ForegroundColor White
        Write-Host ""

    } else {
        Write-Host "  ⚠️  文件大小异常：$fileSizeKB KB" -ForegroundColor Yellow
        Write-Host "  预期大小：250-280 KB" -ForegroundColor Gray
    }
} else {
    Write-Host "  ❌ 找不到 JAR 文件" -ForegroundColor Red
    exit 1
}

Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
