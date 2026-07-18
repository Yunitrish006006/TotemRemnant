# Design: Copper Golem Client Text Input Shortcuts

## Input event order

Minecraft 文字輸入會先收到 `keyPressed`，可列印字元再由後續 `charTyped` 寫入 `EditBox`。`AbstractContainerScreen` 在 focused child 沒有消耗 `keyPressed` 時，會繼續檢查 Inventory key 等容器快捷鍵。

因此預設綁定為 `E` 時，流程可能變成：

```text
EditBox focused
→ keyPressed(E) 未被 EditBox 消耗
→ AbstractContainerScreen 判定 Inventory key
→ Screen 關閉
→ charTyped('e') 無目標可寫入
```

## Chosen interception point

沿用既有 `AbstractContainerScreenMixin#keyPressed` HEAD injection。每次按鍵先判斷目前 focused listener 是否為已聚焦的 `EditBox`。

### Focused EditBox

- Inventory key matching 時直接回傳 `true`，阻止容器關閉。
- DeadRecall container-sort key 不執行。
- 其他按鍵交回原本 Screen／EditBox 路徑。
- `charTyped` 維持 Vanilla 流程，因此 `e`／`E` 仍寫入文字框。

### No focused EditBox

- Inventory key 維持 Vanilla 關閉行為。
- DeadRecall container-sort key 維持原有行為。

## Scope

Copper Golem 畫面中的以下欄位都透過同一焦點規則處理：

- Sorting binding LLM Prompt
- Gathering LLM Prompt
- LLM API URL
- LLM API Key
- LLM Model

不對字串內容做額外正規化；儲存時仍使用既有 trim、長度限制、權限與 revision 驗證。

## Test strategy

Server GameTest 無法產生真實 Client 鍵盤焦點事件，因此驗證分成：

1. Java 25 Client source／Mixin 編譯。
2. 完整 Server GameTests 與 restart probes，確認共用 Mixin 修改沒有破壞其他系統。
3. 人工 Client 矩陣：`e`、`E`、空白、數字、URL 字元、刪除、方向鍵、貼上、Escape、取消焦點後按 `E`。
