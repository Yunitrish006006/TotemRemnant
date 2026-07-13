# 開發者文件

本區說明程式碼入口、資料安全、網路與渲染維護原則。正式需求、資料模型與驗收條件以 `openspec/` 為準。

## 文件

- [專案結構](project-structure.md)
- [Data Components 與背包資料](data-components.md)
- [網路與執行緒](networking.md)
- [Mixin 參考](mixins.md)
- [Client Rendering](rendering.md)
- [OpenSpec 索引](../../OPENSPEC_INDEX.md)
- [系統架構](../../openspec/architecture.md)
- [開發路線圖](../../openspec/roadmap.md)
- [銅魁儡完整規格](../../openspec/specs/copper-golem/spec.md)

## 維護規則

1. 玩家可見行為改動時更新 `docs/`。
2. 系統 invariant、資料模型或驗收條件改動時更新 `openspec/specs/`。
3. 尚未完成的設計放入 `openspec/changes/`。
4. 不在 README、玩家文件與 OpenSpec 中複製同一份完整技術內容。
5. API Key、Webhook URL 與其他秘密不得提交到版本控制。