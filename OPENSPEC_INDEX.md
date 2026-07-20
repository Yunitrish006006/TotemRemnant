# DeadRecall / Totem Platform OpenSpec Index

此 repository 同時保存 DeadRecall 現有功能規格，以及未來拆分為 Totem 模組化生態系的架構文件。

## 平台文件

- [`openspec/README.md`](openspec/README.md)：平台定位、模組名稱與依賴關係。
- [`openspec/architecture.md`](openspec/architecture.md)：共用開發架構、資料安全與相容性規範。
- [`openspec/roadmap.md`](openspec/roadmap.md)：完成狀態、進行中工作與開發順序。

## Totem Nexus / Space Unit

- [`openspec/specs/space-unit-lodestone/spec.md`](openspec/specs/space-unit-lodestone/spec.md)
- [`openspec/changes/direct-friend-player-teleport/`](openspec/changes/direct-friend-player-teleport/)
- [`openspec/changes/amethyst-catalyst-teleport-discount/`](openspec/changes/amethyst-catalyst-teleport-discount/)
- [`openspec/changes/teleport-interface-item-specializations/`](openspec/changes/teleport-interface-item-specializations/)
- [`openspec/future/totem-nexus-adaptive-multiblock.md`](openspec/future/totem-nexus-adaptive-multiblock.md)：模組拆分後再開發的原版方塊、自適應多方塊傳送陣設計。

涵蓋磁石與石碑、探索與權限、傳送 session、成本／偏差／風險、好友、人體磁石、死亡節點、分散重生，以及傳送介面物品特化。Future 文件另記錄目的地負載、逐步擴建、核心 UUID 歸屬、連結力／擴張力／準確力／傳送力、對稱效率、當地材料共鳴與材料遞減收益；不屬於目前 DeadRecall 發行範圍。

## Totem Nexus / Death Node Administration

- [`openspec/changes/admin-death-node-manager/`](openspec/changes/admin-death-node-manager/)
- [`openspec/changes/admin-death-node-manager/specs/death-node-administration/spec.md`](openspec/changes/admin-death-node-manager/specs/death-node-administration/spec.md)

涵蓋依玩家／Dimension／狀態／時間查詢，以及檢視、傳送、停用、永久刪除、批次清理、確認 token 與稽核紀錄。

## Totem Automata / Copper Golem

- [`openspec/specs/copper-golem/spec.md`](openspec/specs/copper-golem/spec.md)
- [`openspec/changes/copper-golem-operation-modes/`](openspec/changes/copper-golem-operation-modes/)
- [`openspec/changes/copper-golem-client-text-input-shortcuts/`](openspec/changes/copper-golem-client-text-input-shortcuts/)
- [`docs/developer/copper-golem-client-input-testing.md`](docs/developer/copper-golem-client-input-testing.md)

`SORTING`／`GATHERING`、容器型管理 GUI、來源與目的地、採集工作區、工具／倉庫／燃料、Home、LLM、Client 視覺化、權威 payload、chunk persistence、跨 JVM probe 與壓力測試均已完成。Client text-input change 修正 Prompt／API 欄位聚焦時 Inventory key 與整理快捷鍵搶占輸入的問題；實際鍵盤驗收狀態以該 change 的 `tasks.md` 為準。

## DeadRecall / Discord Bridge

- [`openspec/specs/discord-bridge/spec.md`](openspec/specs/discord-bridge/spec.md)
- [`openspec/changes/discord-zh-tw-event-localization/`](openspec/changes/discord-zh-tw-event-localization/)

涵蓋 Minecraft 聊天、玩家生命週期、死亡背包、Space Unit、管理稽核、健康告警、Boss／raid、村民升級與伺服器狀態通知。`discord-zh-tw-event-localization` 已統一處理 advancement、村民、死亡、Boss／實體、raid 與 difficulty 的繁體中文解析、custom text 保留、安全 fallback、missing-key 節流、Worker 失敗隔離，以及 Server Data runtime reload 與原子 snapshot 替換。

## Totem Remnant / Offline Player Body

- [`openspec/specs/offline-player-body/spec.md`](openspec/specs/offline-player-body/spec.md)

涵蓋登出身體、重連接回、playerdata body lock、防複製、身體死亡、死亡背包／死亡節點／Discord 整合，以及 restart／crash recovery。

