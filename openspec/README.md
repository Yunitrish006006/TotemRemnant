# Totem Platform OpenSpec

Totem 是由 DeadRecall 演進而來的模組化 Minecraft Fabric 生態系。目標不是維持單一大型模組，而是將現有功能拆成可獨立安裝的模組，並由 `Totem Core` 提供共用基礎能力。

## 模組規劃

| 模組 | 定位 | 目前狀態 |
|---|---|---|
| Totem Core | 共用 Library、API、網路、資料、設定與 GUI 基礎 | 規劃中 |
| Totem Nexus | 磁石、Space Unit、傳送、好友、人體磁石、分散重生 | 設計中 |
| Totem Remnant | 死亡背包、物品回收、死亡紀錄與死亡殘響 | 核心功能已完成 |
| Totem Automata | 銅傀儡、分類、採集、自動化工作模式 | 核心功能進行中 |
| Totem Excavation | 區域採掘錘與未來工程工具 | 待從 Blossom 移植 |
| Totem Cognition | Agent Framework、自然語言、規劃、工具呼叫與 Provider | 研究階段 |

## 核心依賴規則

```text
Totem Core
├── Totem Nexus
├── Totem Remnant
├── Totem Automata
├── Totem Excavation
└── Totem Cognition
```

- `Totem Core` 不得依賴其他 Totem 模組。
- 功能模組預設只依賴 `Totem Core`。
- 模組間整合使用公開 API、事件或可選整合層。
- 不允許循環依賴。
- `Totem Automata` 必須在未安裝 `Totem Cognition` 時仍可完整運作。

## 文件

- [`architecture.md`](architecture.md)：平台架構與強制開發規範。
- [`roadmap.md`](roadmap.md)：已完成、進行中及未完成項目。
- [`specs/space-unit-lodestone/spec.md`](specs/space-unit-lodestone/spec.md)：Totem Nexus 的磁石傳送與分散重生規格。
- [`specs/copper-golem/spec.md`](specs/copper-golem/spec.md)：現有銅傀儡目標規格。
- [`specs/discord-bridge/spec.md`](specs/discord-bridge/spec.md)：Discord Bridge 的事件轉播、Worker 路由與安全規格。

## 目前專案名稱

現有 repository 與模組暫時維持 `DeadRecall`。模組化拆分完成前，不要求立即修改 mod ID、package 或儲存資料識別碼。重新命名必須提供資料 migration，避免玩家既有世界資料消失。
