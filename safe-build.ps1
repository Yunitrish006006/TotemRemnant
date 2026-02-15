# 安全的重新构建脚本

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     重新构建 DeadRecall 模组                  ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

Write-Host "使用配置：compileOnly Gson（使用 Minecraft 内置）" -ForegroundColor Yellow
Write-Host ""

# 步骤 1: 清理 build 目录
Write-Host "【步骤 1/3】清理 build 目录..." -ForegroundColor Yellow

if (Test-Path "build") {
    Remove-Item "build" -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "  ✅ build 目录已删除" -ForegroundColor Green
} else {
    Write-Host "  ✅ build 目录不存在，跳过" -ForegroundColor Green
}

Write-Host ""
Start-Sleep -Seconds 1

# 步骤 2: 构建
Write-Host "【步骤 2/3】构建模组..." -ForegroundColor Yellow
Write-Host "  执行: gradlew.bat build" -ForegroundColor Gray
Write-Host ""

$buildStart = Get-Date

try {
    # 使用普通的 build 命令，不使用 --no-daemon
    $buildOutput = & .\gradlew.bat build 2>&1 | Out-String
    $buildEnd = Get-Date
    $buildTime = ($buildEnd - $buildStart).TotalSeconds

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ 构建成功（耗时 $([math]::Round($buildTime, 1)) 秒）" -ForegroundColor Green
    } else {
        Write-Host "  ❌ 构建失败" -ForegroundColor Red
        Write-Host ""
        Write-Host "构建输出：" -ForegroundColor Yellow
        Write-Host $buildOutput
        Write-Host ""
        exit 1
    }
} catch {
    Write-Host "  ❌ 构建失败：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Start-Sleep -Seconds 1

# 步骤 3: 验证
Write-Host "【步骤 3/3】验证 JAR 文件..." -ForegroundColor Yellow
Write-Host ""

$jarPath = "build\libs\deadrecall-1.1.0.jar"

if (-not (Test-Path $jarPath)) {
    Write-Host "  ❌ 找不到 JAR 文件：$jarPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "检查 build\libs 目录内容：" -ForegroundColor Yellow
    if (Test-Path "build\libs") {
        Get-ChildItem "build\libs" | ForEach-Object {
            Write-Host "    - $($_.Name)" -ForegroundColor Gray
        }
    } else {
        Write-Host "    build\libs 目录不存在" -ForegroundColor Red
    }
    Write-Host ""
    exit 1
}

$fileSize = (Get-Item $jarPath).Length
$fileSizeKB = [math]::Round($fileSize / 1KB, 2)

Write-Host "  ✅ JAR 文件已生成" -ForegroundColor Green
Write-Host "  路径: $jarPath" -ForegroundColor Cyan
Write-Host "  大小: $fileSizeKB KB" -ForegroundColor Cyan
Write-Host ""

# 详细验证
if ($fileSizeKB -lt 50) {
    Write-Host "  ❌ 文件大小异常小！" -ForegroundColor Red
    Write-Host "  当前: $fileSizeKB KB" -ForegroundColor Red
    Write-Host "  预期: 250-280 KB" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "可能的原因：" -ForegroundColor Yellow
    Write-Host "  1. 源代码没有被编译" -ForegroundColor White
    Write-Host "  2. 类文件没有被打包" -ForegroundColor White
    Write-Host "  3. 构建配置问题" -ForegroundColor White
    Write-Host ""
    Write-Host "建议：在 IntelliJ IDEA 中构建" -ForegroundColor Yellow
    Write-Host "  1. 右键点击 build.gradle" -ForegroundColor White
    Write-Host "  2. 选择 'Reload Gradle Project'" -ForegroundColor White
    Write-Host "  3. Build > Rebuild Project" -ForegroundColor White
    Write-Host "  4. 查看 build\libs\ 目录" -ForegroundColor White
    Write-Host ""

} elseif ($fileSizeKB -ge 250 -and $fileSizeKB -le 280) {
    Write-Host "  ✅ 文件大小正常" -ForegroundColor Green
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║           ✅ 构建成功！                       ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "模组使用 Minecraft 内置 Gson，可以部署了！" -ForegroundColor Green
    Write-Host ""
    Write-Host "部署命令：" -ForegroundColor Yellow
    Write-Host "  Copy-Item '$jarPath' -Destination `"`$env:APPDATA\.minecraft\mods\`" -Force" -ForegroundColor Cyan
    Write-Host ""

} else {
    Write-Host "  ⚠️  文件大小：$fileSizeKB KB" -ForegroundColor Yellow
    Write-Host "  预期范围：250-280 KB" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  模组应该可以使用，但建议检查内容是否完整" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
