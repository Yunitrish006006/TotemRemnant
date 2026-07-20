# Totem Platform 開發狀態與 Roadmap

## 已完成或已有可運作基礎

### Totem Remnant / DeadRecall

- 死亡背包核心流程。
- 死亡物品收集與回收。
- 死亡背包 pre-drop 直接擷取：在 `Player.dropEquipment` 入口、原版／addon 掉落與 `Inventory.dropAll()` 前封裝 Inventory／Equipment，保留 Components、排除背包巢狀並提供交易 rollback。
- `keepInventory`、消失詛咒、既有世界掉落物、雙玩家同位置同 tick、實體／死亡節點故障注入與原版 fallback GameTest 已通過。
- 岩漿、仙人掌、虛空、爆炸，以及只持有一般／死亡背包的死亡 GameTest 已通過；環境死亡仍使用唯一的直接擷取路徑。
- Active menu 游標與玩家 2×2 crafting inputs 已納入同一個死亡 transaction；外部箱子持久 storage 保持隔離，暫存背包排除、暫存消失詛咒與 transient rollback GameTest 已通過。
- Crafting Table、Anvil、Smithing Table、Grindstone、Stonecutter、Loom、Cartography Table 與 Enchanting Table 的暫存 inputs 已透過 class／slot-range 白名單納入死亡 transaction；result preview 與持久 block/entity inventory 維持排除。
- 第三方 player-owned inventory 已提供公開 transaction SPI：provider／slot registry、commit-time compare-and-clear、反向 rollback、Inventory fallback、provider 例外隔離及可攜式容器排除已完成。
- Trinkets Updated 4.1.x／Minecraft 26.2 optional adapter 已完成；真實 player `DROP` slot、Components、source 清空及 exactly-once 世界結果已由 GameTest 驗證，未安裝 Trinkets 時不形成必要依賴。
- 可攜式容器巢狀限制已覆蓋 Backpack／Bundle／Shulker 的手動操作、Hopper、Hopper Minecart chain、Dropper、Dispenser、死亡 rollback、legacy data 與雙玩家同 tick Server fixture。
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
- 好友 PLAYER 傳送多人 GameTests 已完成：雙向好友直接啟動、非好友／單向邀請／自己拒絕、解除好友雙方向立即取消、離線／死亡取消、完成時最新位置與安全落點、跨 Dimension 追蹤及成本 exactly-once。
- Space Unit 石碑紫水晶催化已完成：快照 optional 欄位、資料驅動 tag、既有掃描計數、每 4 方塊折抵 1 碎片、最終成本最低 1、Server Payload／本地化 UI、完成前重新報價、跨維度扣款及舊世界三次 JVM migration probe 均已驗證。
- 傳送介面 Phase A–D 已完成：四種物品入口、Server context／session、普通羅盤能力邊界、回生羅盤自己的死亡節點偏差 -50%、書本固定磁石準備時間 -20%／石碑磨損 -25%，以及已繪製地圖的 Server coverage、食物／偏差 -20%、動態玩家重驗證與位置隱私均已完成；Payload／GUI 顯示食物、時間、偏差及磨損的 base／final 明細，29 個 required GameTests 全數通過。

### 共用基礎

