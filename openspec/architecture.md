# Totem Platform Architecture

本文件是 Totem 生態系的架構基準。所有模組與後續 OpenSpec 必須遵守本文件。

## 1. 模組邊界

### Totem Core

提供共用能力：

- Payload 註冊與版本管理。
- SavedData 與資料 migration 基礎。
- Data Component codec 工具。
- Config 載入與同步。
- GUI 共用元件。
- 權限、識別碼、序列化及事件 API。

Core 不得包含具體玩法，不得直接註冊死亡背包、銅傀儡、傳送石碑或採掘錘。

### 功能模組

每個功能模組負責自己的：

- Registry 內容。
- SavedData。
- Payload。
- Server service。
- Client state 與畫面。
- Config 區段。
- Migration。

跨模組資料只能透過公開 API 或事件交換，不得直接讀取其他模組的 private implementation。

## 2. Server Authoritative

Client 不可信。以下資料只能由 Server 決定：

- 世界座標與 Dimension。
- 玩家權限與擁有權。
- ItemStack 內容與成本扣除。
- 傳送落點、偏差與安全檢查。
- 銅傀儡工作狀態。
- 死亡背包內容。
- 分散重生位置。
- 結構掃描與損壞結果。

Client 只傳送操作意圖、目標 UUID、UI 選項及 revision。

## 3. Networking 規範

- 一個 Payload 只負責一個明確操作。
- 禁止使用 `String operation + arbitrary data`。
- 優先傳 UUID、registry identifier 或 revision，不傳可信座標。
- Receiver 必須切回 Server thread 執行世界操作。
- 所有修改型封包都要重新驗證距離、權限、物品與目標狀態。
- 有狀態競爭的 GUI 操作必須使用 revision 或 session ID。
- Clientbound 資料只包含玩家有權查看的欄位。

## 4. SavedData 規範

- 永久世界狀態使用世界 SavedData，不使用 Client JSON 當權威來源。
- 每份資料必須有 `dataVersion`。
- 格式變更必須提供 migration。
- 儲存資料使用穩定 UUID，不以顯示名稱作主鍵。
- Dimension 使用 registry key，不儲存 Java class 名稱。
- 需要位置索引時使用 `Dimension + BlockPos` 組合鍵。
- Runtime session 預設不持久化，除非規格明確要求重啟恢復。

## 5. Data Component 規範

- ItemStack 狀態必須存於 Data Component，不存於 Item singleton 欄位。
- Component 必須提供 codec，網路同步需求另提供 stream codec。
- Component 更新以完整不可變值替換，避免直接修改共享集合。
- 任何舊 NBT 或舊 CustomData 格式都必須定義 migration。

## 6. GUI 規範

- GUI 不直接修改 Server 資料。
- Client 畫面僅維護顯示快照與 pending 狀態。
- 送出操作後等待 Server 回覆，不預先假設成功。
- 錯誤訊息必須指出權限、revision、距離、成本或狀態失敗原因。
- 大型列表應分頁、篩選或使用視窗化渲染。
- 世界位置視覺化不得暴露玩家沒有權限查看的節點。

## 7. Tick 與非同步工作

- 禁止以裸 `Thread.sleep()` 實作遊戲倒數。
- 世界修改必須在 Server thread。
- 網路或 LLM 請求可以非同步，但結果回注必須切回 Server thread。
- 大型掃描、採掘及傳送工作使用 session，每 tick 限制工作量。
- 玩家離線、世界卸載及 Server 關閉時必須清理 session。

## 8. 權限模型

每個系統應明確區分：

- Owner。
- Administrator。
- Explicit Allowed。
- Friend/Public visibility。
- Server operator。

顯示權限與操作權限分離。能看見資料不代表能修改或執行。

## 9. 可選整合

模組不得因可選功能缺席而崩潰。

- Remnant 可發布死亡背包建立／回收事件。
- Nexus 可選擇訂閱事件建立死亡節點。
- Automata 可發布工作工具 API。
- Cognition 可選擇註冊自然語言解譯器。
- 未安裝 Cognition 時，Automata 使用 GUI、規則與手動設定。

## 10. 命名與相容性

- 公開 API package 必須穩定並標示版本。
- Mod ID、component ID、payload ID 與 SavedData key 變更時必須 migration。
- 顯示名稱可使用 Totem 品牌，但現有 DeadRecall 世界資料不可直接作廢。
- 第三方擴充只應依賴公開 API，不依賴 internal package。

## 11. 驗收基準

每個新系統至少要有：

- Server 權限與偽造封包測試。
- 儲存後重啟測試。
- 多玩家狀態隔離測試。
- Dimension 切換測試。
- 玩家離線與 session 清理測試。
- 舊資料 migration 測試。
- Dedicated Server 無 Client class 載入測試。