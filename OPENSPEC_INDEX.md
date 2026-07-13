# DeadRecall / Totem Platform OpenSpec Index

此 repository 目前同時保存 DeadRecall 現有功能規格，以及未來拆分為 Totem 模組化生態系的架構文件。

## 平台文件

- [`openspec/README.md`](openspec/README.md)：Totem 平台定位、模組名稱與依賴關係。
- [`openspec/architecture.md`](openspec/architecture.md)：所有模組必須遵守的開發架構與安全規範。
- [`openspec/roadmap.md`](openspec/roadmap.md)：已完成、進行中、尚未完成及建議開發順序。

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
