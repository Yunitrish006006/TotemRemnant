# DeadRecall 文件

此目錄是 DeadRecall 的主要文件入口。玩家操作、伺服器設定、開發參考與系統規格分開維護。

## 玩家文件

- [Release Notes](releases/README.md)
- [模組概覽](overview.md)
- [Gameplay Quality of Life](gameplay-qol.md)
- [背包系統](backpacks/README.md)
  - [背包等級與防護](backpacks/tiers.md)
  - [死亡背包與 `/back`](backpacks/death-backpack.md)
  - [整理功能](backpacks/sorting.md)
- [銅魁儡使用指南](copper-golem/README.md)
  - [管理 GUI](copper-golem/gui.md)
  - [分類模式](copper-golem/classify-mode.md)
  - [採集模式](copper-golem/gather-mode.md)
  - [LLM 設定](copper-golem/llm.md)
  - [故障排除](copper-golem/troubleshooting.md)
- [Totem Nexus／Space Unit](nexus/README.md)
  - [磁石註冊、探索與羅盤操作](nexus/space-units.md)
  - [Space Unit 地圖](nexus/map.md)
  - [傳送成本與安全條件](nexus/teleportation.md)
  - [實作狀態](nexus/status.md)
- [附魔台與雕紋書櫃](enchanting/README.md)
- [煉金系統](alchemy/README.md)
  - [材料](alchemy/materials.md)
  - [配方](alchemy/recipes.md)
- [指令參考](commands.md)

## 管理員文件

- [Discord Bridge](discord/README.md)
  - [伺服器設定](discord/server-setup.md)
  - [Cloudflare Worker](discord/worker.md)
  - [故障排除](discord/troubleshooting.md)

## 開發者文件

- [開發者文件入口](developer/README.md)
  - [專案結構](developer/project-structure.md)
  - [Data Components](developer/data-components.md)
  - [網路與執行緒](developer/networking.md)
  - [Mixin](developer/mixins.md)
  - [Client Rendering](developer/rendering.md)
- [OpenSpec 索引](../OPENSPEC_INDEX.md)
- [OpenSpec 目錄](../openspec/README.md)
- [架構說明](../openspec/architecture.md)
- [開發路線圖](../openspec/roadmap.md)

## 文件維護原則

- `README.md`：專案首頁與快速導覽。
- `docs/`：描述目前玩家、管理員或開發者可使用的功能。
- `openspec/specs/`：描述已採用的系統規格、資料模型與 invariant。
- `openspec/changes/`：描述尚在設計、實作或驗證中的變更。
- 玩家可見行為改動時，應同步更新對應的 `docs/` 與 OpenSpec。
- 舊的平面文件只保留遷移提示，不再複製內容。
- 版本資訊以 `gradle.properties`、`fabric.mod.json` 與發佈頁面為準。
