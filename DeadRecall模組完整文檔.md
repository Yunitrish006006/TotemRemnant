# 🎮 DeadRecall 模組完整文檔

## 📖 項目概述

**DeadRecall** 是一個 Minecraft Fabric 模組，主要功能是實現多等級背包系統。

### 🎯 主要功能
- ✅ **多等級背包系統**：4種不同容量的背包
- ✅ **漸進式升級**：從基礎背包逐步升級到高級背包
- ✅ **網路同步**：解決客戶端服務器同步問題
- ✅ **創造模式支援**：方便測試和使用
- ✅ **死亡背包系統**：死亡時自動收集物品到背包實體

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
| 🟦 **標準背包** | 18格 | 2排 | 鐵砧升級 | 箱子 + 基礎 + 鐵錠 |
| ⚫ **進階背包** | 27格 | 3排 | 鐵砧升級 | 箱子 + 標準 + 鑽石 |
| 🟣 **獄髓背包** | 36格 | 4排 | 鐵砧升級 | 獄髓模板 + 進階 + 獄髓錠 |
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
**鐵砧台升級**：

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
- ✅ **防火保護**：獄髓背包防火

#### 界面特性
- ✅ **動態大小**：根據等級自動調整界面
- ✅ **網路同步**：無 IndexOutOfBoundsException 錯誤
- ✅ **拖拽支援**：支持 Shift+點擊快速移動物品

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

### 🛠️ 開發環境設定

#### Java 環境問題
如果遇到 `JAVA_HOME is not set` 錯誤：

**一鍵解決**：
```powershell
.\setup-java.ps1
```

**手動解決**：
1. 確保安裝了 JDK 21
2. 設定環境變數：
   - `JAVA_HOME` = `C:\Program Files\Java\jdk-21`
   - `PATH` 包含 `%JAVA_HOME%\bin`

#### 常用腳本
```powershell
# 設定 Java
.\setup-java.ps1

# 重新構建
.\rebuild.ps1

# 驗證 JAR
.\verify-jar.ps1

# 安全構建
.\safe-build.ps1

# 快速重新構建
.\quick-rebuild.ps1
```

### 🎮 遊戲中使用

#### 生存模式
1. **收集材料**：箱子和皮革
2. **製作基礎背包**：工作台合成
3. **逐步升級**：使用鐵砧台升級

#### 創造模式
1. **開啟物品欄**（E鍵）
2. **工具標籤** 或 **功能性標籤** 找到背包
3. **或使用搜索**：輸入 "backpack"
4. **Shift+點擊** 獲取背包

#### 指令獲取
```bash
/give @s deadrecall:backpack_basic
/give @s deadrecall:backpack_standard
/give @s deadrecall:backpack_advanced
/give @s deadrecall:backpack_netherite
```

---

## 💀 死亡背包系統

### 🎯 功能概述
**死亡背包**是一個特殊的死亡物品收集系統，當玩家死亡時會自動收集死亡掉落的物品，並將其存儲在一個特殊的背包實體中，生成在死亡地點。

### 🔧 工作原理

#### 自動收集流程
1. **玩家死亡**：當玩家因任何原因死亡時
2. **物品掉落**：玩家的物品正常掉落到地上
3. **系統收集**：模組自動收集附近的掉落物品
4. **背包生成**：在死亡地點生成死亡背包實體
5. **物品存儲**：所有收集的物品存儲在背包中

#### 技術實現
```java
// 死亡事件監聽
ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
    if (entity instanceof ServerPlayerEntity player) {
        handlePlayerDeath(player);
    }
});

// 收集掉落物品
Box searchBox = new Box(deathPos).expand(10.0);
List<ItemEntity> droppedItems = world.getEntitiesByClass(ItemEntity.class, searchBox,
    itemEntity -> itemEntity.getOwner() != null && itemEntity.getOwner().equals(player));
```

### 🎮 使用方式

#### 生存模式體驗
1. **正常死亡**：當你死亡時，物品會正常掉落
2. **系統收集**：模組自動收集附近的掉落物品
3. **背包生成**：死亡地點會生成一個死亡背包實體
4. **收到通知**：聊天欄顯示 "你的物品已被收集到死亡背包中！"
5. **返回取回**：返回死亡地點拾取背包

