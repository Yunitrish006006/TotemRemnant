# 🎮 DeadRecall 模組完整文檔

## 📖 項目概述

**DeadRecall** 是一個 Minecraft Fabric 模組，主要功能是實現多等級背包系統和死亡物品收集。

### 🎯 主要功能
- ✅ **多等級背包系統**：4種不同容量的背包
- ✅ **漸進式升級**：從基礎背包逐步升級到高級背包
- ✅ **網路同步**：解決客戶端服務器同步問題
- ✅ **創造模式支援**：方便測試和使用
- ✅ **死亡背包系統**：死亡時自動收集物品到背包實體
- ✅ **Discord 橋接**：聊天和死亡消息發送到 Discord

### 📊 技術資訊
- **Minecraft 版本**：1.21.1
- **模組載入器**：Fabric
- **當前版本**：v1.6.0
- **開發日期**：2026年2月15日

---

## 🎒 背包系統完整說明

### 📋 背包等級總覽

| 等級 | 容量 | 行數 | 獲取方式 | 升級材料 |
|-----|------|------|---------|---------|
| 🟤 **基礎背包** | 9格 | 1排 | 工作台合成 | 箱子 + 8皮革 |
| 🟦 **標準背包** | 18格 | 2排 | 鐵匠台升級 | 箱子 + 基礎 + 鐵錠 |
| ⚫ **進階背包** | 27格 | 3排 | 鐵匠台升級 | 箱子 + 標準 + 鑽石 |
| 🟣 **獄髓背包** | 36格 | 4排 | 鐵匠台升級 | 獄髓模板 + 進階 + 獄髓錠 |
| 💀 **死亡背包** | 27格 | 3排 | 死亡自動生成 | 死亡時收集物品 |

### 🔧 合成與升級配方

#### 基礎背包製作
**工作台合成**：
```
[皮革] [皮革] [皮革]
[皮革] [箱子] [皮革]
[皮革] [皮革] [皮革]
```
**需要材料**：1個箱子 + 8個皮革

#### 高級背包升級
**鐵匠台升級**：

**標準背包**：
- 模板：箱子
- 基礎：基礎背包
- 附加：鐵錠

**進階背包**：
- 模板：箱子
- 基礎：標準背包
- 附加：鑽石

**獄髓背包**：
- 模板：獄髓升級模板
- 基礎：進階背包
- 附加：獄髓錠

### 🎨 背包特性

#### 通用特性
- ✅ **物品保存**：關閉界面後物品不會消失
- ✅ **防止套娃**：背包不能放入背包中
- ✅ **Tooltip 顯示**：顯示等級和容量信息
- ✅ **防火保護**：獄髓背包和死亡背包防火

#### 界面特性
- ✅ **動態大小**：根據等級自動調整界面
- ✅ **網路同步**：無 IndexOutOfBoundsException 錯誤
- ✅ **拖拽支援**：支持 Shift+點擊快速移動物品

---

## 💀 死亡背包系統

### 🎯 功能概述
當玩家死亡時，系統會自動收集死亡掉落的物品，並將其存儲在一個特殊的背包實體中，生成在死亡地點。

### 🔧 工作流程
1. **死亡事件觸發**：監聽玩家死亡事件
2. **收集掉落物品**：搜尋死亡地點附近的掉落物品
3. **創建死亡背包**：將收集的物品存儲到背包中
4. **生成實體**：在死亡地點生成背包實體
5. **通知玩家**：顯示收集成功消息

### 📊 技術細節
- **搜尋範圍**：10格立方體範圍
- **延遲時間**：2秒（給物品掉落時間）
- **數據存儲**：使用 Minecraft 的 ContainerComponent
- **實體特性**：有2秒拾取延遲，防火保護

---

## 💬 Discord 橋接系統

### 🎯 功能特性
- ✅ **聊天消息轉發**：遊戲內聊天消息發送到 Discord
- ✅ **死亡消息通知**：玩家死亡時發送詳細信息到 Discord
- ✅ **座標記錄**：死亡消息包含精確的死亡座標

### 📝 消息格式

#### 聊天消息
```
PlayerName: Hello everyone!
```

#### 死亡消息
```
PlayerName
💀 PlayerName 被僵尸杀死了
📍 座标: X=-550, Y=66, Z=10
```

### 🔧 配置方式
在 `run/config/fabric/deadrecall/discord-bridge.json` 中配置：
```json
{
  "enabled": true,
  "workerUrl": "https://mc-discord-bot.yunitrish0419.workers.dev",
  "apiKey": "your_api_key"
}
```

---

## 🚀 安裝與使用指南

### 📦 安裝步驟

1. **設定 Java 環境**
   ```powershell
   .\setup-java.ps1
   ```
   *這個腳本會自動搜尋並設定 Java 環境*

