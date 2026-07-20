# Proposal: Safe Multi-Repository Modularization

## Summary

DeadRecall 目前以單一 Fabric mod、單一 Gradle project 與單一 repository 承載死亡保全、傳送、銅傀儡、Discord Bridge 與其他玩法。目標是讓每個主要功能模組擁有獨立 repository，同時保留 DeadRecall 單一安裝包、既有世界資料與公開識別碼的相容性。

拆分採漸進式 strangler 流程：先在 DeadRecall 內建立明確邊界，再一次抽出一個 repository。新模組必須先通過獨立安裝、DeadRecall compatibility bundle、舊世界 migration、Dedicated Server 與跨模組整合驗證，DeadRecall 才能刪除原實作。

## Motivation

- `Deadrecall` server initializer 已集中約 894 行初始化、Payload receiver 與跨功能事件。
- 33 個 network Payload 和 41 個 Server Mixin 目前由共用設定集中管理。
- `death -> space`、`item <-> inventory` 等直接依賴會在直接搬檔時造成循環 repository dependency。
- 既有 `deadrecall:*` registry、Payload、SavedData 與 resource identifier 已存在於玩家世界及 addon 整合，不能跟著 repository 或 mod ID 任意改名。
- 使用者目前只需要安裝一個 DeadRecall JAR；模組化不能突然要求手動組合多個相依版本。

## Scope

- 定義 `TotemCore`、`TotemDiscordBridge`、`TotemRemnant`、`TotemAutomata`、`TotemNexus` 與 DeadRecall compatibility repository 的所有權。
- 先在 DeadRecall 內拆分 bootstrap、Payload、Mixin、client 與 resource registration 邊界。
- 建立只能由公開 API、事件或可選 integration layer 穿越的 repository boundary。
- 建立 `deadrecall:*` identifier 與輸出資源的相容性基線及 CI 檢查。
- 定義一次一個 repository 的複製、驗證、切換與刪除流程。
- 初期以 lockstep version manifest 組裝 DeadRecall compatibility bundle。
- 最終讓每個功能 repository 擁有獨立 CI、版本與 Modrinth 發布流程。

## Out of Scope for Phase 0

- 建立或刪除 GitHub repository。
- 移動 production Java、resource、Mixin 或 GameTest 檔案。
- 修改 `deadrecall` mod ID、registry namespace、SavedData key 或 Payload ID。
- 修改玩家世界資料格式。
- 改變目前 DeadRecall JAR 的執行行為。
- 啟用各 Totem 模組的獨立正式發布。
