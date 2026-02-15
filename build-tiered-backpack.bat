@echo off
echo ========================================
echo 背包等級系統 - 構建腳本
echo ========================================
echo.

echo [1/3] 清理舊構建文件...
call gradlew.bat clean --no-daemon

echo.
echo [2/3] 構建項目...
call gradlew.bat build --no-daemon

echo.
echo [3/3] 檢查構建結果...
if exist "build\libs\deadrecall-1.5.0.jar" (
    echo.
    echo ========================================
    echo ✅ 構建成功！
    echo ========================================
    echo.
    echo JAR 文件位置:
    dir /B build\libs\*.jar
    echo.
    echo 下一步:
    echo 1. 運行測試: gradlew.bat runClient
    echo 2. 或將 JAR 複製到 mods 文件夾
    echo.
    echo 新功能:
    echo - 基礎背包 (1排9格)
    echo - 標準背包 (3排27格)
    echo - 進階背包 (6排54格)
    echo.
) else (
    echo.
    echo ========================================
    echo ❌ 構建失敗
    echo ========================================
    echo.
    echo 請檢查錯誤信息
    echo.
)

echo 按任意鍵退出...
pause >nul

