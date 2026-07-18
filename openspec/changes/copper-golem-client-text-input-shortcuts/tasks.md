# Tasks: Copper Golem Client Text Input Shortcuts

## 1. Client input handling

- [x] 1.1 在 `AbstractContainerScreenMixin` 判斷 focused listener 是否為聚焦中的 `EditBox`。
- [x] 1.2 文字框聚焦時攔截 Inventory key，避免預設 `E` 關閉介面。
- [x] 1.3 文字框聚焦時停用 DeadRecall container-sort key。
- [x] 1.4 無文字焦點時保留 Vanilla Inventory key 與既有整理快捷鍵。
- [x] 1.5 不修改 Prompt、API URL、API Key、Model 的保存 payload 或 Server 驗證。

## 2. Automated validation

- [x] 2.1 Java 25 編譯 Client source 與 Mixin 成功。
- [x] 2.2 完整 Fabric Server GameTests 通過。
- [x] 2.3 死亡背包與 Copper Golem 三階段 restart probes 通過最新文件 head。

## 3. Manual client regression

- [ ] 3.1 Sorting Prompt 可輸入包含 `e`／`E` 的英文句子，畫面不關閉。
- [ ] 3.2 Gathering Prompt 可輸入包含 `e`／`E` 的英文句子，畫面不關閉。
- [ ] 3.3 API URL、API Key、Model 可輸入英文、數字、冒號、斜線、句點與連字號。
- [ ] 3.4 Backspace、Delete、左右方向鍵、Home／End、Ctrl+A／C／V 正常。
- [ ] 3.5 文字框聚焦時整理快捷鍵不執行。
- [ ] 3.6 Escape 仍可關閉介面；點擊文字框外後按 Inventory key 仍可正常關閉。
- [ ] 3.7 儲存後重新開啟畫面，Prompt 與 API 設定文字保持一致。

## 4. Documentation

- [x] 4.1 新增 change proposal、design 與 delta spec。
- [x] 4.2 新增 Client 人工驗收文件。
- [ ] 4.3 完成人工 Client 驗收後更新 release notes。
