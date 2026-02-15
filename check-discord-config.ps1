# Discord Bridge 配置检查和修复脚本

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     Discord Bridge 配置检查                   ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# 可能的配置文件位置
$possiblePaths = @(
    "$env:APPDATA\.minecraft\config\discord-bridge.json",
    "$env:APPDATA\ModrinthApp\profiles\*\config\discord-bridge.json",
    "D:\dev\minecraft\DeadRecall\run\config\discord-bridge.json"
)

Write-Host "正在搜寻配置文件..." -ForegroundColor Yellow
Write-Host ""

$foundConfigs = @()

foreach ($path in $possiblePaths) {
    if ($path -like "*\*\*") {
        # 处理通配符路径
        $baseDir = Split-Path (Split-Path $path)
        if (Test-Path $baseDir) {
            $files = Get-ChildItem $path -ErrorAction SilentlyContinue
            foreach ($file in $files) {
                $foundConfigs += $file.FullName
            }
        }
    } else {
        if (Test-Path $path) {
            $foundConfigs += $path
        }
    }
}

if ($foundConfigs.Count -eq 0) {
    Write-Host "❌ 找不到任何 discord-bridge.json 配置文件" -ForegroundColor Red
    Write-Host ""
    Write-Host "可能的原因：" -ForegroundColor Yellow
    Write-Host "1. 你使用的 Minecraft 启动器不是标准的" -ForegroundColor White
    Write-Host "2. 游戏还没有生成配置文件" -ForegroundColor White
    Write-Host ""
    Write-Host "请手动查找配置文件：" -ForegroundColor Yellow
    Write-Host "1. 打开 Minecraft 启动器" -ForegroundColor White
    Write-Host "2. 找到游戏实例/配置文件夹" -ForegroundColor White
    Write-Host "3. 进入 config 文件夹" -ForegroundColor White
    Write-Host "4. 查找或创建 discord-bridge.json" -ForegroundColor White
    Write-Host ""

    # 显示示例配置
    Write-Host "示例配置内容：" -ForegroundColor Cyan
    Write-Host @"
{
  "enabled": true,
  "workerUrl": "https://mc-discord-bot.yunitrish0419.workers.dev",
  "apiKey": "mc_ak_7Xp9Qm3vKsW2nF8jRtYb6LdA4eHcZu"
}
"@ -ForegroundColor Gray
    Write-Host ""
    exit 1
}

Write-Host "✅ 找到 $($foundConfigs.Count) 个配置文件：" -ForegroundColor Green
Write-Host ""

# 检查每个配置文件
$needsUpdate = @()

foreach ($configPath in $foundConfigs) {
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray
    Write-Host "配置文件：" -ForegroundColor Cyan
    Write-Host $configPath -ForegroundColor White
    Write-Host ""

    try {
        $content = Get-Content $configPath -Raw
        $config = $content | ConvertFrom-Json

        Write-Host "当前配置：" -ForegroundColor Yellow
        Write-Host "  enabled: $($config.enabled)" -ForegroundColor $(if ($config.enabled) { "Green" } else { "Red" })
        Write-Host "  workerUrl: $($config.workerUrl)" -ForegroundColor Gray
        Write-Host "  apiKey: $($config.apiKey.Substring(0, [Math]::Min(15, $config.apiKey.Length)))..." -ForegroundColor Gray
        Write-Host ""

        # 检查问题
        $hasIssues = $false

        if (-not $config.enabled) {
            Write-Host "  ⚠️  问题：enabled 设置为 false" -ForegroundColor Yellow
            $hasIssues = $true
        }

        if ([string]::IsNullOrWhiteSpace($config.workerUrl)) {
            Write-Host "  ⚠️  问题：workerUrl 为空" -ForegroundColor Yellow
            $hasIssues = $true
        }

        if ([string]::IsNullOrWhiteSpace($config.apiKey)) {
            Write-Host "  ⚠️  问题：apiKey 为空" -ForegroundColor Yellow
            $hasIssues = $true
        }

        if ($hasIssues) {
            $needsUpdate += $configPath
        } else {
            Write-Host "  ✅ 配置正常" -ForegroundColor Green
        }

    } catch {
        Write-Host "  ❌ 读取失败：$($_.Exception.Message)" -ForegroundColor Red
        $needsUpdate += $configPath
    }

    Write-Host ""
}

if ($needsUpdate.Count -eq 0) {
    Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║           ✅ 所有配置都正常！                 ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "如果 Discord 消息还是没有显示，请检查：" -ForegroundColor Yellow
    Write-Host "1. Cloudflare Worker 是否正常运行" -ForegroundColor White
    Write-Host "2. Discord Webhook URL 是否有效" -ForegroundColor White
    Write-Host "3. API Key 是否正确" -ForegroundColor White
    Write-Host ""
    exit 0
}

# 询问是否修复
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray
Write-Host ""
Write-Host "发现 $($needsUpdate.Count) 个配置文件需要更新" -ForegroundColor Yellow
Write-Host ""
Write-Host "是否要自动修复配置？(Y/N)" -ForegroundColor Yellow
$fix = Read-Host

if ($fix -ne "Y" -and $fix -ne "y") {
    Write-Host ""
    Write-Host "已取消" -ForegroundColor Gray
    exit 0
}

# 修复配置
Write-Host ""
Write-Host "正在修复配置..." -ForegroundColor Yellow
Write-Host ""

$fixedConfig = @{
    enabled = $true
    workerUrl = "https://mc-discord-bot.yunitrish0419.workers.dev"
    apiKey = "mc_ak_7Xp9Qm3vKsW2nF8jRtYb6LdA4eHcZu"
}

foreach ($configPath in $needsUpdate) {
    try {
        # 备份原文件
        $backupPath = "$configPath.backup"
        if (Test-Path $configPath) {
            Copy-Item $configPath $backupPath -Force
            Write-Host "  ✅ 已备份：$backupPath" -ForegroundColor Green
        }

        # 写入新配置
        $fixedConfig | ConvertTo-Json | Set-Content $configPath -Encoding UTF8
        Write-Host "  ✅ 已更新：$configPath" -ForegroundColor Green

    } catch {
        Write-Host "  ❌ 更新失败：$configPath" -ForegroundColor Red
        Write-Host "     错误：$($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║           ✅ 配置已修复！                     ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "下一步：" -ForegroundColor Yellow
Write-Host "1. 重启 Minecraft" -ForegroundColor White
Write-Host "2. 查看日志，应该看到：" -ForegroundColor White
Write-Host "   [DeadRecall] [DiscordBridge] 已启用，Worker URL: ..." -ForegroundColor Cyan
Write-Host "3. 发送聊天消息测试" -ForegroundColor White
Write-Host ""
