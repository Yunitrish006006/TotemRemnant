# 🎮 DeadRecall 模組完整文檔

## 📖 項目概述

**DeadRecall** 是一個 Minecraft Fabric 模組，提供多等級背包系統、死亡物品保護、Discord 聊天橋接及煉金配方等功能。

### 📊 技術資訊
- **Minecraft 版本**：26.2
- **模組載入器**：Fabric（需 Fabric API）
- **當前版本**：v1.7.2
- **授權**：BSD-3-Clause

---

## 🧑‍🎮 生存會體驗到的功能

### 💬 Discord 聊天橋接

玩家在遊戲中發送的聊天訊息，會**自動轉發到 Discord 頻道**，讓不在線的玩家也能掌握伺服器動態。

#### 運作方式
- 遊戲內聊天 → Cloudflare Worker API → Discord Webhook → Discord 頻道
- 傳送格式：`**玩家名稱**: 訊息內容`
- 使用非同步傳送，不影響遊戲效能

#### 設定方式（伺服器管理員）
1. 在伺服器 `config/` 目錄找到 `discord-bridge.json`（首次啟動自動建立）
2. 編輯設定檔，填入下列欄位：
```json
{
  "enabled": true,
  "workerUrl": "https://your-worker.workers.dev",
  "apiKey": "your-api-key"
}
```
| 欄位 | 要填寫什麼 | 範例 |
|------|------------|------|
| `enabled` | 是否啟用 Discord 聊天橋接 | `true` |
| `workerUrl` | 你的 Cloudflare Worker 網址 | `https://your-worker.workers.dev` |
| `apiKey` | 與 Worker 端一致的 API 金鑰 | `your-api-key` |
3. 重啟伺服器後生效

#### Cloudflare Worker 設定（管理員）
將 `cloudflare-worker-example.js` 部署到 Cloudflare Worker，並設定以下環境變數：
| 環境變數 | 要填寫什麼 | 範例 |
|----------|------------|------|
| `MC_API_KEY` | 與 `discord-bridge.json` 中 `apiKey` 相同的金鑰 | `your-api-key` |
| `DISCORD_WEBHOOK_URLS` | Discord Webhook URL 的 JSON 陣列 | `["https://discord.com/api/webhooks/xxx/yyy"]` |

`DISCORD_WEBHOOK_URLS` 範例：
  ```
  ["https://discord.com/api/webhooks/xxx/yyy"]
  ```

> **注意**：若未啟用或設定不完整，此功能會自動停用，不影響其他功能正常運作。

---

### 💀 死亡背包系統

玩家死亡後，物品**不會散落一地**，而是自動收集進一個特殊的「死亡背包」，並生成在死亡地點。

#### 生存體驗流程
1. **死亡**：玩家因任何原因死亡
2. **自動收集**：模組掃描死亡地點附近 **10 格範圍**內的所有掉落物
3. **生成背包**：死亡背包（物品實體）出現在死亡座標
4. **收到通知**：聊天欄顯示 `§e你的物品已被收集到死亡背包中！`
5. **取回物品**：返回死亡地點，等待 **2 秒拾取延遲**後拾起背包，右鍵打開取回物品

#### 死亡背包特性
| 屬性 | 說明 |
|------|------|
| 容量 | 動態（依物品數量，最多 6 排 54 格） |
| 搜尋範圍 | 10 格立方體 |
| 拾取延遲 | 2 秒 |
| 防火 | ✅ 背包本身防火，物品不會被燒毀 |
| 存活時間 | 永久（不會自然消失） |
| Tooltip | 顯示物品數量與排數 |

> ⚠️ 若死亡時物品欄為空，或附近沒有掉落物，則不會生成死亡背包。

---

### 🎒 背包系統

模組提供 **4 個等級**的可升級背包，讓玩家在生存中逐步擴充攜帶空間。

#### 背包等級總覽

| 等級 | 物品 ID | 容量 | 防火 | 取得方式 |
|------|---------|------|------|---------|
| 🟤 **基礎背包** | `backpack_basic` | 9格（1排） | ❌ | 工作台合成 |
| 🟦 **標準背包** | `backpack_standard` | 18格（2排） | ❌ | 鍛造台升級 |
| ⚫ **進階背包** | `backpack_advanced` | 27格（3排） | ❌ | 鍛造台升級 |
| 🟣 **獄髓背包** | `backpack_netherite` | 36格（4排） | ✅ | 鍛造台升級 |

#### 合成與升級配方

