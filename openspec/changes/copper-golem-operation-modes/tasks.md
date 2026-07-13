# Tasks: Copper Golem Operation Modes

## 1. Data model and migration

- [x] 1.1 新增 `CopperGolemMode` 與 `CopperGolemActivity`。
- [ ] 1.2 建立 `CopperGolemData`，集中讀寫 common、sorting、gathering。
- [x] 1.3 加入 `data_version = 2`。
- [x] 1.4 無 mode 的舊銅傀儡遷移為 `SORTING`。
- [ ] 1.5 遷移 running、fuel、bindings、LLM、source、blocked。
- [x] 1.6 保留板手 `deadrecall_selected_golem` UUID 相容性。
- [ ] 1.7 為資料 codec 與遷移加入測試。

## 2. Wrench interaction

- [x] 2.1 移除普通左鍵銅傀儡的選擇邏輯。
- [x] 2.2 Shift＋右鍵銅傀儡改為「綁定目前板手＋開啟 GUI」。
- [x] 2.3 重新綁定只覆寫板手 UUID，不改舊銅傀儡。
- [x] 2.4 移除 Shift＋左鍵銅傀儡的粒子路徑操作。
- [x] 2.5 依模式分派方塊左鍵、右鍵、Shift＋右鍵。
- [x] 2.6 銅箱互動在 GATHERING 優先設定 Home。
- [ ] 2.7 防止實體互動與背景方塊互動同時觸發。
- [x] 2.8 加入 UUID 失效、跨維度、距離與權限提示。
- [x] 2.9 銅箱右鍵綁定為單一來源銅箱，左鍵來源銅箱解除。
- [x] 2.10 一般容器右鍵只在 SORTING 加入分類目的地。

## 3. Controller refactor

- [ ] 3.1 建立 `CopperGolemController`。
- [ ] 3.2 將現有分類邏輯移入 `SortingModeController`。
- [ ] 3.3 將燃料移入 `CopperGolemFuelService`。
- [ ] 3.4 將容器綁定移入 `SortingBindingService`。
- [ ] 3.5 保留舊 API 的暫時轉接方法。
- [ ] 3.6 改善 tick 查找，避免每 tick 掃描所有實體。

## 4. Sorting mode isolation and regression

- [x] 4.1 所有搬運 Mixin 入口增加 `mode == SORTING`。
- [x] 4.2 確認 GATHERING 工具不會被視為貨物。
- [x] 4.3 保留每次最多 16 個分類物品。
- [x] 4.4 保留 source slot、return、tried destinations、blocked。
- [x] 4.5 停止、解除最後綁定或切換前嘗試退回來源。
- [x] 4.6 無法退回時保持貨物，不得刪除或掉落。
- [ ] 4.7 回歸測試容器內 DeadRecall 背包分類。

## 5. Mode switching

- [x] 5.1 新增 `ChangeCopperGolemModePayload`。
- [x] 5.2 實作伺服器原子切換。
- [x] 5.3 SORTING → GATHERING 驗證 stopped、主手空、無返回／存放。
- [x] 5.4 GATHERING → SORTING 驗證 stopped、工具空、倉庫空、無工作交易。
- [x] 5.5 成功後停止 navigation 並重設模式 AI memory。
- [x] 5.6 失敗時回傳 reason code 並同步完整狀態。
- [x] 5.7 客戶端移除 optimistic mode update。

## 6. Gathering inventory

- [x] 6.1 新增伺服器工具欄 ItemStack。
- [x] 6.2 新增伺服器採集倉庫 ItemStack。
- [x] 6.3 強制 storage count ≤ 16。
- [x] 6.4 實作工具合法性、插入、取出與耐久保存。
- [x] 6.5 實作倉庫取出與相同 Components 合併。
- [ ] 6.6 定義銅傀儡死亡／移除時工具與倉庫掉落規則。（死亡掉落已完成；非死亡移除仍待安全事件入口）
- [x] 6.7 所有物品變更只在 server thread 執行。

## 7. Gathering configuration

- [x] 7.1 實作 Corner A、Corner B。
- [x] 7.2 實作區域正規化、同維度、每軸與體積上限。
- [x] 7.3 實作 Home 銅箱設定與有效性。
- [x] 7.4 實作板手左鍵切換手動 Block ID。
- [x] 7.5 GUI 提供手動規則查看與刪除。
- [x] 7.6 設定變更時重設 scan cursor 與 target。
- [x] 7.7 將 Home 銅箱與 SORTING 來源箱統一為 shared source copper chest。

