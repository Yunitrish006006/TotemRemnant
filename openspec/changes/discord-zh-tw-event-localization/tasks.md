# Tasks: Discord zh-TW Event Localization

## 1. Localization infrastructure

- [x] 1.1 新增 Server-only `DiscordLocalizationService`，Discord-facing locale 固定為 `zh_tw`。
- [x] 1.2 純 Dedicated Server runtime 載入 DeadRecall 內建、Minecraft 26.2 鎖定的 immutable translation snapshot，不依賴 Client-only class。
- [x] 1.3 Runtime Server Data resource reload 與原子 snapshot 替換；bundled snapshot 保持 fallback，datapack 可經 `/reload` 覆寫指定 key。
- [x] 1.4 實作 literal、translatable、巢狀參數、placeholder 與 sibling component 純文字解析。
- [x] 1.5a 未知 key 使用安全中文 fallback，最終 payload 不洩漏 raw translation key。
- [x] 1.5b Missing-key warning 依 key 去重並限制最多 128 種，避免未知 key 洗版；fallback 維持 deterministic。

## 2. Semantic event formatting

- [x] 2.1 Formatter、immutable payload、test observer 與既有 HTTP transport 分離。
- [x] 2.2 Advancement 傳遞未提前解析的 title Component 與 semantic frame type。
- [x] 2.3 `task`／`goal`／`challenge` 顯示為「進度」／「目標」／「挑戰」。
- [x] 2.4 村民升級傳遞 custom name、profession path、previous level 與 current level。
- [x] 2.5 未命名村民、profession 與 level 1–5 使用繁中名稱；自訂名稱維持 literal。
- [x] 2.6 死亡訊息、Boss／實體預設名稱、raid result 與 difficulty display name 接入共用 localization service。
- [x] 2.7 玩家名稱、物品／村民自訂名稱與 nested literal text 不被翻譯或改寫。

## 3. Safety and compatibility

- [x] 3.1 保留既有 Worker endpoint、payload 欄位、event 名稱、多頻道路由與 Webhook／Bot Token fallback。
- [x] 3.2 Localization 例外在 formatter 邊界安全 fallback，不中止 advancement 或村民升級流程。
- [x] 3.3 非同步 HTTP worker 只接收 immutable localized strings，不讀取 Entity、Level 或 registry mutable state。
- [x] 3.4 不新增設定 migration、SavedData、世界資料或 identifier 變更。

## 4. Tests

- [x] 4.1 Vanilla advancement key 解析為預期繁中 title。
- [x] 4.2 Advancement task／goal／challenge 中文格式矩陣。
- [x] 4.3 巢狀 Component 保留玩家名稱、物品自訂名稱與格式參數。
- [x] 4.4 未知 translation key 使用安全 fallback，payload 不包含 raw key。
- [x] 4.5 未命名村民＋圖書管理員＋level 1→2 產生完整中文訊息。
- [x] 4.6 自訂村民名稱原樣保留，profession 與 level 仍中文化。
- [x] 4.7 Advancement 與村民各自 exactly-once 建立 Discord payload。
- [x] 4.8 Dedicated Server GameTest 載入 bundled snapshot，不依賴 Client language class。
- [x] 4.9 本機 HTTP Worker 回傳 503 時，localized payload 已送達且錯誤留在非同步 transport，不回拋到遊戲事件。
- [x] 4.10 Java 25 Validate、完整 Server GameTests、死亡背包與 Copper Golem 三階段 restart probes 全部通過。
- [x] 4.11 Advancement、村民、系統事件與 reload GameTest 類別已正式註冊至 `fabric-gametest` entrypoint；併發 JUnit 驗證單次 Component render 不混用新舊 snapshot，Dedicated Server GameTest 透過真實 resource reload 將 stale snapshot 替換為 datapack 值並保留 bundled fallback。

## 5. Documentation

- [x] 5.1 Discord Bridge 主規格已記錄固定繁中、semantic event 與後續範圍。
- [x] 5.2 `docs/discord/localization.md` 已說明 custom text、fallback 與人工驗收。
- [x] 5.3 `docs/releases/2.4.1.md` 已加入版本變更紀錄與 Discord 顯示驗收矩陣。
- [x] 5.4 `docs/developer/testing.md` 已記錄四組 Discord GameTest entrypoint、reload fixture 與併發 snapshot 驗證。