**基礎背包**（工作台）：
```
[皮革] [皮革] [皮革]
[皮革] [箱子] [皮革]
[皮革] [皮革] [皮革]
```

**標準背包**（鍛造台）：
- 模板：箱子
- 基底：基礎背包
- 附加材料：鐵錠

**進階背包**（鍛造台）：
- 模板：箱子
- 基底：標準背包
- 附加材料：鑽石

**獄髓背包**（鍛造台）：
- 模板：獄髓升級模板
- 基底：進階背包
- 附加材料：獄髓錠

#### 背包特性
- ✅ 右鍵使用開啟背包界面
- ✅ 物品保存（關閉後不丟失）
- ✅ Tooltip 顯示等級與格數
- ✅ 每次只能堆疊 1 個（不可疊放）
- ✅ Shift+點擊快速移動物品

#### 整理快捷鍵
- 預設按鍵：中鍵
- 會整理你目前開著的物品介面內容，將相同物品盡量堆疊並依物品排序
- 可到 **設定 → 按鍵綁定 → DeadRecall** 重新指定快捷鍵

---

### ⚗️ 煉金配方（新增資源）

模組加入數種新素材與配方，提供另一種製作火藥的途徑。

#### 新增物品
| 物品 | 物品 ID | 說明 |
|------|---------|------|
| 硫磺 | `sulfur` | Minecraft 原版資源 |
| 硝石 | `saltpeter` | 煉金合成材料 |
| 缽 | `stone_bowl` | 對硫磺方塊使用後可裝填硫磺 |
| 帶硫磺的缽 | `sulfur_bowl` | 作為煉金火藥材料，合成後回傳缽 |

#### 配方

**硝石**（工作台，產出 2 個）：
```
[礫石] [煤炭/木炭] [礫石]
[  空  ] [  骨粉  ] [  空  ]
[礫石] [  礫石  ] [礫石]
```

**裝填硫磺**（對方塊互動）：
```
手持缽，右鍵點擊硫磺方塊
=> 變為「帶硫磺的缽」，硫磺方塊會被消耗
```

**缽**（工作台）：
```
[石] [空] [石]
[空] [石] [空]
[空] [燧石] [空]
```

**火藥 × 4（煉金法 1）**（工作台，需帶硫磺的缽）：
```
帶硫磺的缽 + 硝石 + 木炭/煤（無序合成）
```

**火藥 × 4（煉金法 2）**（工作台，需原硫磺）：
```
[硫磺] [木炭] [硫磺]
[  空  ] [硝石] [  空  ]
[硫磺] [木炭] [硫磺]
```

---

### 📜 指令

| 指令 | 權限 | 說明 |
|------|------|------|
| `/back` | 一般玩家 | 傳送回上次死亡地點（使用一次後失效） |
| `/give @s deadrecall:backpack_basic` | OP | 給予基礎背包 |
| `/give @s deadrecall:backpack_standard` | OP | 給予標準背包 |
| `/give @s deadrecall:backpack_advanced` | OP | 給予進階背包 |
| `/give @s deadrecall:backpack_netherite` | OP | 給予獄髓背包 |
| `/give @s deadrecall:death_backpack` | OP | 給予死亡背包（測試用） |
| `/give @s minecraft:sulfur` | OP | 給予硫磺（原版） |
| `/give @s deadrecall:saltpeter` | OP | 給予硝石 |
| `/give @s deadrecall:stone_bowl` | OP | 給予缽 |
| `/give @s deadrecall:sulfur_bowl` | OP | 給予帶硫磺的缽 |

#### `/back` 指令說明
- 玩家死亡後，死亡座標會被**自動記錄**
- 執行 `/back` 後立即傳送至死亡地點
- **傳送後座標清除**，同一次死亡只能使用一次
- 若無死亡座標，顯示 `§c沒有死亡座標可傳送！`

---

## 📈 更新日誌

### v1.7.2（當前版本）
- ✅ 新增背包整理快捷鍵（預設中鍵）
- ✅ 新增 Discord 聊天橋接功能（Cloudflare Worker 架構）
- ✅ 新增 `/back` 死亡座標傳送指令
- ✅ 新增硝石、缽、帶硫磺的缽物品（硫磺改用原版）
- ✅ 新增缽右鍵硫磺方塊裝填機制（會消耗方塊）
- ✅ 新增煉金火藥配方
- ✅ 死亡背包改為動態容量（依物品數最多 54 格）
- ✅ 死亡背包永久不消失（`setUnlimitedLifetime`）
- ✅ 修復 IndexOutOfBoundsException 網路同步問題

