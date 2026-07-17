# Totem Platform 開發狀態與 Roadmap

## 已完成或已有可運作基礎

### Totem Remnant / DeadRecall

- 死亡背包核心流程。
- 死亡物品收集與回收。
- 死亡背包 pre-drop 直接擷取：在原版 `Inventory.dropAll()` 前封裝 Inventory／Equipment，保留 Components、排除背包巢狀並提供交易 rollback。
- `keepInventory`、消失詛咒、既有世界掉落物、雙玩家同位置同 tick、實體／死亡節點故障注入與原版 fallback GameTest 已通過。
- 岩漿、仙人掌、虛空、爆炸，以及只持有一般／死亡背包的死亡 GameTest 已通過；環境死亡仍使用唯一的直接擷取路徑。
- Active menu 游標與玩家 2×2 crafting inputs 已納入同一個死亡 transaction；外部箱子持久 storage 保持隔離，暫存背包排除、暫存消失詛咒與 transient rollback GameTest 已通過。
- Crafting Table、Anvil、Smithing Table、Grindstone、Stonecutter、Loom、Cartography Table 與 Enchanting Table 的暫存 inputs 已透過 class／slot-range 白名單納入死亡 transaction；result preview 與持久 block/entity inventory 維持排除。
- 第三方 player-owned inventory 已提供公開 transaction SPI：provider／slot registry、commit-time compare-and-clear、反向 rollback、Inventory fallback、provider 例外隔離及可攜式容器排除已完成。
- Trinkets Updated 4.1.x／Minecraft 26.2 optional adapter 已完成；真實 player `DROP` slot、Components、source 清空及 exactly-once 世界結果已由 GameTest 驗證，未安裝 Trinkets 時不形成必要依賴。
- 死亡背包回收改以背包綁定的 node UUID 停用節點；原 owner 離線後可由其他玩家安全回收，通知故障不影響節點停用或空背包移除，回收狀態與 discovery 已通過 SavedData codec round-trip。
- 死亡背包 entity、Space Unit SavedData、discovery、同 UUID replacement player 回收與刪除狀態已通過三次獨立正常 Dedicated Server JVM 的 seed／recover／verify world reload。
- 舊 nearby-drop 掃描器、UUID 差集、雙重 server task、狀態 Map／Set、record 與相容 Mixin 已從程式碼完整刪除；失敗時只回到原版世界掉落。
- 自訂物品及 Data Component 遷移基礎。
- 多人伺服器運作基礎。
- Minecraft 26.2 / Fabric 遷移工作。

### Totem Automata / Copper Golem

- `SORTING`／`GATHERING` 雙模式、伺服器原子切換、欄位清空驗證、revision 與 stale payload 防護已完成。
- 管理介面已使用 `Menu`／`Slot`／`AbstractContainerScreen`；玩家原版背包、燃料、採集工具與採集倉庫皆為伺服器權威真實 slot，支援拖曳、Shift 點擊及即時 live-state 驗證。
- 箱子分類、每次最多 16 個物品、來源 exactly-once rollback、blocked snapshot、未載入綁定保留與箱內 DeadRecall 背包目的地已完成。
- 自主採集的工作區、增量掃描、尋路、可視破壞、掉落入倉、工具耐久、燃料、返回 Home 與存放生命週期已完成。
- Home 滿載／失效、工具損壞、不可達目標及無有效目標的 blocked activity 與資料保留規則已完成。
- LLM 共用 HTTP client、分類與採集 Prompt、pending 原子去重、失敗 cooldown、快取上限及過期非同步回應防護已完成。
- 客戶端工作區、Home、target、目的地連線與 blocked activity 視覺化已完成。
- Entity NBT、真實 chunk unload／reload、三次獨立 Dedicated Server JVM world persistence probe 已通過。
- stale revision、雙玩家同 revision、真實 Menu 點擊、128 隻 Copper Golem scanner 壓力及 controller unload／rediscovery GameTests 已通過。
- `copper-golem-operation-modes` OpenSpec 1–14 已完成，測試與限制記錄於 `docs/developer/testing.md`。

### Totem Nexus / Space Unit

