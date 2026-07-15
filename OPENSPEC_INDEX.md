# DeadRecall / Totem Platform OpenSpec Index

此 repository 目前同時保存 DeadRecall 現有功能規格，以及未來拆分為 Totem 模組化生態系的架構文件。

## 平台文件

- [`openspec/README.md`](openspec/README.md)：Totem 平台定位、模組名稱與依賴關係。
- [`openspec/architecture.md`](openspec/architecture.md)：所有模組必須遵守的開發架構與安全規範。
- [`openspec/roadmap.md`](openspec/roadmap.md)：已完成、進行中、待排程、尚未完成及建議開發順序。

## 系統規格

### Totem Nexus / Space Unit

- [`openspec/specs/space-unit-lodestone/spec.md`](openspec/specs/space-unit-lodestone/spec.md)

內容包括：

- 磁石與傳送石碑。
- 羅盤右鍵確認／綁定。
- 玩家到場後以羅盤左鍵啟用磁石。
- 探索與權限雙重過濾。
- 相對位置地圖 GUI。
- 飢餓值、食物及跨維度紫水晶成本。
- 傳送時間、偏差、安全落點及結構磨損。
- 好友、人體磁石及死亡節點。
- 可開關的分散重生 Gamerule。

近期變更：

```text
openspec/changes/direct-friend-player-teleport/
├── proposal.md
├── design.md
├── tasks.md
└── specs/space-unit-lodestone/spec.md

openspec/changes/amethyst-catalyst-teleport-discount/
├── proposal.md
├── design.md
├── tasks.md
└── specs/space-unit-lodestone/spec.md

openspec/changes/teleport-interface-item-specializations/
├── proposal.md
├── design.md
├── tasks.md
└── specs/space-unit-lodestone/spec.md
```

- 雙向好友將直接授權線上 `PLAYER` 目標傳送，不再逐次確認。
- 固定石碑中的紫水晶催化方塊將降低跨維度碎片成本。
- 普通羅盤、回生羅盤、書本與已繪製地圖將可開啟傳送介面，並提供死亡節點精準化、路線典籍與地圖覆蓋特化。

### Totem Automata / Copper Golem

```text
openspec/
├── specs/copper-golem/spec.md
└── changes/copper-golem-operation-modes/
    ├── proposal.md
    ├── design.md
    ├── tasks.md
    └── specs/copper-golem/spec.md
```

已確定設計：

- Shift＋右鍵銅傀儡：綁定目前使用的銅板手並開啟 GUI。
- 現有功能為 `SORTING` 箱子分類模式。
- 新增 `GATHERING` 資源採集模式。
- 採集模式有一個工具欄及一個最多 16 個物品的採集倉庫欄。
- 採集掃描每 tick 最多檢查 512 個候選方塊。
- 模式切換前必須清空對應欄位及進行中工作。
- 採集資源放回指定 Home 銅箱。
- 板手手動規則優先於 LLM。
- 管理 GUI 目標為容器型 `Menu` / `Slot` / `AbstractContainerScreen`。
- 右半邊使用玩家原版背包與快捷欄，銅傀儡內部欄位也使用原版風格且對齊 hitbox 的 slot 底板；面板依 GUI 可用寬高伸縮並重排。
- 銅傀儡內部 slot 僅能繪製與真實 `Slot.x` / `Slot.y` 對齊的一套底板，不再保留假 slot 框；欄位說明使用簡短文字或圖示，不額外說明原版操作。
- 目前動作／activity 以狀態圖示顯示，文字說明只放在 hover tooltip，避免 header 互相遮擋。
- 管理 GUI、tooltip、錯誤訊息及 Discord 設定畫面等玩家可見文字必須走 `assets/deadrecall/lang/*.json` 翻譯 key，不得在 client UI 直接硬編碼中文或英文。
- 燃料、採集工具與採集倉庫使用真實 slot 拖曳與 Shift 點擊。
- 舊的燃料／工具／採集倉庫 serverbound payload 與主手自動放入流程已移除。
- 範圍與路徑顯示由 Client 渲染。
- 受傷銅傀儡可用銅錠右鍵修復。

