# DeadRecall Discord Bridge 規格

## 1. 目標

Discord Bridge 將伺服器端可公開的 Minecraft 事件轉送到 Discord，經由 Cloudflare Worker 統一處理 API Key 驗證、Webhook 或 Bot Token 發送、多頻道路由與錯誤隔離。

Bridge 是管理與觀測功能，不得影響遊戲邏輯。Discord API、Worker、網路或本地化失敗時，Minecraft Server 必須繼續運作。

## 2. 事件範圍

目前轉播事件：

- 玩家聊天與死亡訊息。
- 玩家第一次加入、加入及離開。
- 會公告聊天的重要 advancement。
- 村民職業等級提升。
- 管理稽核：op、deop、ban、pardon、kick、白名單、difficulty、gamerule。
- 伺服器健康告警。
- 死亡背包建立與回收。
- 公開 Space Unit 更新。
- 終界龍、凋零與襲擊事件。
- Dedicated Server 開啟與關閉狀態。

不在目前範圍：

- 任意系統通知或 Discord 到 Minecraft 的反向聊天。
- 完整指令原文或敏感管理參數。
- 私人 Space Unit、好友可見 Space Unit、死亡節點座標、背包內容、玩家 IP。

新增事件前必須先更新本規格。

## 3. Server 端規則

所有轉播來源必須由 Server event 產生，不得信任 Client payload。

Advancement 僅轉播 display 設定會公告聊天、gamerule 允許且正式 progress 已完成的事件。村民升級由 Server-side career level change 偵測。其他事件使用既有 Fabric Server event、Server lifecycle event 或 Mixin。

一般事件必須非同步送出，避免阻塞 server tick。關服狀態可同步送出，降低程序結束前訊息尚未送出的機率。

Discord Bridge 關閉、設定不完整或傳送失敗時必須安全 no-op 或記錄非敏感錯誤，不得中止事件流程。

設定查詢、儲存與頻道管理 payload 必須經由 Server 權限檢查。Server 不得將既有 API Key 同步回 Client；API Key 欄位留空時應保留既有值。

頻道管理必須驗證 snowflake、去除重複值並限制最多 10 個頻道；Client 清單以 Server authoritative 同步為準。

## 4. Discord-facing 本地化

Minecraft 產生的系統文字在建立非同步 payload 前，必須於 Server thread 轉為固定 `zh_tw` 繁體中文。不得依賴 Client-only language class、玩家 Client locale 或修改 Minecraft 全域 `Language` instance。

Minecraft 26.2 Dedicated Server 不提供完整 Client `zh_tw.json`，因此 DeadRecall 使用版本鎖定的 Discord 專用 immutable translation snapshot。Server Data 資料包可由 `data/deadrecall/deadrecall/discord_zh_tw/*.json` 覆寫指定 key；啟動與標準 `/reload` 必須先以 bundled translations 建立完整 candidate，再原子替換 snapshot。單次 Component render 不得混用 reload 前後的 template。

### 已完成事件

Advancement 必須保留未提前解析的 title Component，並將 semantic frame type 顯示為：

- `task`：進度。
- `goal`：目標。
- `challenge`：挑戰。

格式範例：

```text
Alex 完成了進度「石器時代」
```

村民升級必須包含自訂名稱或通用「村民」、職業、前一階級及目前階級：

```text
村民（圖書管理員）升級：新手 → 學徒
```

玩家名稱、聊天、村民／物品自訂名稱及 nested literal Component 必須維持原樣。

缺少 translation key 時使用安全中文 fallback；最終 Discord payload 不得包含 unresolved raw key，也不得因此重複建立事件。

死亡訊息保留 translatable template 與巢狀 Component 到 localization 邊界；玩家名稱與 custom item literal 不得被改寫。Boss／實體預設名稱、raid result 與 difficulty display name 已共用相同 localization service，不得建立第二套翻譯架構。

## 5. Worker 與路由

Minecraft Server 只呼叫 Cloudflare Worker：

```text
POST /api/mc/chat
POST /api/mc/server/status
```

`/api/mc/chat` payload 使用既有 `event`、`username`、`message` 與 `channels` 欄位。本地化不得更改 endpoint、event ID 或 routing semantics。

所有請求必須帶 `X-API-Key`。

- 有有效 `channels` 且設定 Bot Token 時優先送指定頻道。
- 沒有有效頻道或 Bot Token 不可用時回退 Webhook。
- 聊天與狀態 endpoint 使用相同 routing 規則。
- Worker 驗證 snowflake、移除重複／無效值並限制單次頻道數。

## 6. 安全

不得在一般 log 中輸出完整 API Key、Webhook URL 或 Bot Token。

Worker response 診斷資料不得包含 secret。死亡背包與 Space Unit 通知不得附加未公開座標、背包內容、玩家 IP 或私有資料。

非同步 HTTP worker 只能接收 immutable localized strings，不得讀取 Entity、Level、registry 或其他 mutable Minecraft state。

HTTP 逾時、Discord rate limit、Worker misconfiguration、單一頻道失敗或 localization exception 不得造成 Server crash。

## 7. 驗收條件

- 玩家聊天、死亡、生命週期、管理、健康、死亡背包、公開 Space Unit、Boss、raid 與伺服器狀態事件維持既有路由。
- Advancement 的 Vanilla title 顯示繁體中文，task／goal／challenge 分別顯示「進度／目標／挑戰」。
- Advancement 完成只建立一筆 `advancement` payload。
- 未命名圖書管理員 level 1→2 顯示「村民（圖書管理員）升級：新手 → 學徒」。
- 自訂村民名稱維持 literal，profession 與 level 仍中文化。
- 村民升級只建立一筆 `villager_level_up` payload。
- 未知 key 不洩漏 raw translation key。
- 死亡 template、Vanilla 實體／Boss 名稱、raid 結果與 difficulty 名稱顯示繁中；自訂名稱維持 literal。
- Dedicated Server 不載入 Client-only language class。
- Dedicated Server 啟動及 `/reload` 會套用 Server Data translation override；移除 override 後回到 bundled fallback。
- Reload 中的 Discord Component render 只能觀察完整舊 snapshot 或完整新 snapshot。
- 本地化不更改 Worker endpoint、payload 欄位、event ID、頻道路由或 secret handling。
- 未授權玩家不能讀取 Discord Bridge 設定或開啟管理 GUI。
- Worker、Discord 或 localization 失敗時，Minecraft Server 不崩潰。
- Worker 回傳非 2xx 時，localized event 的來源流程仍正常完成。
