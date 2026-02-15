@echo off
echo ========================================
echo 背包網路協定修復 - 構建腳本
echo ========================================
echo.

echo [1/3] 清理舊構建文件...
call gradlew.bat clean --no-daemon

echo.
echo [2/3] 構建項目...
call gradlew.bat build --no-daemon

echo.
echo [3/3] 檢查構建結果...
if exist "build\libs\deadrecall-1.6.0.jar" (
    echo.
    echo ========================================
    echo ✅ 構建成功！
    echo ========================================
    echo.
    echo JAR 文件位置:
    dir /B build\libs\*.jar
    echo.
    echo 修復內容:
    echo - 修復網路協定錯誤
    echo - 調整背包大小：
    echo   基礎: 1排 (9格)
    echo   標準: 2排 (18格)
    echo   進階: 3排 (27格)
    echo   獄髓: 4排 (36格)
    echo.
    echo 下一步:
    echo 1. 運行測試: gradlew.bat runClient
    echo 2. 測試各等級背包是否正常開啟
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

