# Design: Discord zh-TW Event Localization

## Goals

- Discord 的 Minecraft 系統文字固定輸出繁體中文。
- 本地化只在 Server 執行，不依賴玩家 Client locale。
- 玩家名稱、聊天、自訂名稱與 datapack literal text 維持原樣。
- 翻譯失敗不得影響原本遊戲事件。

## Dedicated Server runtime decision

Java 25／Dedicated GameTest classpath 驗證顯示 Minecraft 26.2 不提供 Client 資產：

```text
assets/minecraft/lang/zh_tw.json
```

因此 DeadRecall 不使用 Client language manager，也不修改全域 `Language` instance。第一階段內建 Minecraft 26.2 鎖定的 Discord 專用翻譯子集：

```text
assets/deadrecall/lang/discord_zh_tw/*.json
```

各檔案在 class initialization 時合併為 bundled fallback。Server Data reload listener 另讀取：

```text
data/deadrecall/deadrecall/discord_zh_tw/*.json
```

每次啟動或 `/reload` 都先以 bundled fallback 建立完整 candidate，再依 resource identifier 的穩定順序合併 datapack 覆寫，最後以單次 volatile reference write 發布 `Map.copyOf` immutable snapshot。讀取中的 Component render 先擷取同一份 snapshot reference，包含巢狀參數與 sibling 都不會混用 reload 前後的翻譯。

單一損壞的 JSON resource 只會被忽略；若 resource enumeration 本身失敗則保留目前 snapshot。成功發布新 snapshot 時清除 missing-key warning 去重集合，讓新增／移除的 key 以新版本重新判定。

Phase 2 將 Minecraft 26.2 的 death template 與 entity name 子集合併到同一 snapshot。Missing key 只會依 key 記錄一次 warning，且單次程序最多記錄 128 種，避免 datapack 或版本差異造成 log flood。

## Processing boundary

```text
Server event
→ unresolved Component / semantic registry path
→ DiscordLocalizationService
→ DiscordEventFormatter
→ immutable DiscordEventPayload
→ existing DiscordBridge async HTTP transport
```

非同步 HTTP worker 只接收字串，不得讀取 Entity、Level 或 registry mutable state。

## Component rendering

Resolver 支援：

- literal Component 與 sibling。
- translatable Component。
- `%s`、`%1$s`、`%%` placeholder。
- 巢狀 Component 參數。
- 玩家、物品與村民自訂名稱 literal 保留。

未知 advancement、entity 或其他 key 使用中文安全 fallback；不得把 raw translation key 傳到 Discord。

## Advancement

來源傳遞未提前 `getString()` 的 title Component，frame type 映射：

| Type | 顯示 |
|---|---|
| `task` | 進度 |
| `goal` | 目標 |
| `challenge` | 挑戰 |

格式：`Alex 完成了進度「石器時代」`。同一次完成只建立一筆 `advancement` payload。

## Villager level-up

來源傳遞 custom name、profession path、previous level 與 current level。

- 未命名：使用「村民」。
- Profession：使用繁中名稱。
- Level 1–5：新手、學徒、老手、專家、大師。
- 自訂名稱：原樣保留。

格式：`村民（圖書管理員）升級：新手 → 學徒`。同一次升級只建立一筆 `villager_level_up` payload。

## Phase 2 system events

- 玩家死亡保留原始 death `Component`，在建立 payload 前解析 template、玩家／實體與 custom item 參數。
- 終界龍與凋零使用實體 display-name `Component`；Vanilla 預設名稱翻譯，自訂 literal 名稱保持原樣。
- Raid 結果以 `victory`／`defeat`／`stopped`／`ended` semantic value 進入 formatter。
- Difficulty 以 serialized path 解析 `options.difficulty.*`，不把英文 path 顯示到 Discord。

這些事件與 advancement／村民共用 `DiscordEventNotifications → DiscordEventDispatcher`，因此 transport 只接收 immutable strings。JUnit 使用真實本機 HTTP endpoint 回覆 503，驗證 Worker 失敗留在非同步 transport 且不回拋到遊戲事件。

## Compatibility

不改變 Worker endpoint、`event/username/message/channels` 欄位、event ID、API Key、Bot Token／Webhook fallback、多頻道路由、SavedData 或世界 identifier。

## Phasing

Phase 1 完成 advancement 與村民中文化；Phase 2 完成死亡訊息、Boss／實體名稱、raid result 與 difficulty；Phase 3 完成 Server Data runtime resource reload、bundled fallback 及原子 snapshot 替換，全部沿用同一 service。

## Tests

JUnit 驗證 title、frame type、nested literal、fallback、村民 profession／level、exactly-once payload，以及 reload 與 Component render 併發時只會看到完整的新或舊 snapshot。Dedicated Server GameTest 驗證 bundled snapshot、datapack override 與真實 resource reload，且 advancement／村民各產生一筆 localized payload。完整 CI 仍包含 Server GameTests 與兩套 restart probes。