## 8. Block LLM classifier

- [ ] 8.1 從現有 LLM service 抽出共用 HTTP client。
- [ ] 8.2 建立 `BlockLlmClassifier`。
- [x] 8.3 固定 JSON schema 與安全解析。
- [x] 8.4 請求包含 Block ID、名稱、Tags、掉落、工具摘要。
- [x] 8.5 加入 Prompt revision。
- [x] 8.6 Prompt 改變時清除採集 LLM 快取。
- [x] 8.7 過期非同步回應不得寫入。
- [x] 8.8 加入 pending 去重、失敗重試與快取上限。
- [x] 8.9 手動規則優先於 LLM。

## 9. Scanner and pathing

- [x] 9.1 建立增量 `GatheringTargetScanner`。
- [x] 9.2 每 tick 使用固定掃描 budget。
- [x] 9.3 只處理已載入區塊。
- [x] 9.4 排除容器、Home、分類綁定箱、不可破壞與危險方塊。
- [ ] 9.5 候選目標通過工具、掉落容量及可達性預檢。
- [x] 9.6 保存 scan cursor，設定變更時重設。
- [x] 9.7 無目標時低頻重試。
- [x] 9.8 抵達後重新驗證 BlockState。

## 10. Breaking and deposit

- [ ] 10.1 建立原子方塊破壞交易。
- [x] 10.2 破壞前計算掉落並驗證單格 16 容量。
- [ ] 10.3 尊重伺服器保護與破壞事件。
- [x] 10.4 成功後才扣工具耐久與燃料。
- [x] 10.5 倉庫滿或工作完成時返回 Home。
- [x] 10.6 Home 存放先合併再用空 slot。
- [x] 10.7 Home 滿或失效時保留倉庫並阻塞。
- [x] 10.8 不得靜默刪除或丟棄溢出物。

## 11. GUI and networking

- [x] 11.1 建立或擴充 `CopperGolemScreenPayload`。
- [x] 11.2 加入 mode、activity、revision、gathering state。
- [x] 11.3 新增模式切換按鈕與拒絕訊息。
- [x] 11.4 新增採集分頁、工具、倉庫、Home、區域、Prompt。
- [x] 11.5 顯示手動目標與 LLM 快取摘要。
- [x] 11.6 所有列表與字串加入 codec 上限。
- [x] 11.7 每個 serverbound action 驗證板手 UUID、距離、維度、權限、模式、revision。
- [x] 11.8 API Key 只同步給管理者。
- [x] 11.9 SORTING UI 顯示來源銅箱＋分類目的地，GATHERING UI 顯示來源銅箱＋採集目標。

## 12. Client visualization

- [x] 12.1 建立手持已綁定板手的 client local particle renderer。
- [x] 12.2 SORTING 顯示分類箱連線與狀態。
- [x] 12.3 GATHERING 顯示工作區、角點、Home、target。
- [x] 12.4 移除持續顯示所需的伺服器粒子 spam。
- [x] 12.5 換手、移除板手、UUID 改變、跨維度時清除 cache。
- [x] 12.6 顯示 blocked activity。

## 13. Tests

- [ ] 13.1 模式切換單元測試矩陣。
- [ ] 13.2 倉庫容量、Components、多掉落拒絕測試。
- [ ] 13.3 工作區與跨維度測試。
- [ ] 13.4 手動／LLM 優先序及過期回應測試。
- [ ] 13.5 分類完整回歸 GameTests。
- [ ] 13.6 採集、返回、Home 滿、工具損壞 GameTests。
- [ ] 13.7 重啟、區塊卸載與資料恢復測試。
- [ ] 13.8 偽造 payload、多人操作與壓力測試。

## 14. Documentation

- [x] 14.1 更新 DeadRecall 完整文檔。
- [ ] 14.2 更新銅板手 Tooltip 與操作說明。
- [x] 14.3 更新語言檔。
- [ ] 14.4 記錄舊資料遷移。
- [x] 14.5 說明單格 16 個、同 Item＋Components、無區塊載入等限制。
- [x] 14.6 整理 v2.2.1 銅魁儡採集掃描、碰撞箱站位與銅錠修復文件。
