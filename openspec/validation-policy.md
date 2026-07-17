# Validation Policy

本 repository 的 OpenSpec 實作流程必須包含可追蹤的建置驗證。

## Required flow

1. 每個實作切片完成後，先查詢對應 commit 的 GitHub Actions／status check。
2. 如果已有失敗或取消的 workflow run，必須重新執行失敗工作或整個 workflow。
3. 如果 commit 沒有產生 workflow run，必須嘗試以 `workflow_dispatch` 手動執行 `.github/workflows/validate.yml`。
4. 如果目前工具或權限無法觸發 workflow，必須明確記錄「未驗證」，不得宣稱 build、測試或啟動已通過。
5. 在取得成功結果前，後續實作可以繼續，但 release readiness 不得標記為完成。

## Minimum validation

- Java 25 toolchain。
- `./gradlew build --stacktrace`。
- JUnit 測試。
- 相關 GameTest 或 Dedicated Server 啟動驗證。
- Payload／Codec 變更必須完成 Client／Server 相同版本連線驗證。
- Mixin 變更必須確認啟動時沒有 injection 或 apply failure。

## Reporting

每次回報需區分：

- 已實作。
- 靜態檢查完成。
- CI 已觸發／重新觸發。
- CI 通過或失敗。
- 尚需遊戲內人工驗證的項目。
