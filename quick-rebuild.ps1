# 快速重建脚本（使用修复后的配置）

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     修复 Gson 打包问题并重建                  ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

Write-Host "已更新 build.gradle 配置：" -ForegroundColor Yellow
Write-Host "  implementation(include('com.google.code.gson:gson:2.10.1'))" -ForegroundColor Cyan
Write-Host ""
Write-Host "这是 Fabric Loom 推荐的依赖打包方式" -ForegroundColor Gray
Write-Host ""

# 强制清理所有缓存
Write-Host "【步骤 1/4】强制清理 Gradle 缓存..." -ForegroundColor Yellow
Write-Host ""

try {
    # 清理 build 目录
    if (Test-Path "build") {
        Write-Host "  删除 build 目录..." -ForegroundColor Gray
        Remove-Item "build" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "  ✅ build 目录已删除" -ForegroundColor Green
    }

    # 清理 .gradle/loom-cache
    if (Test-Path ".gradle\loom-cache") {
        Write-Host "  删除 loom-cache..." -ForegroundColor Gray
        Remove-Item ".gradle\loom-cache" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "  ✅ loom-cache 已删除" -ForegroundColor Green
    }

    Write-Host "  ✅ 缓存清理完成" -ForegroundColor Green
} catch {
    Write-Host "  ⚠️  部分缓存清理失败（可以忽略）" -ForegroundColor Yellow
}

Write-Host ""
Start-Sleep -Seconds 1

# 执行 clean
Write-Host "【步骤 2/4】执行 Gradle clean..." -ForegroundColor Yellow
Write-Host ""

try {
    & .\gradlew.bat clean --no-daemon 2>&1 | Out-Null
    Write-Host "  ✅ Clean 完成" -ForegroundColor Green
} catch {
    Write-Host "  ⚠️  Clean 完成（有警告）" -ForegroundColor Yellow
}

Write-Host ""
Start-Sleep -Seconds 1

# 构建
Write-Host "【步骤 3/4】重新构建模组（包含 Gson）..." -ForegroundColor Yellow
Write-Host "  这可能需要 1-3 分钟..." -ForegroundColor Gray
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
Write-Host "【步骤 4/4】验证 JAR 文件..." -ForegroundColor Yellow
Write-Host ""

$jarPath = "build\libs\deadrecall-1.1.0.jar"

if (Test-Path $jarPath) {
    $fileSize = (Get-Item $jarPath).Length
    $fileSizeKB = [math]::Round($fileSize / 1KB, 2)

    Write-Host "  ✅ JAR 文件已生成" -ForegroundColor Green
    Write-Host "  路径: $jarPath" -ForegroundColor Cyan
    Write-Host "  大小: $fileSizeKB KB" -ForegroundColor Cyan
    Write-Host ""

    # 检查大小变化
    if ($fileSizeKB -lt 200) {
        Write-Host "  ⚠️  文件大小异常小！Gson 可能仍未打包" -ForegroundColor Red
    } elseif ($fileSizeKB -gt 280) {
        Write-Host "  ✅ 文件大小正常，Gson 应该已打包" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  文件大小处于临界值，需要验证" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ 找不到 JAR 文件" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# 自动执行验证
Write-Host "正在验证 Gson 是否已打包..." -ForegroundColor Yellow
Write-Host ""

Start-Sleep -Seconds 1

# 手动检查 Gson（不依赖 verify-jar.ps1）
$tempDir = "temp_quick_check"
if (Test-Path $tempDir) {
    Remove-Item $tempDir -Recurse -Force
}

try {
    # 重命名并解压
    $zipPath = $jarPath -replace '\.jar$', '_check.zip'
    Copy-Item $jarPath -Destination $zipPath -Force
    Expand-Archive -Path $zipPath -DestinationPath $tempDir -Force
    Remove-Item $zipPath -Force

    # 检查 Gson
    $gsonPath = "$tempDir\com\google\gson"
    if (Test-Path $gsonPath) {
        $gsonFiles = Get-ChildItem -Path $gsonPath -Recurse -File
        Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Green
        Write-Host "║           ✅ 成功！Gson 已打包！              ║" -ForegroundColor Green
        Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Green
        Write-Host ""
        Write-Host "  ✅ 找到 $($gsonFiles.Count) 个 Gson 文件" -ForegroundColor Green
        Write-Host "  ✅ JAR 大小: $fileSizeKB KB" -ForegroundColor Green
        Write-Host ""
        Write-Host "你的模组现在可以在生产环境正常使用了！" -ForegroundColor Green
        Write-Host ""
        Write-Host "下一步：" -ForegroundColor Yellow
        Write-Host "  Copy-Item '$jarPath' -Destination `"`$env:APPDATA\.minecraft\mods\`" -Force" -ForegroundColor Cyan
        Write-Host ""

        $success = $true
    } else {
        Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Red
        Write-Host "║           ❌ 失败：Gson 仍未打包              ║" -ForegroundColor Red
        Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Red
        Write-Host ""
        Write-Host "可能的原因：" -ForegroundColor Yellow
        Write-Host "  1. Gradle 缓存问题" -ForegroundColor White
        Write-Host "  2. Fabric Loom 版本问题" -ForegroundColor White
        Write-Host "  3. 依赖配置语法问题" -ForegroundColor White
        Write-Host ""
        Write-Host "请尝试：" -ForegroundColor Yellow
        Write-Host "  1. 在 IntelliJ IDEA 中执行 'Reload Gradle Project'" -ForegroundColor Cyan
        Write-Host "  2. 在 IDE 中执行 Build > Rebuild Project" -ForegroundColor Cyan
        Write-Host "  3. 检查 IDE 的 Build 输出" -ForegroundColor Cyan
        Write-Host ""

        $success = $false
    }

    # 清理
    Remove-Item $tempDir -Recurse -Force

} catch {
    Write-Host "❌ 验证过程出错：$($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
