@echo off
echo ========================================
echo 背包存储修复 - 构建脚本
echo ========================================
echo.

echo [1/3] 清理旧构建文件...
call gradlew.bat clean --no-daemon

echo.
echo [2/3] 构建项目...
call gradlew.bat build --no-daemon

echo.
echo [3/3] 检查构建结果...
if exist "build\libs\deadrecall-1.4.1.jar" (
    echo.
    echo ========================================
    echo ✅ 构建成功！
    echo ========================================
    echo.
    echo JAR 文件位置:
    dir /B build\libs\*.jar
    echo.
    echo 下一步:
    echo 1. 运行测试: gradlew.bat runClient
    echo 2. 或将 JAR 复制到 mods 文件夹
    echo.
) else (
    echo.
    echo ========================================
    echo ❌ 构建失败
    echo ========================================
    echo.
    echo 请检查错误信息
    echo.
)

echo 按任意键退出...
pause >nul

