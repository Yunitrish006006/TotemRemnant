# Totem Platform 開發狀態與 Roadmap

## 已完成或已有可運作基礎

### Totem Remnant / DeadRecall

- 死亡背包核心流程。
- 死亡物品收集與回收。
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
- Space Unit 線上好友 `PLAYER` 目的地、粗略位置地圖顯示與每次傳送確認基礎。
- Space Unit 地圖資訊面板的線上玩家 Administrator／允許名單調整基礎。
- Space Unit 未註冊磁石的石碑預覽 GUI 與 Server-side pending 確認封包基礎。
- Space Unit 好友管理 GUI 名單瀏覽、邀請狀態顯示與移除／取消關係基礎。

### 共用基礎

- 自訂 Payload 與 Client/Server 同步。
- 自訂 GUI。
- 世界及設定資料存取。
- ItemStack 自訂資料處理。
- OpenSpec 銅傀儡文件。
- Discord Bridge OpenSpec、玩家／管理／公開事件轉播、健康告警及伺服器狀態 Bot Token 頻道路由修正。

## 進行中

- 銅傀儡模式切換與欄位清空驗證。
- 資源採集、Home 銅箱及 LLM 規則整合。
- 死亡背包資料安全、複製及物品回注問題修正。
- OpenSpec 統一與平台架構整理。
- DeadRecall 向 Totem 模組化架構過渡。
- Nexus 進階地圖功能、石碑管理與好友權限模型。

## 尚未完成

### Totem Core

- 獨立 repository／module。
- 穩定公開 API。
- 共用 Payload registry。
- SavedData migration framework。
- 共用 Config、GUI 與 permission API。
- 第三方 addon 範例與文件。

### Totem Nexus

- 磁石完整管理介面整合與 UX 打磨。
- 石碑完整管理介面的離線玩家查詢、名單瀏覽與批次調整。
- 好友／人體磁石進階設定（同意偏好、通知、黑名單與完整互動流程）。
- 死亡節點傳送成本細節與回收後歷史紀錄介面。

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
