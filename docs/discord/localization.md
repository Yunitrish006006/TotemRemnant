# Discord 事件繁體中文顯示

## 語系策略

DeadRecall 的 Discord-facing Minecraft 系統事件固定使用繁體中文（`zh_tw`），不依賴：

- Minecraft Client 語言設定。
- 在線玩家的語系。
- Dedicated Server 作業系統語系。
- Discord Worker 或 Bot 的翻譯功能。

Minecraft 26.2 的純 Dedicated Server runtime 不提供完整 Client `zh_tw.json`。因此 DeadRecall 內建與目前 Minecraft 版本鎖定的 Discord 專用翻譯子集，並以 immutable snapshot 使用。伺服器資料包可以覆寫指定 key；啟動與 `/reload` 都會原子替換完整 snapshot。

## 已中文化事件

### Advancement

Discord 會顯示繁體中文進度名稱與類型，例如：

```text
Alex 完成了進度「石器時代」
Alex 完成了目標「鑽石！」
Alex 完成了挑戰「獵取怪物」
```

`task`、`goal`、`challenge` 不會直接以英文顯示。

### 村民升級

未命名村民會顯示通用名稱、職業及前後階級：

```text
村民（圖書管理員）升級：新手 → 學徒
```

有自訂名稱時名稱維持原樣：

```text
Archivist E（圖書管理員）升級：學徒 → 老手
```

### 死亡訊息

死亡 template、攻擊者的 Vanilla 實體名稱及巢狀參數會一起中文化；玩家名稱與 custom item 名稱保持原樣，例如：

```text
Alex 被 殭屍 用 Excalibur-E 殺死
```

### Boss、襲擊與難度

- 終界龍、凋零與其他 Vanilla 實體預設名稱使用繁中；自訂 Boss 名稱保持原樣。
- 襲擊結果顯示「勝利／失敗／停止／結束」。
- `peaceful／easy／normal／hard` 顯示「和平／簡單／普通／困難」。

## 不翻譯的內容

以下文字視為玩家或資料包提供的 literal text，不會被 DeadRecall 改寫：

- 玩家名稱。
- 玩家聊天內容。
- 村民自訂名稱。
- 物品自訂名稱。
- 巢狀 Component 中的 literal 參數。

## 缺漏翻譯

缺少 translation key 時使用中文安全 fallback，例如「未知進度」或「未知實體」。Discord payload 不會直接顯示：

```text
advancements.example.missing.title
```

Fallback 不會建立第二筆 Discord 事件。

## 相容性

中文化不改變：

- `/api/mc/chat` endpoint。
- `event`、`username`、`message`、`channels` payload 欄位。
- `advancement`、`villager_level_up` event ID。
- API Key、Bot Token、Webhook fallback 或多頻道路由。
- SavedData、世界資料或遊戲 identifier。

## 翻譯資源更新

內建 translation snapshot 與 Minecraft 26.2 鎖定，仍隨 DeadRecall 版本發布更新。管理員可在資料包加入任意 `.json`：

```text
data/deadrecall/deadrecall/discord_zh_tw/custom.json
```

內容是 translation key 到繁中 template 的 JSON object，例如：

```json
{
  "entity.minecraft.zombie": "自訂殭屍名稱",
  "options.difficulty.hard": "自訂困難"
}
```

啟用資料包後執行 Minecraft 標準 `/reload`。DeadRecall 會先載入完整 bundled fallback，再合併資料包覆寫，完成後一次替換 snapshot；正在格式化的單筆 Discord 訊息只會使用完整舊版或完整新版，不會混合兩者。

損壞或非 JSON object 的個別檔案會被記錄並略過。移除資料包覆寫後再次 `/reload`，對應 key 會回到 bundled 值。`/discordbridge reload` 只重新讀取 Bridge 設定，不重新載入資料包。

## 人工驗收

1. 啟用 Discord Bridge 並設定測試頻道。
2. 完成至少一個 task、一個 goal 及一個 challenge advancement。
3. 確認 Discord 顯示繁中標題與「進度／目標／挑戰」。
4. 讓未命名圖書管理員從 level 1 升到 level 2。
5. 確認訊息為「村民（圖書管理員）升級：新手 → 學徒」。
6. 為村民設定包含英文或中文的自訂名稱，再次升級並確認名稱未被改寫。
7. 以 Vanilla 生物及有 custom item 名稱的武器觸發死亡訊息，確認 template／實體中文化且 literal 名稱未變。
8. 驗證終界龍或凋零、自訂 Boss 名稱、raid 勝敗及四種 difficulty 顯示。
9. 讓測試 Worker 回傳 503，確認 Server 遊戲事件正常完成且錯誤只記錄於 Discord transport。
10. 加入覆寫 `entity.minecraft.zombie` 的測試資料包並執行 `/reload`，確認新死亡訊息立即使用覆寫值。
11. 移除覆寫並再次 `/reload`，確認值回到內建「殭屍」。