- Space Unit 世界 SavedData 基礎。
- 磁石以羅盤右鍵註冊與綁定。
- 玩家到場後以羅盤左鍵啟用／探索磁石。
- 私有 Owner 權限基礎。
- 權限及探索雙重過濾的 Space Unit 地圖查詢 Payload。
- 相對位置 Space Unit 地圖 GUI 基礎。
- Space Unit 地圖資訊面板的 server-side 傳送報價基礎。
- Space Unit 基礎傳送 session：啟動、倒數、移動／受傷／離線條件取消、成本扣除及安全落點。
- Space Unit 傳送偏差、抵達風險傷害及石碑結構磨損／方塊退化基礎。
- Space Unit 地圖 GUI 搜尋、類型篩選及排序基礎。
- Space Unit 地圖收藏持久化、收藏標記與收藏切換封包基礎。
- Space Unit 石碑結構掃描、磨損判斷及退化候選方塊 tag 化基礎。
- Space Unit 石碑退化替換映射 data pack 化基礎。
- Space Unit 石碑退化規則補上 Nether Bricks 與 Polished Blackstone Bricks。
- Space Unit 石碑退化規則補上 Copper Grates 與 Copper Bulbs。
- 固定磁石方塊被破壞或替換後停用對應 Space Unit，並避免同座標重放磁石沿用舊 Owner／權限。
- Space Unit 未註冊磁石的石碑預覽與二次右鍵確認註冊基礎。
- Space Unit 地圖資訊面板的磁石校準按鈕與 server-side 重新掃描基礎。
- Space Unit 地圖資訊面板的固定磁石公開／私人可見性切換基礎。
- Space Unit 地圖資訊面板顯示可見性、結構階級與可管理狀態。
- Space Unit 地圖資訊面板的傳送成本與風險摘要圖示化基礎。
- Space Unit 地圖資訊面板的固定磁石重新命名基礎。
- Space Unit 好友 SavedData、羅盤右鍵玩家邀請／接受及好友可見性過濾基礎。
- Space Unit 地圖 GUI 好友篩選與資訊面板進階細節基礎：距離、成本、風險圖示及好友共享標示。
- DeadRecall 死亡背包成功建立後建立死亡節點基礎。
- 死亡背包清空回收後依 binding 停用正確死亡節點並從 Space Unit 地圖隱藏；非 owner 回收、通知故障隔離與跨程序重載已驗證。
- Space Unit 分散重生 Gamerule、持久個人重生點與密度加權有限採樣基礎。
- Space Unit 線上好友 `PLAYER` 目的地、粗略位置地圖顯示，以及雙向好友直接進入已授權傳送 session 的基礎。
- Space Unit 地圖資訊面板的線上玩家 Administrator／允許名單調整基礎。
- Space Unit 未註冊磁石的石碑預覽 GUI 與 Server-side pending 確認封包基礎。
- Space Unit 好友管理 GUI 名單瀏覽、邀請狀態顯示與移除／取消關係基礎。
- Space Unit 好友直接傳送已移除舊逐次 consent，補上成功啟動通知、解除好友立即取消，以及離線／不可用／關係解除的精確取消原因。
- Space Unit 石碑紫水晶催化核心：快照 optional 欄位、資料驅動 tag、既有掃描計數、每 4 方塊折抵 1 碎片、最終成本最低 1，以及 Java 25 公式測試已完成。

### 共用基礎

- 自訂 Payload 與 Client/Server 同步。
- 自訂 GUI。
- 世界及設定資料存取。
- ItemStack 自訂資料處理。
- OpenSpec 銅傀儡文件。
- Discord Bridge OpenSpec、玩家／管理／公開事件轉播、健康告警及伺服器狀態 Bot Token 頻道路由修正。
- 講台替代配方資源：4 個任意木半磚＋1 本書；Java 25 build 與 Dedicated Server recipe 載入已驗證。
- 混凝土粉末掉落物水中硬化核心：16 色映射、Server-side 同一 ItemEntity 轉換、無世界全量掃描、Java 25 build 與 Dedicated Server 啟動已完成。
- Fabric Loom Server GameTest 基礎：獨立 `gametest` source set、測試模組 entrypoint、`runGameTest` 自動接入 `build`，並保留失敗報告 artifact。
- 正常 Dedicated Server restart probe 基礎：死亡背包 `runRestartProbe` 與銅傀儡 `runCopperRestartProbe` 使用獨立 world、跨 JVM phase marker、entity region／SavedData reload 與失敗 artifact。
- 混凝土粉末自動回歸：水源、非水源流動水、未接觸水、雨天、64 格數量、自訂名稱、同一 ItemEntity、age、位置、速度與 pickup delay；5 個 required GameTests 全部通過。
- 最新 `master` Dedicated Server 煙霧測試成功：Fabric／Mixin 初始化、1,594 個 recipe、1,699 個 advancement、三維度建立、保存與正常停止均完成。

## 進行中

- 離線玩家身體 OpenSpec 與實作：登出保留身體、重連接回、死亡處理與防複製。
- OpenSpec 統一與平台架構整理。
- DeadRecall 向 Totem 模組化架構過渡。
- Nexus 進階地圖功能、石碑管理與好友權限模型。
- 好友玩家直接傳送：核心流程、死碼清理、通知、主動取消與精確原因已完成；待兩名真實玩家回歸及完成時最新位置／安全落點驗證。
- 講台配方覆寫：Dedicated Server 載入已通過；待不同木種製作、圖書管理員、書本與紅石行為驗證。
- 混凝土粉末掉落物硬化：Server GameTest 自動驗證已完成；只剩兩名以上真人玩家水流測試與大量 ItemEntity 壓力測試。
- 紫水晶催化折抵：Dedicated Server 啟動與 Mixin 初始化已通過；待舊世界載入、拆除後重新報價、實際跨維度扣款，以及 Payload／UI 明細。

