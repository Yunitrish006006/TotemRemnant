@echo off
echo ========================================
echo 背包網路協定修復 - 最終測試
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
    echo - 修復網路協定錯誤 (IndexOutOfBoundsException)
    echo - 使用 ExtendedScreenHandlerType 正確同步等級
    echo - 確保客戶端和服務器端界面大小一致
    echo.
    echo 測試步驟:
    echo 1. 啟動遊戲: .\gradlew runClient
    echo 2. 獲取不同等級背包
    echo 3. 測試開啟界面 - 應該沒有網路錯誤
    echo 4. 測試物品保存功能
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

