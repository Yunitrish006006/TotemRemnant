@echo off
echo ========================================
echo 背包鐵砧升級系統 - 構建腳本
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
    echo 新功能:
    echo - 基礎背包 (9格) - 工作台合成
    echo - 標準背包 (27格) - 鐵砧台升級
    echo - 進階背包 (54格) - 鐵砧台升級
    echo - 獄髓背包 (81格) - 鐵砧台升級
    echo.
    echo 升級配方:
    echo - 基礎→標準: 箱子 + 基礎背包 + 鐵錠
    echo - 標準→進階: 箱子 + 標準背包 + 鑽石
    echo - 進階→獄髓: 獄髓模板 + 進階背包 + 獄隨錠
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

