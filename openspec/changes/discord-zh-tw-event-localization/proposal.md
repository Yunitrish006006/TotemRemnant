# Change: Discord zh-TW Event Localization

## Why

Discord Bridge 目前在多個事件來源直接呼叫 Minecraft `Component.getString()`，結果依 Dedicated Server 的預設語言解析。Vanilla advancement 標題、未命名村民、村民職業、Boss 名稱、死亡訊息中的系統文字等通常因此以英文送到 Discord；村民升級通知目前也只顯示數字等級，沒有中文職業與階級名稱。

玩家在 Minecraft 使用繁體中文時，Discord 公開事件卻混入 `Villager`、`Librarian`、英文 advancement title 或 `task`／`goal`／`challenge`，訊息不一致且難以閱讀。

## What Changes

- 新增 Dedicated Server 可用的 Discord 文字本地化層，Discord-facing locale 第一階段固定為 `zh_tw`。
- 所有 Minecraft 產生的可翻譯 `Component` 在排入非同步 HTTP queue 前，以 `zh_tw` 解析成純文字。
- Advancement 通知使用中文標題與中文 frame type，不再直接傳送英文 `task`、`goal`、`challenge`。
- 村民升級通知保留自訂名稱；未命名村民顯示「村民」，並加入中文職業與前後階級名稱。
- 死亡訊息、Boss／實體名稱、難度與 raid 結果等 Minecraft 系統文字共用同一解析規則，避免各事件各自硬編碼。
- 玩家名稱、玩家聊天、村民自訂名稱、物品自訂名稱與 datapack literal text 必須原樣保留，不得機器翻譯或改寫。
- `zh_tw` 缺少 translation key、格式參數不相容或語系資源載入失敗時，使用安全 fallback；不得把 unresolved translation key 直接送到 Discord，也不得讓事件或 Server crash。
- 不變更 Worker endpoint、event 名稱、頻道路由、Webhook／Bot Token fallback 或 Discord secret 處理。

## Impact

### Affected code

- 新增 Server-only Discord localization／message formatting service。
- `PlayerAdvancementsMixin`：傳遞未提前解析的 advancement title 與 semantic frame type。
- `VillagerEntityMixin`：傳遞村民自訂名稱、profession 與前後 level。
- `DiscordBridge` advancement、villager、death、boss、raid、difficulty 等事件 formatter。
- Dedicated Server localization tests 與 Discord payload capture tests。
- Discord Bridge OpenSpec、Roadmap 與開發者文件。

### Compatibility

- 保留現有 `/api/mc/chat` payload 欄位與 `event` 值。
- 不修改 Discord Bridge 設定檔格式；第一階段不新增可切換 locale。
- 不修改玩家資料、世界資料、SavedData、物品 ID、方塊 ID 或 Component identifier。
- 不依賴 Client-only language class；純 Dedicated Server 必須可啟動。

### Risks

- 嘗試全域替換 Minecraft language instance，意外改變 Server 其他訊息。
- `zh_tw` 資源在 Dedicated Server runtime 不完整，產生 translation key 或英文混雜。
- 巢狀 translatable component、格式參數或自訂名稱被錯誤丟失。
- Resource reload 與 Discord worker thread 同時讀取 translation table，造成競態。
- Advancement 或村民通知在 formatter 重構後重複發送或改變既有事件路由。