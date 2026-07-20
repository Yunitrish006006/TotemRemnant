# Totem Platform OpenSpec

Totem 是由 DeadRecall 演進而來的模組化 Minecraft Fabric 生態系。目標不是維持單一大型模組，而是將現有功能拆成可獨立安裝的模組，並由 `Totem Core` 提供共用基礎能力。

## 模組規劃

| 模組 | 定位 | 目前狀態 |
|---|---|---|
| Totem Core | 共用 Library、API、網路、資料、設定與 GUI 基礎 | 規劃中 |
| Totem Nexus | 磁石、Space Unit、傳送、好友、人體磁石、分散重生 | 設計中 |
| Totem Remnant | 死亡背包、物品回收、死亡紀錄、死亡殘響與離線玩家身體 | 死亡背包核心完成，離線身體設計中 |
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
- [`roadmap.md`](roadmap.md)：已完成、進行中、待排程及未完成項目。
- [`specs/space-unit-lodestone/spec.md`](specs/space-unit-lodestone/spec.md)：Totem Nexus 的磁石傳送與分散重生規格。
- [`specs/copper-golem/spec.md`](specs/copper-golem/spec.md)：現有銅傀儡目標規格。
- [`specs/offline-player-body/spec.md`](specs/offline-player-body/spec.md)：Totem Remnant 的玩家下線後保留身體、重連、死亡與防複製規格。
- [`specs/discord-bridge/spec.md`](specs/discord-bridge/spec.md)：Discord Bridge 的事件轉播、Worker 路由與安全規格。
- [`specs/gameplay-recipes/spec.md`](specs/gameplay-recipes/spec.md)：DeadRecall 覆寫或新增的資料層配方規格。
- [`changes/direct-friend-player-teleport/`](changes/direct-friend-player-teleport/)：雙向好友直接傳送，不再逐次確認。
- [`changes/amethyst-catalyst-teleport-discount/`](changes/amethyst-catalyst-teleport-discount/)：傳送石碑紫水晶催化方塊降低跨維度成本。
- [`changes/teleport-interface-item-specializations/`](changes/teleport-interface-item-specializations/)：普通羅盤、回生羅盤、書本與已繪製地圖的傳送介面與特化規格。
- [`changes/lectern-recipe-override/`](changes/lectern-recipe-override/)：以木半磚與書覆寫講台配方。
- [`changes/concrete-powder-item-hardening/`](changes/concrete-powder-item-hardening/)：混凝土粉末掉落物水中硬化功能。
- [`changes/safe-multi-repo-modularization/`](changes/safe-multi-repo-modularization/)：一次一個功能 repository、DeadRecall compatibility bundle、識別碼基線與可回滾拆分流程。

## 目前專案名稱

現有 repository 與模組暫時維持 `DeadRecall`。模組化拆分完成前，不要求立即修改 mod ID、package 或儲存資料識別碼。重新命名必須提供資料 migration，避免玩家既有世界資料消失。

實體拆分採多 repository 架構；`DeadRecall` 在觀察期內保留為鎖定精確模組版本的 compatibility bundle。新 repository 通過獨立安裝、bundle、舊世界、restart 與 Dedicated Server 驗證前，不得刪除原實作。