### v1.5.0
- ✅ 實現多等級背包（基礎／標準／進階／獄髓）
- ✅ 添加鍛造台升級機制

### v1.4.1
- ✅ 解決物品放入背包後丟失的問題
- ✅ 完善 Data Components API 儲存實作

### v1.3.0
- ✅ 修復背包界面顯示問題
- ✅ 優化槽位佈局

### v1.2.0
- ✅ 動態界面大小（依等級調整排數）

### v1.1.0
- ✅ 修復物品無法保存的問題
- ✅ 改用 Player+Hand 引用系統

### v1.0.0
- ✅ 基本背包功能
- ✅ 物品保存機制

---

## 🛠️ 開發提醒

### 項目結構
```
DeadRecall/
├── src/main/java/com/adaptor/deadrecall/
│   ├── Deadrecall.java                 # 主入口：物品註冊、事件監聽、指令
│   ├── DiscordBridge.java              # Discord 橋接（Cloudflare Worker HTTP）
│   ├── DeathLocationManager.java       # 死亡座標管理（UUID → BlockPos）
│   ├── item/
│   │   ├── ModItems.java              # 所有物品的靜態常量與註冊
│   │   ├── TieredBackpackItem.java    # 等級背包（含 BackpackTier enum）
│   │   └── DeathBackpackItem.java     # 死亡背包（動態容量）
│   └── inventory/
│       └── BackpackInventory.java     # 背包物品欄邏輯（Data Components 儲存）
├── src/main/resources/data/deadrecall/recipe/
│   ├── backpack.json                  # 基礎背包工作台配方
│   ├── backpack_standard_smithing.json
│   ├── backpack_advanced_smithing.json
│   ├── backpack_netherite_smithing.json
│   ├── saltpeter.json
│   ├── gunpowder_from_sulfur.json
│   ├── gunpowder_from_alchemy.json
│   └── stone_bowl.json
├── src/main/resources/assets/deadrecall/lang/
│   ├── zh_tw.json
│   ├── zh_cn.json
│   └── en_us.json
├── cloudflare-worker-example.js        # Discord Worker 範例程式碼
├── discord-bridge.json                 # （執行時於 config/ 自動產生）
└── fabric.mod.json
```

### 關鍵技術注意事項

#### Discord Bridge
- 使用單執行緒 `ExecutorService` 非同步傳送，避免阻塞主線程
- 設定檔路徑：`<server>/config/discord-bridge.json`
- API 端點：`POST {workerUrl}/api/mc/chat`，Header：`X-API-Key`
- Worker 端點另有 `/api/mc/server/status` 可供未來擴充伺服器狀態回報

#### 死亡背包
- 使用 `ServerLivingEntityEvents.AFTER_DEATH` 監聽，雙層 `execute()` 確保在物品掉落後才收集
- 收集範圍：`AABB.inflate(10.0)`，收集所有實體（非僅特定玩家的）
- 使用 `DataComponents.CONTAINER` + `ItemContainerContents` 儲存物品
- `setUnlimitedLifetime()` 防止背包自然消失
- `setPickUpDelay(40)` = 2 秒（20 ticks/秒）

#### `/back` 指令
- `DeathLocationManager` 以 `HashMap<UUID, DeathLocation>` 暫存死亡座標
- 使用後呼叫 `clearDeathLocation()` 清除，避免重複傳送
- 注意：目前不處理跨維度死亡傳送（`worldRegistryKey` 已儲存但未使用）

#### 背包儲存
- 使用原版 `DataComponents.CONTAINER`（`ItemContainerContents`）儲存物品，相容性高
- `BackpackInventory` 在開啟時從 DataComponent 讀取，關閉時寫回
- 死亡背包的容量為動態計算：`Math.max(1, Math.min(6, ceil(itemCount / 9.0)))` 排

### 待辦 / 已知問題
- ⚠️ `/back` 指令未處理跨維度傳送（如從地獄傳回主世界）
- ⚠️ 死亡背包收集範圍未過濾所有者，多人同時死亡可能互相收集到對方物品
- ⚠️ Discord Bridge `sendServerStatus` 方法已實作但尚未被呼叫（預留擴充）
- ⚠️ 舊版 `deadrecall:backpack` 物品 ID 仍保留但標記為 `@Deprecated`

### 構建指令
```bash
# 編譯
./gradlew compileJava

# 完整構建（產出 .jar）
./gradlew build

# 資料生成
./gradlew runDatagen
```
