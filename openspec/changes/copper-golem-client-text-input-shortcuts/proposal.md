# Change: Copper Golem Client Text Input Shortcuts

## Why

銅魔像管理介面的 Prompt、API URL、API Key 與 Model 都使用 `EditBox`，但畫面繼承 `AbstractContainerScreen`。當文字框聚焦時輸入預設 Inventory key `E`，`EditBox` 會等待後續 `charTyped` 事件寫入字元，但容器畫面先把同一次 `keyPressed` 解讀為關閉介面，導致英文文字無法正常輸入。

DeadRecall 的容器整理快捷鍵也可能在文字框聚焦時搶先執行，造成輸入與容器操作衝突。

## What Changes

- 文字框聚焦時，Inventory key 不得關閉容器介面。
- 文字框聚焦時，DeadRecall 容器整理快捷鍵不得執行。
- Prompt、API URL、API Key 與 Model 欄位共用相同行為。
- 文字框未聚焦時，Inventory key 與整理快捷鍵維持原有功能。
- Escape、完成按鈕與 Server 關閉 Menu 流程維持原有行為。

## Impact

### Affected code

- `AbstractContainerScreenMixin`
- `CopperWrenchBindingsScreen` 的四個 `EditBox`
- Client 輸入回歸文件

### Compatibility

- 不修改任何 payload、SavedData、世界資料或 identifier。
- 不修改 Server 權限、revision 或 Prompt 保存流程。
- 修正適用於所有 `AbstractContainerScreen` 中已聚焦的 `EditBox`，避免其他容器文字欄位出現相同問題。
