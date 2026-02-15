# 终止 Minecraft 进程并重新构建

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     终止 Minecraft 进程并重新构建             ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 检查是否有 Minecraft/Java 进程占用文件
Write-Host "检查运行中的 Minecraft 进程..." -ForegroundColor Yellow
Write-Host ""

$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue

if ($javaProcesses) {
    Write-Host "找到 $($javaProcesses.Count) 个 Java 进程：" -ForegroundColor Yellow
    Write-Host ""

    foreach ($proc in $javaProcesses) {
        $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($proc.Id)").CommandLine

        if ($commandLine -match "minecraft|fabric|forge") {
            Write-Host "  [PID $($proc.Id)] Minecraft 相关进程" -ForegroundColor Red
            Write-Host "    启动时间: $($proc.StartTime)" -ForegroundColor Gray
            Write-Host "    内存使用: $([math]::Round($proc.WorkingSet64 / 1MB, 2)) MB" -ForegroundColor Gray
        } else {
            Write-Host "  [PID $($proc.Id)] 其他 Java 进程" -ForegroundColor Gray
        }
    }

    Write-Host ""
    Write-Host "⚠️  需要关闭 Minecraft 才能继续构建" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "是否要终止所有 Java 进程？(Y/N)" -ForegroundColor Yellow
    Write-Host "（这会关闭 Minecraft 客户端，但不会影响 IDE）" -ForegroundColor Gray
    $kill = Read-Host

    if ($kill -eq "Y" -or $kill -eq "y") {
        Write-Host ""
        Write-Host "正在终止 Java 进程..." -ForegroundColor Yellow

        foreach ($proc in $javaProcesses) {
            try {
                Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
                Write-Host "  ✅ 已终止进程 $($proc.Id)" -ForegroundColor Green
            } catch {
                Write-Host "  ⚠️  无法终止进程 $($proc.Id)" -ForegroundColor Yellow
            }
        }

        Write-Host ""
        Write-Host "等待进程完全终止..." -ForegroundColor Yellow
        Start-Sleep -Seconds 3
        Write-Host "✅ 进程已终止" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "❌ 已取消构建" -ForegroundColor Red
        Write-Host ""
        Write-Host "请手动关闭 Minecraft 客户端，然后重新运行此脚本" -ForegroundColor Yellow
        Write-Host ""
        exit 1
    }
} else {
    Write-Host "✅ 没有发现运行中的 Java 进程" -ForegroundColor Green
}

Write-Host ""
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# 询问是否继续构建
Write-Host "是否要立即开始构建？(Y/N)" -ForegroundColor Yellow
$build = Read-Host

if ($build -eq "Y" -or $build -eq "y") {
    Write-Host ""
    Write-Host "执行构建脚本..." -ForegroundColor Yellow
    Write-Host ""
    & .\quick-rebuild.ps1
} else {
    Write-Host ""
    Write-Host "下一步：" -ForegroundColor Yellow
    Write-Host ".\quick-rebuild.ps1" -ForegroundColor Cyan
    Write-Host ""
}
