# 尋找並設定 Java 環境

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     Java 環境檢測與設定工具                   ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

Write-Host "正在搜尋 Java 安裝位置..." -ForegroundColor Yellow
Write-Host ""

# 常見的 Java 安裝位置
$commonPaths = @(
    "$env:ProgramFiles\Java",
    "$env:ProgramFiles\Eclipse Adoptium",
    "$env:ProgramFiles\Microsoft",
    "$env:ProgramFiles\Zulu",
    "${env:ProgramFiles(x86)}\Java",
    "$env:LOCALAPPDATA\Programs\Eclipse Adoptium",
    "C:\Program Files\Eclipse Foundation",
    "$env:USERPROFILE\.jdks"
)

$javaHomes = @()

foreach ($path in $commonPaths) {
    if (Test-Path $path) {
        Write-Host "檢查: $path" -ForegroundColor Gray

        # 搜尋 JDK 目錄
        $jdks = Get-ChildItem -Path $path -Directory -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -match "jdk" -or $_.Name -match "java" -or $_.Name -match "temurin"
        }

        foreach ($jdk in $jdks) {
            $javaBin = Join-Path $jdk.FullName "bin\java.exe"
            if (Test-Path $javaBin) {
                $javaHomes += $jdk.FullName
            }
        }
    }
}

# 也檢查 Gradle 使用的 Java
$gradleJava = $env:JAVA_HOME
if ($gradleJava -and (Test-Path "$gradleJava\bin\java.exe")) {
    if ($javaHomes -notcontains $gradleJava) {
        $javaHomes += $gradleJava
    }
}

Write-Host ""

if ($javaHomes.Count -eq 0) {
    Write-Host "❌ 找不到 Java 安裝" -ForegroundColor Red
    Write-Host ""
    Write-Host "請執行以下步驟：" -ForegroundColor Yellow
    Write-Host "1. 下載並安裝 Java 21（Minecraft 1.21.1 需要）" -ForegroundColor White
    Write-Host "   推薦來源：https://adoptium.net/" -ForegroundColor Cyan
    Write-Host "2. 安裝後重新執行此腳本" -ForegroundColor White
    Write-Host ""
    exit 1
}

Write-Host "✅ 找到 $($javaHomes.Count) 個 Java 安裝：" -ForegroundColor Green
Write-Host ""

# 顯示每個 Java 版本
$validJavas = @()
for ($i = 0; $i -lt $javaHomes.Count; $i++) {
    $javaHome = $javaHomes[$i]
    $javaBin = Join-Path $javaHome "bin\java.exe"

    try {
        $versionOutput = & $javaBin -version 2>&1 | Select-Object -First 1
        $version = "Unknown"

        if ($versionOutput -match 'version "?(\d+)') {
            $version = $matches[1]
        }

        Write-Host "[$($i + 1)] Java $version" -ForegroundColor Cyan
        Write-Host "    路徑: $javaHome" -ForegroundColor Gray

        $validJavas += @{
            Index = $i + 1
            Version = $version
            Path = $javaHome
        }
    } catch {
        Write-Host "[$($i + 1)] (無法檢測版本)" -ForegroundColor Yellow
        Write-Host "    路徑: $javaHome" -ForegroundColor Gray
    }

    Write-Host ""
}

# 檢查是否有 Java 21
$java21 = $validJavas | Where-Object { $_.Version -eq "21" } | Select-Object -First 1

if ($java21) {
    Write-Host "✅ 找到 Java 21（Minecraft 1.21.1 推薦版本）" -ForegroundColor Green
    Write-Host ""
    Write-Host "是否要使用 Java 21？(Y/N，推薦選 Y)" -ForegroundColor Yellow
    $useRecommended = Read-Host

    if ($useRecommended -eq "Y" -or $useRecommended -eq "y" -or $useRecommended -eq "") {
        $selectedJava = $java21.Path
    } else {
        Write-Host ""
        Write-Host "請選擇要使用的 Java 版本 (1-$($validJavas.Count)):" -ForegroundColor Yellow
        $selection = Read-Host

        if ($selection -match '^\d+$' -and [int]$selection -ge 1 -and [int]$selection -le $validJavas.Count) {
            $selectedJava = $javaHomes[[int]$selection - 1]
        } else {
            Write-Host "❌ 無效的選擇" -ForegroundColor Red
            exit 1
        }
    }
} else {
    Write-Host "⚠️  未找到 Java 21（Minecraft 1.21.1 推薦版本）" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "請選擇要使用的 Java 版本 (1-$($validJavas.Count)):" -ForegroundColor Yellow
    $selection = Read-Host

    if ($selection -match '^\d+$' -and [int]$selection -ge 1 -and [int]$selection -le $validJavas.Count) {
        $selectedJava = $javaHomes[[int]$selection - 1]
    } else {
        Write-Host "❌ 無效的選擇" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  設定 JAVA_HOME" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Write-Host "選擇的 Java 路徑：" -ForegroundColor Green
Write-Host $selectedJava -ForegroundColor Cyan
Write-Host ""

# 設定環境變數（當前 Session）
$env:JAVA_HOME = $selectedJava
$env:PATH = "$selectedJava\bin;$env:PATH"

Write-Host "✅ 已設定 JAVA_HOME（當前 PowerShell Session）" -ForegroundColor Green
Write-Host ""

# 驗證 Java
Write-Host "驗證 Java 設定..." -ForegroundColor Yellow
try {
    $javaVersion = & java -version 2>&1 | Out-String
    Write-Host "✅ Java 可用" -ForegroundColor Green
    Write-Host $javaVersion -ForegroundColor Gray
} catch {
    Write-Host "❌ 無法執行 Java" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# 詢問是否要永久設定
Write-Host "是否要永久設定 JAVA_HOME（系統環境變數）？(Y/N)" -ForegroundColor Yellow
Write-Host "（如果選 N，只在當前 PowerShell 有效）" -ForegroundColor Gray
$permanent = Read-Host

if ($permanent -eq "Y" -or $permanent -eq "y") {
    Write-Host ""
    Write-Host "正在設定系統環境變數（需要管理員權限）..." -ForegroundColor Yellow

    try {
        # 設定用戶環境變數
        [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $selectedJava, [System.EnvironmentVariableTarget]::User)

        Write-Host "✅ 已設定 JAVA_HOME 用戶環境變數" -ForegroundColor Green
        Write-Host "   重新開啟 PowerShell 後會永久生效" -ForegroundColor Gray
    } catch {
        Write-Host "⚠️  無法設定環境變數：$($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "   可能需要管理員權限" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "╔════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║           ✅ Java 環境設定完成！              ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

# 詢問是否繼續構建
Write-Host "是否要立即構建模組？(Y/N)" -ForegroundColor Yellow
$build = Read-Host

if ($build -eq "Y" -or $build -eq "y") {
    Write-Host ""
    Write-Host "執行構建腳本..." -ForegroundColor Yellow
    Write-Host ""
    & .\rebuild.ps1
} else {
    Write-Host ""
    Write-Host "下一步：執行構建腳本" -ForegroundColor Yellow
    Write-Host ".\rebuild.ps1" -ForegroundColor Cyan
    Write-Host ""
}