## 待排程

- 傳送介面物品特化：普通羅盤、回生羅盤、書本與已繪製地圖皆可開啟傳送介面。
- 普通羅盤維持通用基準與磁石／好友管理能力。
- 回生羅盤將自己的死亡節點傳送偏差降低 50%。
- 書本作為路線典籍，對已探索固定磁石目標降低 20% 準備時間與 25% 石碑磨損率。
- 已繪製地圖在目標位於地圖 Dimension 與覆蓋範圍內時，降低 20% 食物等價成本與偏差；不降低紫水晶成本。
- 此項目已完成 proposal、design、tasks 與 delta spec，排在目前功能驗證後開始實作。

### 短週期完成順序

1. 完成好友玩家直接傳送的兩人多人回歸與最新位置／安全落點驗證。
2. 完成講台配方的遊戲內行為驗證。
3. 完成混凝土粉末的真人多人水流與大量 ItemEntity 壓力測試；水源、流動水、雨天、Components 與實體狀態已由 GameTest 驗證。
4. 驗證紫水晶催化折抵，並擴充 Payload／UI 顯示原始成本、催化數量與折抵。
5. 傳送介面 Phase A：共用介面類型、Server context、四物品開啟 UI 與普通羅盤專屬能力分流。
6. 傳送介面 Phase B：回生羅盤死亡節點偏差特化、書本固定磁石路線特化與第一階段 UI。
7. 傳送介面 Phase C：已繪製地圖覆蓋範圍、食物成本／偏差特化、動態玩家目標與隱私驗證。
8. 傳送介面 Phase D：base／final 報價明細、完整 Payload、Dedicated Server 與多人回歸。

## 尚未完成

### Totem Core

- 獨立 repository／module。
- 穩定公開 API。
- 共用 Payload registry。
- SavedData migration framework。
- 共用 Config、GUI 與 permission API。
- 第三方 addon 範例與文件。

### Totem Remnant

- 離線玩家身體 Entity、SavedData、playerdata body lock 與 data migration。
- 登出建立身體、重連接回身體、身體死亡及一次性死亡流程。
- 與死亡背包、死亡紀錄、Nexus 死亡節點及 Discord Bridge 死亡事件整合。
- Server restart、server shutdown、crash recovery 與管理員修復指令。
- 多玩家、PVP、區塊卸載、fake player、Creative／Spectator 與 Dedicated Server 測試。
- 尚無 Minecraft 26.2 直接版本的特定 Accessories adapter；其他 addon 已可使用公開死亡 inventory SPI。

### Totem Nexus

- 磁石完整管理介面整合與 UX 打磨。
- 石碑完整管理介面的離線玩家查詢、名單瀏覽與批次調整。
- 好友／人體磁石進階設定（通知、黑名單與完整互動流程）。
- 紫水晶催化折抵的 base cost、catalyst count、discount Payload 欄位與地圖明細 UI。
- 死亡節點傳送成本細節與回收後歷史紀錄介面。
- 傳送介面物品特化：四物品入口、Server context、回生羅盤、路線典籍、地圖覆蓋與報價明細。

### Totem Excavation

- Blossom 錘子移植至 26.2。
- ItemStack Data Component 選區。
- 多玩家狀態隔離。
- 分 tick 採掘 session。
- Tag 驅動採掘規則。
- Client-side 選區框線。

### Totem Cognition

- Agent API。
- Provider abstraction。
- OpenAI、Gemini、Claude、Ollama 等 Provider。
- Tool calling、memory、planner 與 permission sandbox。
- Automata 可選整合。
- 未安裝 Cognition 時的完整 fallback。

## 建議開發順序

1. 收尾 Nexus 好友直接傳送、講台配方、混凝土粉末壓力及紫水晶催化等短週期驗證。
2. 實作傳送介面物品特化 Phase A–D，完成目前 Nexus 使用者介面主線。
3. 實作 Remnant 離線玩家身體及其死亡背包、死亡節點與 Discord 整合。
4. 抽出 Totem Core 最小共用層，再逐步建立穩定公開 API 與 migration framework。
5. 完成 DeadRecall 向 Totem 模組化架構過渡及 addon 文件。
6. 移植 Excavation。
7. 最後建立 Cognition Agent Framework，作為可選模組。

## 重新命名策略

在模組真正拆分前：

- repository、mod ID 與 package 可暫時維持 DeadRecall。
- UI 與文件可逐步使用 Totem 品牌。
- 不直接更改現有 component、SavedData、item 或 block identifier。

正式拆分時：

- 為每個模組建立獨立 mod ID。
- 提供舊 DeadRecall identifier 到新模組 identifier 的 migration。
- 提供整合版或相容層，讓舊世界能安全升級。
