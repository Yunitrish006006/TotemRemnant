# 銅魔像管理介面 Client 輸入驗收

## 問題背景

`CopperWrenchBindingsScreen` 繼承 `AbstractContainerScreen`。文字框接收可列印字元時，Minecraft 先觸發 `keyPressed`，再觸發 `charTyped`。預設 Inventory key 為 `E`，因此舊行為會在 `charTyped('e')` 前先關閉容器畫面。

修正位於 `AbstractContainerScreenMixin`：只要目前 focused listener 是聚焦中的 `EditBox`，Inventory key 不會關閉畫面，DeadRecall 的 container-sort key 也不會執行。

## 自動驗證

以下項目由 CI 覆蓋：

- Java 25 Client source 與 Mixin 編譯。
- 完整 Fabric Server GameTests。
- 死亡背包三階段 Dedicated Server restart probe。
- Copper Golem 三階段 Dedicated Server restart probe。

自動 Server GameTest 不會產生真實 Client 鍵盤焦點事件，因此仍需執行以下人工矩陣。

## 人工 Client 矩陣

### Sorting Prompt

1. 以板手開啟銅魔像管理介面。
2. 使用 `SORTING` 模式，選取已啟用 LLM 的目的地。
3. 點擊 Prompt 欄位。
4. 輸入：

```text
Accept every eligible item except explosives
```

5. 確認每個 `e`／`E` 都出現在文字框中，畫面沒有關閉。
6. 儲存後重新開啟，確認文字完整保留。

### Gathering Prompt

1. 切換到 `GATHERING` 模式並啟用 LLM。
2. 輸入：

```text
Collect emerald ore and deepslate emerald ore
```

3. 確認介面不會因 `e` 關閉，儲存後文字保持一致。

### API 欄位

分別驗證：

```text
https://api.example.com/v1/chat/completions
sk-example-1234567890
example-model-2026
```

必須可輸入英文、數字、冒號、斜線、句點與連字號。

### 編輯鍵

在每個欄位驗證：

- Backspace、Delete。
- 左右方向鍵、Home、End。
- Ctrl+A、Ctrl+C、Ctrl+V。
- Shift 選取文字。

### 快捷鍵邊界

- 文字框聚焦時按 Inventory key：不得關閉介面。
- 文字框聚焦時按 DeadRecall container-sort key：不得送出整理請求。
- 按 Escape：仍應關閉介面。
- 點擊文字框外，使其失去焦點後按 Inventory key：應正常關閉介面。
- 沒有文字框焦點時，container-sort key 應維持原有行為。

## 限制

本修正不改變 Prompt 或 API 設定的 Server payload、權限、revision、最大長度或儲存規則。人工矩陣完成前，OpenSpec 的 Client 驗收項目保持未勾選。
