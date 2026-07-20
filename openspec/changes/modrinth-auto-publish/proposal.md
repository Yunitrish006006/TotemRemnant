# Proposal: Modrinth Auto Publish

## Summary

DeadRecall 的新版本目前只有 repository 內的版本號、release notes 與 CI build，沒有一致的 Modrinth 發布步驟。新增一條只在 `master` 版本 metadata 變更時執行的發布流程，讓正式 JAR 通過完整 build 後自動建立 Modrinth 版本。

## Scope

- 以 `gradle.properties` 的 `mod_version` 和 `minecraft_version` 為發布來源。
- 以 `docs/releases/<version>.md` 為 Modrinth changelog。
- 上傳 `build/libs/deadrecall-<version>.jar`。
- 支援自動觸發與手動重試。
- 在 repository 外保存 Modrinth project ID 與 token。

## Out of scope

- 自動建立或送審 Modrinth 專案。
- 自動修改版本號或產生 release notes。
- 覆蓋、刪除或修改已存在但內容不同的 Modrinth 版本。
