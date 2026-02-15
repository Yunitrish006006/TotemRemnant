# 🔧 Java 環境問題快速解決

## ❌ 錯誤訊息
```
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

## 📋 解決方法

### 🚀 方法 1: 自動設定（最簡單）

執行我創建的自動設定腳本：
```powershell
.\setup-java.ps1
```

這個腳本會：
1. ✅ 自動搜尋所有 Java 安裝
2. ✅ 顯示版本並讓你選擇
3. ✅ 自動設定 JAVA_HOME
4. ✅ 驗證 Java 是否可用
5. ✅ 詢問是否立即構建

---

### 🔧 方法 2: 手動設定

如果你知道 Java 的安裝位置：

```powershell
# 設定 JAVA_HOME（替換成你的 Java 路徑）
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 驗證
java -version

# 然後重新構建
.\gradlew.bat clean
.\gradlew.bat build
```

**常見的 Java 安裝位置**：
- `C:\Program Files\Java\jdk-21`
- `C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot`
- `C:\Program Files\Microsoft\jdk-21.x.x`
- `C:\Program Files\Zulu\zulu-21`

---

### 🔍 方法 3: 搜尋 Java 位置

```powershell
# 搜尋 Java 安裝
Get-ChildItem "C:\Program Files\Java" -Directory
Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue

# 或者
where.exe java
```

找到後使用方法 2 設定。

---

## 📥 如果沒有安裝 Java

### 下載並安裝 Java 21

Minecraft 1.21.1 需要 **Java 21**。

**推薦來源**：

1. **Eclipse Temurin (推薦)**
   - 網址：https://adoptium.net/
   - 選擇：Java 21 (LTS)
   - 平台：Windows x64
   - 類型：JDK

2. **Microsoft OpenJDK**
   - 網址：https://www.microsoft.com/openjdk
   - 下載 Java 21

3. **Oracle JDK**
   - 網址：https://www.oracle.com/java/technologies/downloads/

### 安裝步驟

1. 下載安裝程式（.msi 或 .exe）
2. 執行安裝
3. **重要**：安裝時勾選「設定 JAVA_HOME 環境變數」
4. 安裝完成後重啟 PowerShell
5. 執行 `.\setup-java.ps1` 或重新構建

---

## 🎯 完整流程（從頭開始）

```powershell
# 1. 設定 Java 環境
.\setup-java.ps1

# 腳本會自動找到 Java 並設定，然後詢問是否構建
# 選擇 Y 即可自動構建

# 2. 如果沒有自動構建，手動執行
.\rebuild.ps1

# 3. 驗證
.\verify-jar.ps1

# 4. 部署
Copy-Item "build\libs\deadrecall-1.1.0.jar" -Destination "$env:APPDATA\.minecraft\mods\" -Force
```

---

## 🤔 為什麼會遇到這個問題？

### IntelliJ IDEA 開發環境

在 IDE 中開發時：
- ✅ IDE 有內建的 JDK
- ✅ IDE 自動管理 Java 環境
- ✅ 可以直接構建和運行

### 命令列環境

在 PowerShell 中：
- ❌ 沒有 JAVA_HOME 環境變數
- ❌ Java 不在 PATH 中
- ❌ Gradle 找不到 Java

**解決方案**：設定 JAVA_HOME 環境變數

---

## 📝 環境變數說明

### JAVA_HOME
指向 Java 安裝目錄（例如：`C:\Program Files\Java\jdk-21`）

### PATH
包含 Java 執行檔目錄（例如：`C:\Program Files\Java\jdk-21\bin`）

### 設定方式

**臨時設定（只在當前 PowerShell）**：
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

**永久設定（所有 PowerShell）**：
```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21", [System.EnvironmentVariableTarget]::User)
```

或使用 GUI：
1. 搜尋「環境變數」
2. 點擊「編輯系統環境變數」
3. 點擊「環境變數」按鈕
4. 新增 `JAVA_HOME` 變數

---

## ✅ 驗證 Java 設定

```powershell
# 檢查 JAVA_HOME
echo $env:JAVA_HOME

# 檢查 Java 版本
java -version

# 應該顯示類似：
# openjdk version "21.0.x" 2024-xx-xx
# OpenJDK Runtime Environment Temurin-21+xx
# OpenJDK 64-Bit Server VM Temurin-21+xx
```

---

## 🎬 現在執行

**最簡單的方法**：
```powershell
.\setup-java.ps1
```

腳本會自動處理一切，然後詢問是否構建模組！