## Totem Remnant / Death Backpack Capture

- [`openspec/changes/death-backpack-pre-drop-capture/`](openspec/changes/death-backpack-pre-drop-capture/)
- [`openspec/changes/death-backpack-pre-drop-capture/specs/death-backpack/spec.md`](openspec/changes/death-backpack-pre-drop-capture/specs/death-backpack/spec.md)

目前設計：

- 在原版 `Inventory.dropAll()` 前直接從玩家權威來源建立死亡背包。
- 支援 Inventory、Equipment、游標、玩家 crafting inputs 與明確白名單的原版工作站 inputs。
- 保留 ItemStack count 與完整 Data Components。
- 外部持久容器、result preview、DeadRecall 背包與禁止巢狀的可攜式容器保持排除。
- 公開 addon inventory transaction SPI 支援自訂 player-owned slots、commit-time compare-and-clear、反向 rollback 與 Inventory fallback。
- Trinkets Updated 4.1.x／Minecraft 26.2 optional adapter 只擷取上游最終判定為 `DROP` 的 slots。
- 未安裝 Trinkets Updated 時 DeadRecall 可獨立啟動。
- 舊附近 ItemEntity 掃描、UUID 差集、雙重排程與第二條死亡背包 fallback 已完整移除；交易失敗只回到原版死亡掉落。

開發者 API 文件：[`docs/developer/death-backpack-addon-inventory-api.md`](docs/developer/death-backpack-addon-inventory-api.md)。

## DeadRecall / Container Safety

- [`openspec/changes/container-nesting-restrictions/`](openspec/changes/container-nesting-restrictions/)
- [`openspec/changes/container-nesting-restrictions/specs/container-safety/spec.md`](openspec/changes/container-nesting-restrictions/specs/container-safety/spec.md)

禁止 DeadRecall 背包與 Bundle、Shulker Box 或設定型可攜式容器雙向巢狀；GUI、Shift 點擊、拖曳、自動化與死亡 transaction 共用 Server policy。

## Gameplay Recipes

- [`openspec/specs/gameplay-recipes/spec.md`](openspec/specs/gameplay-recipes/spec.md)
- [`openspec/changes/lectern-recipe-override/`](openspec/changes/lectern-recipe-override/)
- [`docs/developer/lectern-recipe-testing.md`](docs/developer/lectern-recipe-testing.md)

講台配方覆寫為 4 個任意木半磚＋1 本書；木種混用、書本／Menu、Comparator／紅石脈衝及村民圖書管理員 POI 已由 Server GameTest 驗證。

## Gameplay QoL / Concrete Powder Item Hardening

- [`openspec/changes/concrete-powder-item-hardening/`](openspec/changes/concrete-powder-item-hardening/)
- [`openspec/changes/concrete-powder-item-hardening/specs/item-entity-transformations/spec.md`](openspec/changes/concrete-powder-item-hardening/specs/item-entity-transformations/spec.md)

16 色混凝土粉末 ItemEntity 水中硬化核心與 required GameTests 已完成；轉換保留數量、相容 Components、同一 Entity 及其 age／位置／速度／pickup delay。

## Release Automation

- [`openspec/changes/modrinth-auto-publish/`](openspec/changes/modrinth-auto-publish/)
- [`openspec/changes/modrinth-auto-publish/specs/release-automation/spec.md`](openspec/changes/modrinth-auto-publish/specs/release-automation/spec.md)

`master` 上版本號變更會在完整 build 後發布到 Modrinth；project ID 與 token 由 GitHub Actions variable／secret 提供，且以版本號及 JAR SHA-512 防止重複或覆蓋發布。

## 模組命名方向

| 模組 | 內容 |
|---|---|
| Totem Core | 共用 Library 與 API |
| Totem Nexus | 傳送、磁石、好友、Space Unit |
| Totem Remnant | 死亡背包、死亡紀錄與離線身體 |
| Totem Automata | 銅傀儡與自動化 |
| Totem Excavation | 區域採掘與工程工具 |
| Totem Cognition | 可選 Agent Framework |

在正式拆分前，repository、mod ID 與既有世界資料 identifier 暫時維持 DeadRecall。功能完成狀態以 [`openspec/roadmap.md`](openspec/roadmap.md) 及各 change 的 `tasks.md` 為準。