#### 取回物品
1. **找到背包**：返回死亡地點找到紅色的死亡背包
2. **拾取背包**：右鍵點擊拾取死亡背包物品
3. **打開背包**：手持背包右鍵打開界面
4. **取回物品**：從27格的背包中取出需要的物品

### 📊 背包特性

#### 基本信息
- **名稱**：死亡背包 (Death Backpack)
- **容量**：27格 (3×9 網格)
- **獲取方式**：死亡時自動生成
- **特殊屬性**：防火保護

#### 搜尋範圍
- **搜尋半徑**：10 格立方體範圍
- **搜尋條件**：只收集屬於死亡玩家的物品
- **處理延遲**：2 秒（給物品掉落時間）

#### 安全機制
- **所有者檢查**：只收集屬於死亡玩家的物品
- **拾取延遲**：背包實體有 2 秒拾取延遲
- **防火保護**：背包物品不會被火燒毀

### 🎨 外觀特點

#### 物品外觀
- **圖標**：使用與普通背包相同的 bundle 圖標
- **顏色**：紅色邊框表示特殊狀態
- **Tooltip**：顯示 "死亡背包 - 收集死亡掉落物品"

#### 實體外觀
- **生成位置**：精確的死亡地點
- **視覺效果**：紅色光暈效果
- **拾取提示**：2 秒延遲後可拾取

### 🧪 測試指南

#### 基本測試
1. **創造模式獲取**：獲取死亡背包物品測試界面
2. **放入物品**：測試物品存儲功能
3. **死亡測試**：故意死亡測試自動收集功能

#### 邊界測試
1. **空死亡測試**：空物品欄死亡，確認沒有背包生成
2. **遠程物品測試**：物品掉落在遠處，確認不會被收集
3. **多玩家測試**：多人同時死亡，確認各自物品正確收集

### 🐛 故障排除

#### 背包沒有生成
**可能原因**：
- 死亡時沒有掉落物品
- 搜尋範圍內沒有找到物品
- 服務器延遲導致物品還沒掉落

**解決方案**：
- 確保死亡時有物品掉落
- 檢查死亡地點附近是否有物品
- 等待幾秒鐘讓系統處理

#### 物品沒有被收集
**可能原因**：
- 物品掉落在搜尋範圍外
- 其他玩家或實體干擾
- 物品被立即拾取或銷毀

**解決方案**：
- 確保在死亡地點附近死亡
- 避免在複雜的地形中死亡
- 檢查是否有其他玩家在場

#### 背包界面無法打開
**可能原因**：
- 背包物品損壞
- 模組版本不匹配

**解決方案**：
- 重新獲取背包物品
- 確保使用最新版本的模組

---

## 🔧 技術實現詳解

### 📁 項目結構

```
DeadRecall/
├── src/main/java/com/adaptor/deadrecall/
│   ├── Deadrecall.java                 # 主模組類
│   ├── item/
│   │   ├── ModItems.java              # 物品註冊
│   │   ├── TieredBackpackItem.java    # 等級背包實現
│   │   └── BackpackItem.java          # 舊版背包（已棄用）
│   ├── inventory/
│   │   └── BackpackInventory.java     # 背包物品欄邏輯
│   └── screen/
│       └── BackpackScreenHandler.java # 界面處理邏輯
├── src/main/resources/
│   ├── assets/deadrecall/
│   │   ├── lang/                      # 多語言支援
│   │   └── models/item/               # 物品模型
│   └── data/deadrecall/
│       └── recipe/                    # 合成配方
└── build.gradle                       # 構建配置
```

### 🛠️ 核心技術

#### 網路同步解決方案
```java
// 使用 ExtendedScreenHandlerType 傳遞等級信息
public static final ScreenHandlerType<BackpackScreenHandler> SCREEN_HANDLER_TYPE =
    Registry.register(Registries.SCREEN_HANDLER, Identifier.of("deadrecall", "backpack"),
        new ExtendedScreenHandlerType<>(
            (syncId, playerInventory, data) -> {
                int tierOrdinal = (Integer) data;
                TieredBackpackItem.BackpackTier tier = TieredBackpackItem.BackpackTier.values()[tierOrdinal];
                return new BackpackScreenHandler(syncId, playerInventory, tier);
            },
            PacketCodecs.INTEGER
        ));
```

