# Totem Platform 開發狀態與 Roadmap

## 已完成或已有可運作基礎

### Totem Remnant / DeadRecall

- 死亡背包核心流程。
- 死亡物品收集與回收。
- 死亡背包 pre-drop 直接擷取第一階段：在原版 `Inventory.dropAll()` 前封裝 Inventory／Equipment，保留 Components、排除背包巢狀、失敗 rollback 與 legacy fallback；Server GameTest 已通過。
- 自訂物品及 Data Component 遷移基礎。
- 多人伺服器運作基礎。
- Minecraft 26.2 / Fabric 遷移工作。

### Totem Automata / Copper Golem

- 銅傀儡綁定與 GUI 基礎。
- 箱子分類模式。
- 物品搬運與庫存狀態。
- 採集模式的規格與部分實作基礎。
- LLM 設定與封包雛形。
- 客戶端範圍及路徑視覺化基礎。

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
- 死亡背包清空回收後停用對應死亡節點並從 Space Unit 地圖隱藏。
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
- 混凝土粉末自動回歸：水源、非水源流動水、未接觸水、雨天、64 格數量、自訂名稱、同一 ItemEntity、age、位置、速度與 pickup delay；5 個 required GameTests 全部通過。
- 最新 `master` Dedicated Server 煙霧測試成功：Fabric／Mixin 初始化、1,594 個 recipe、1,699 個 advancement、三維度建立、保存與正常停止均完成。

## 進行中

- 銅傀儡模式切換與欄位清空驗證。
- 銅傀儡管理 GUI 容器化：右半邊原版玩家背包、燃料／採集工具／採集倉庫真實 slot、拖曳與 Shift 點擊。
- 資源採集、Home 銅箱及 LLM 規則整合。
- 死亡背包 pre-drop 擷取相容性收尾：待故障注入 rollback、`keepInventory`／消失詛咒、雙人同 tick、游標／合成格／第三方欄位驗證，通過後移除舊半徑掃描器。
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

1. 完成死亡背包 pre-drop 擷取的 rollback、原版規則與多人競態驗證，之後刪除舊 ItemEntity 半徑掃描器。
2. 完成好友玩家直接傳送的兩人多人回歸與最新位置／安全落點驗證。
3. 完成講台配方的遊戲內行為驗證。
4. 完成混凝土粉末的真人多人水流與大量 ItemEntity 壓力測試；水源、流動水、雨天、Components 與實體狀態已由 GameTest 驗證。
5. 驗證紫水晶催化折抵，並擴充 Payload／UI 顯示原始成本、催化數量與折抵。
6. 傳送介面 Phase A：共用介面類型、Server context、四物品開啟 UI 與普通羅盤專屬能力分流。
7. 傳送介面 Phase B：回生羅盤死亡節點偏差特化、書本固定磁石路線特化與第一階段 UI。
8. 傳送介面 Phase C：已繪製地圖覆蓋範圍、食物成本／偏差特化、動態玩家目標與隱私驗證。
9. 傳送介面 Phase D：base／final 報價明細、完整 Payload、Dedicated Server 與多人回歸。

## 尚未完成

### Totem Core

- 獨立 repository／module。
- 穩定公開 API。
- 共用 Payload registry。
- SavedData migration framework。
- 共用 Config、GUI 與 permission API。
- 第三方 addon 範例與文件。

### Totem Remnant

- 死亡背包 pre-drop 擷取：刪除 legacy nearby-drop collector、雙重 server task、UUID 差集與相關狀態 Map／Set。
- 死亡背包 fault injection、孤立死亡節點清理、原版規則與多人競態完整回歸。
- 游標 ItemStack、玩家 2×2 crafting slots、外部容器與第三方飾品槽的死亡整合。
- 離線玩家身體 Entity、SavedData、playerdata body lock 與 data migration。
- 登出建立身體、重連接回身體、身體死亡及一次性死亡流程。
- 與死亡背包、死亡紀錄、Nexus 死亡節點及 Discord Bridge 死亡事件整合。
- Server restart、server shutdown、crash recovery 與管理員修復指令。
- 多玩家、PVP、區塊卸載、fake player、Creative／Spectator 與 Dedicated Server 測試。

### Totem Automata

- 銅傀儡管理 GUI 從一般 `Screen` 改為 `Menu` / `Slot` / `AbstractContainerScreen`。
- 右半邊使用玩家原版背包與快捷欄；左半邊保留銅傀儡設定、模式、來源、目的箱、採集與 LLM 控制。
- 燃料 slot、採集工具 slot 與採集倉庫 slot 改為伺服器權威真實 slot，支援拖曳、Shift 點擊、關閉歸還與交易回滾。
- 舊的「按鈕從主手／背包自動搜尋物品」流程已移除，物品移動統一走容器交易。

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

1. 先穩定目前 DeadRecall 2.x 的死亡背包及銅傀儡資料安全。
2. 抽出 Totem Core 最小共用層，但暫不大規模更改 mod ID。
3. 完成 Automata 的無 LLM 核心模式。
4. 實作 Nexus SavedData、磁石互動及探索權限。
5. 完成同維度基礎傳送，再加入穩定度及偏差。
6. 加入跨維度紫水晶、結構磨損及地圖 GUI。
7. 實作好友、人體磁石、分散重生及 Remnant 整合。
8. 移植 Excavation。
9. 最後建立 Cognition Agent Framework，作為可選模組。

## 重新命名策略

在模組真正拆分前：

- repository、mod ID 與 package 可暫時維持 DeadRecall。
- UI 與文件可逐步使用 Totem 品牌。
- 不直接更改現有 component、SavedData、item 或 block identifier。

正式拆分時：

- 為每個模組建立獨立 mod ID。
- 提供舊 DeadRecall identifier 到新模組 identifier 的 migration。
- 提供整合版或相容層，讓舊世界能安全升級。
