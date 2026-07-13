# 專案結構

## Java 程式入口

| 路徑 | 責任 |
| --- | --- |
| `Deadrecall.java` | 模組入口、註冊、事件與指令 |
| `DiscordBridge.java` | Discord Bridge HTTP 傳輸 |
| `DeathLocationManager.java` | 玩家死亡維度與座標管理 |
| `alchemy/` | 投料、煉藥鍋與豬糞流程 |
| `block/` | 自訂方塊、煉藥鍋與 Block Entity |
| `entity/ai/` | 豬吃草與產生豬糞方塊的 AI |
| `item/` | 物品註冊、背包與創造模式頁籤 |
| `item/copper/` | 銅板手與銅魁儡互動 |
| `inventory/` | 背包容器資料讀寫 |
| `mixin/` | 原版行為攔截與擴充 |

## 資源入口

- 一般配方：`src/main/resources/data/deadrecall/recipe/`
- 煉藥鍋配方：`src/main/resources/data/deadrecall/deadrecall/cauldron_recipes/`
- 語言檔：`src/main/resources/assets/deadrecall/lang/`
- Fabric metadata：`src/main/resources/fabric.mod.json`
- Discord Worker：`deploy/` 或專案中的 Worker 範例檔

新增系統時應優先依責任建立 package，避免把所有事件、網路封包和資料處理繼續集中到模組主入口。