#### 動態界面生成
```java
// 根據等級動態生成槽位
int rows = tier.getRows();
for (int row = 0; row < rows; row++) {
    for (int col = 0; col < 9; col++) {
        this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
    }
}
```

#### 物品保存機制
```java
// 使用 Data Components API 保存物品
backpackStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(items));
```

---

## 📈 開發歷史

### v1.0.0 - 初始版本
- ✅ 基本背包功能
- ✅ 物品保存機制

### v1.1.0 - 背包存儲修復
- ✅ 修復物品無法保存的問題
- ✅ 改用 Player+Hand 引用系統

### v1.2.0 - 背包界面優化
- ✅ 添加動態界面大小
- ✅ 改善用戶體驗

### v1.3.0 - 背包界面修復
- ✅ 修復界面顯示問題
- ✅ 優化槽位佈局

### v1.4.1 - 存儲問題最終修復
- ✅ 解決物品丟失問題
- ✅ 完善 Data Components 實現

### v1.5.0 - 背包等級系統
- ✅ 實現多等級背包
- ✅ 添加升級機制
- ✅ 4種不同容量背包

### v1.6.0 - 網路同步與創造模式優化
- ✅ 修復 IndexOutOfBoundsException
- ✅ 實現 ExtendedScreenHandlerType 同步
- ✅ 優化創造模式物品欄註冊

---

## 🐛 故障排除

### 網路協定錯誤
**問題**：`IndexOutOfBoundsException: Index 54 out of bounds for length 54`

**解決方案**：
- 確保使用最新版本 (v1.6.0)
- 該錯誤已在網路同步修復中解決

### 背包物品丟失
**問題**：放入背包的物品消失

**解決方案**：
- 確保使用 v1.4.1+ 版本
- 物品保存機制已完善

### 界面顯示異常
**問題**：背包界面大小不正確

**解決方案**：
- 檢查模組版本
- 確保客戶端和服務器使用相同版本

### 創造模式找不到背包
**問題**：創造模式物品欄中沒有背包

**解決方案**：
- 檢查工具標籤或功能性標籤
- 或使用搜索功能輸入 "backpack"

---

## 🧪 測試指南

### 單元測試
```bash
# 編譯測試
.\gradlew compileJava

# 完整構建測試
.\gradlew build
```

### 遊戲內測試

#### 功能測試
1. **基礎背包製作**：工作台合成
2. **背包升級**：鐵砧台升級測試
3. **物品保存**：放入物品後關閉重開
4. **網路同步**：多人遊戲測試

#### 性能測試
1. **大容量背包**：測試36格背包性能
2. **頻繁操作**：快速開關背包測試
3. **物品移動**：大量物品拖拽測試

---

## 📚 相關文檔

### 開發文檔
- `背包系統實現說明.md` - 技術實現詳解
- `背包系統完成報告.md` - 開發過程記錄
- `背包存储问题修复报告.md` - 修復歷史

### 使用文檔
- `背包系統快速開始.md` - 快速入門指南
- `開始使用-背包等級.md` - 等級系統說明
- `背包等級-快速參考.md` - 參考手冊

### 修復記錄
- `背包網路協定錯誤修復完成.md` - 網路同步修復
- `創造模式背包訪問優化完成.md` - 創造模式優化
- `背包鐵砧升級系統完成.md` - 升級系統實現

---

## 🤝 貢獻指南

### 代碼貢獻
1. Fork 此倉庫
2. 創建功能分支
3. 提交更改
4. 發起 Pull Request

### 問題回報
- 使用 GitHub Issues 回報問題
- 提供詳細的錯誤信息和重現步驟
- 包含模組版本和 Minecraft 版本

### 功能請求
- 在 Issues 中描述新功能需求
- 說明功能的目的和使用場景

---

## 📄 許可證

此項目採用 MIT 許可證。詳見 LICENSE.txt 文件。

---

## 🙏 致謝

感謝所有測試者和回報問題的玩家，你們的反饋讓這個模組更加完善！

---

## 📞 聯繫方式

- **GitHub**：https://github.com/your-repo/deadrecall
- **Issues**：https://github.com/your-repo/deadrecall/issues
- **Wiki**：https://github.com/your-repo/deadrecall/wiki

---

**DeadRecall 模組文檔 - 完整整合版**

*最後更新：2026年2月15日*  
*版本：v1.6.0*  
*狀態：✅ 完整*