- 自訂 Payload 與 Client/Server 同步。
- 自訂 GUI。
- 世界及設定資料存取。
- ItemStack 自訂資料處理。
- OpenSpec 銅傀儡文件。
- Discord Bridge OpenSpec、玩家／管理／公開事件轉播、健康告警及伺服器狀態 Bot Token 頻道路由修正。
- Discord 固定繁中本地化已涵蓋 advancement、村民、死亡 template、Boss／實體名稱、raid 結果與 difficulty；custom literal、missing-key 節流、exactly-once payload、Worker 503 隔離、Server Data runtime reload 及原子 snapshot 替換已自動驗證。
- 講台替代配方已完成：4 個任意木半磚＋1 本書；RecipeManager 已驗證橡木、竹、緋紅蕈木、扭曲蕈木與混合半磚，講台書本／Menu／Comparator／兩 tick 紅石脈衝及村民圖書管理員 POI 均通過 Server GameTest。
- 混凝土粉末掉落物水中硬化核心：16 色映射、Server-side 同一 ItemEntity 轉換、無世界全量掃描、512 實體壓力回歸、Java 25 build 與 Dedicated Server 啟動已完成。
- Fabric Loom Server GameTest 基礎：獨立 `gametest` source set、完整 34 類測試模組 entrypoint、`runGameTest` 自動接入 `build`，並保留失敗報告 artifact。
- 正常 Dedicated Server restart probe 基礎：死亡背包 `runRestartProbe` 與銅傀儡 `runCopperRestartProbe` 使用獨立 world、跨 JVM phase marker、entity region／SavedData reload 與失敗 artifact。
- 混凝土粉末自動回歸：水源、非水源流動水、未接觸水、雨天、64 格數量、自訂名稱、同一 ItemEntity、age、位置、速度、pickup delay，以及 512 個不可合併 ItemEntity 壓力 fixture；6 個 required GameTests 全部通過。
- 最新 `master` Dedicated Server 煙霧測試成功：Fabric／Mixin 初始化、1,594 個 recipe、1,699 個 advancement、三維度建立、保存與正常停止均完成。
- Modrinth 自動發布流程：`master` 版本號變更完成 build 後上傳正式 JAR，並以 project ID／token 隔離與 SHA-512 冪等檢查防止錯誤覆蓋。

## 進行中

- 離線玩家身體 OpenSpec 與實作：登出保留身體、重連接回、死亡處理與防複製。
- OpenSpec 統一與平台架構整理。
- DeadRecall 向 Totem 模組化架構過渡。
- 安全多 repository 拆分 Phase 0：repository ownership、相容 identifier/resource 基線、CI 護欄與逐模組 rollback protocol 已建立；下一步先在單一 artifact 內拆 bootstrap、Payload、Mixin 與 registry ownership。
- Nexus 進階地圖功能、石碑管理與好友權限模型。
- 混凝土粉末掉落物硬化：Server GameTest 與 512 個 ItemEntity 壓力回歸已完成；只剩兩名以上真人玩家水流驗收。
- 傳送介面物品特化：Phase A–D 自動化排程完成；只剩兩名以上真人 Client 的 UI、動態目標與多人驗收。

## 待排程

- 傳送介面 Phase A–D 已完成；真人 Client 驗收在可取得兩名以上玩家時執行。

### 短週期完成順序

1. 完成混凝土粉末的真人多人水流驗收；此人工項目與後續開發平行待辦，512 個 ItemEntity 壓力測試、水源、流動水、雨天、Components 與實體狀態已由 GameTest 驗證。
2. 傳送介面 Phase A：已完成共用介面類型、Server context、四物品開啟 UI 與普通羅盤專屬能力分流。
3. 傳送介面 Phase B：已完成回生羅盤死亡節點偏差特化、書本固定磁石路線特化與第一階段 UI。
4. 傳送介面 Phase C：已完成已繪製地圖覆蓋範圍、食物成本／偏差特化、動態玩家目標與隱私驗證。
5. 傳送介面 Phase D：已完成 base／final 報價明細、完整 Payload／GUI、Dedicated Server 與自動化多人回歸；真人多人 UI 驗收保持待辦。

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
- 死亡節點傳送成本細節與回收後歷史紀錄介面。
- 傳送介面物品特化：Phase A–D 四物品入口、Server context、回生羅盤、路線典籍、地圖覆蓋及 base／final 報價明細已完成；只剩真人多人 UI／動態目標驗收。

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

1. 在混凝土粉末真人多人驗收平行待辦期間，實作傳送介面物品特化 Phase A–D，完成目前 Nexus 使用者介面主線。
2. 完成真人多人可用時才能執行的混凝土粉末水流驗收。
3. 實作 Remnant 離線玩家身體及其死亡背包、死亡節點與 Discord 整合。
4. 先在 DeadRecall 內切分 bootstrap、Payload、Mixin、registry 與 resource ownership，再抽出 Totem Core 最小共用層及穩定公開 API／migration framework。
5. 依 Discord Bridge pilot、Remnant、Automata、Nexus 的順序建立獨立 repository；DeadRecall 保留為精確版本 compatibility bundle 與跨 repository E2E 驗收。
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