2. **下載模組**
   ```bash
   # 編譯模組
   .\gradlew build
   ```

3. **安裝到遊戲**
   - 將 `build/libs/deadrecall-1.6.0.jar` 複製到 `.minecraft/mods/` 文件夹

4. **啟動遊戲**
   ```bash
   .\gradlew runClient
   ```

### 🎮 遊戲內使用

#### 獲取背包
- **創造模式**：在工具或功能性物品欄中找到各等級背包
- **生存模式**：合成基礎背包，然後使用鐵匠台升級

#### 使用背包
1. **右鍵點擊**：打開背包界面
2. **放入物品**：將物品拖拽到背包格子中
3. **關閉界面**：物品會自動保存

#### 死亡背包
- 死亡時系統會自動收集物品
- 在死亡地點找到紅色的死亡背包實體
- 拾取並打開即可取回物品

### 🛠️ 開發者工具

#### 快速重建
```powershell
.\rebuild.ps1
```

#### JAR 驗證
```powershell
.\verify-jar.ps1
```

#### Discord 測試
```powershell
.\test-discord-api.ps1
.\test-webhook.ps1
```

---

## 🔧 技術實現

### 📁 核心文件結構
```
src/main/java/com/adaptor/deadrecall/
├── Deadrecall.java                 # 主模組文件
├── item/                          # 物品系統
│   ├── ModItems.java              # 物品註冊
│   ├── TieredBackpackItem.java    # 等級背包
│   ├── DeathBackpackItem.java     # 死亡背包
│   └── BackpackItem.java          # 舊版背包（已棄用）
├── screen/                        # 界面系統
│   └── BackpackScreenHandler.java # 背包界面處理器
├── inventory/                     # 物品欄系統
│   └── BackpackInventory.java     # 背包物品欄
└── mixin/                         # Mixin 注入
    └── ServerPlayerEntityMixin.java # 死亡消息處理
```

### 🎨 資源文件
```
src/main/resources/
├── assets/deadrecall/             # 資源文件
│   ├── models/item/               # 物品模型
│   └── lang/                      # 語言文件
├── data/deadrecall/               # 數據文件
│   ├── recipe/                    # 合成配方
│   └── loot_tables/               # 戰利品表
└── fabric.mod.json                # 模組配置
```

### 🔄 事件處理
- **死亡事件**：`ServerLivingEntityEvents.AFTER_DEATH`
- **聊天事件**：`ServerMessageEvents.CHAT_MESSAGE`
- **命令註冊**：`/back` 指令返回死亡地點

---

## 🐛 故障排除

### 編譯問題
**問題**：JAVA_HOME 未設定
**解決**：執行 `.\setup-java.ps1`

### 背包界面錯誤
**問題**：IndexOutOfBoundsException
**解決**：確保使用最新版本，已修復網路同步問題

### Discord 消息不顯示
**問題**：配置未啟用或 API 錯誤
**解決**：
1. 檢查 `discord-bridge.json` 配置
2. 測試 API 連接：`.\test-discord-api.ps1`
3. 確保 Worker URL 和 API Key 正確

### 死亡背包未生成
**問題**：搜尋範圍內無物品或延遲不足
**解決**：
- 確保死亡時有物品掉落
- 等待2秒讓系統處理
- 檢查死亡地點附近是否有物品

---

## 📈 版本歷史

### v1.6.0 (2026-02-15)
- ✅ 新增死亡背包系統
- ✅ 修復網路同步問題
- ✅ 優化背包界面動態調整

### v1.5.0 (2026-02-13)
- ✅ 新增多等級背包系統
- ✅ 實現漸進式升級
- ✅ 添加創造模式支援

### v1.4.0 (2026-02-10)
- ✅ 新增 Discord 橋接功能
- ✅ 實現聊天消息轉發
- ✅ 添加死亡消息通知

### v1.3.0 (2026-02-08)
- ✅ 實現基礎背包功能
- ✅ 添加物品保存機制
- ✅ 修復界面同步問題

---

## 🤝 貢獻指南

### 開發環境設定
1. 安裝 Java 21
2. 複製項目
3. 執行 `.\setup-java.ps1`
4. 使用 IntelliJ IDEA 或 VS Code 開發

### 編譯與測試
```bash
# 編譯
.\gradlew build

# 運行客戶端
.\gradlew runClient

# 運行服務器
.\gradlew runServer
```

### 代碼規範
- 使用 Java 21 特性
- 遵循 Minecraft Fabric 最佳實踐
- 添加適當的註釋和文檔

---

## 📄 許可證

本項目採用 MIT 許可證 - 詳見 [LICENSE.txt](LICENSE.txt)

---

**DeadRecall - 讓你的 Minecraft 冒險更加便利！** 🎮

*最後更新：2026年2月15日*</content>
<parameter name="filePath">D:\dev\minecraft\DeadRecall\README.md