### DeadRecall / Discord Bridge

- [`openspec/specs/discord-bridge/spec.md`](openspec/specs/discord-bridge/spec.md)

內容包括：

- Minecraft 聊天轉播。
- 玩家死亡訊息轉播。
- 玩家首次加入、加入／離開通知。
- 重要 advancement、管理稽核、健康告警、死亡背包、公開 Space Unit、Boss 與 raid 通知。
- 村民升級通知。
- 伺服器開啟／關閉狀態通知。
- Cloudflare Worker、Webhook 與 Bot Token 頻道路由。

### Totem Remnant / Offline Player Body

- [`openspec/specs/offline-player-body/spec.md`](openspec/specs/offline-player-body/spec.md)

內容包括：

- 玩家下線後保留離線身體。
- Survival／Adventure 玩家登出建立身體，Creative／Spectator、死亡中玩家與關服流程不建立。
- 玩家生命、飢餓、物品、裝備、經驗、狀態效果與位置的權威交接。
- 防止 playerdata 舊副本與離線身體背包造成物品複製。
- 身體受環境、怪物、投射物與符合 PVP 規則的玩家攻擊影響。
- 玩家重新登入時接回仍存活身體；身體死亡時執行一次死亡流程。
- 死亡背包、死亡紀錄、Nexus 死亡節點與 Discord Bridge 死亡事件整合。
- Server restart、crash recovery、管理員修復與資料不一致處理。

### Totem Remnant / Death Backpack Capture

```text
openspec/changes/death-backpack-pre-drop-capture/
├── proposal.md
├── design.md
├── tasks.md
└── specs/death-backpack/spec.md
```

- 在原版 `Inventory.dropAll()` 前直接從玩家權威 Inventory 建立死亡背包。
- 保留 ItemStack 數量與完整 Data Components，不再依賴附近 ItemEntity 回收。
- DeadRecall 背包維持排除並交由原版世界掉落，避免背包巢狀。
- 第一階段保留舊半徑掃描器作失敗 fallback；成功時會取消舊流程。

### DeadRecall Gameplay Recipes

- [`openspec/specs/gameplay-recipes/spec.md`](openspec/specs/gameplay-recipes/spec.md)

```text
openspec/changes/lectern-recipe-override/
├── proposal.md
├── design.md
├── tasks.md
└── specs/gameplay-recipes/spec.md
```

- 講台配方覆寫為 4 個任意木半磚＋1 本書。
- 使用 `data/minecraft/recipe/lectern.json` 覆寫原版 recipe ID。

### DeadRecall Gameplay QoL / Concrete Powder Item Hardening

```text
openspec/changes/concrete-powder-item-hardening/
├── proposal.md
├── design.md
├── tasks.md
└── specs/item-entity-transformations/spec.md
```

待排程功能：

- 16 種混凝土粉末掉落物浸入水中後，1:1 轉成同色混凝土。
- 只由 Server 替換同一個 ItemEntity 的 ItemStack。
- 保留數量、可相容 Components 與實體狀態。
- 不掃描全世界，不影響原版方塊硬化。
- 不需要世界資料 migration。

## 模組命名方向

| 模組 | 內容 |
|---|---|
| Totem Core | 共用 Library 與 API |
| Totem Nexus | 傳送、磁石、好友、Space Unit |
| Totem Remnant | 死亡背包與死亡紀錄 |
| Totem Automata | 銅傀儡與自動化 |
| Totem Excavation | 區域採掘與工程工具 |
| Totem Cognition | 可選 Agent Framework |

在正式拆分前，repository、mod ID 與既有世界資料識別碼暫時維持 DeadRecall，避免破壞相容性。
