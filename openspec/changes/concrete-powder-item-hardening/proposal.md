# Change: Concrete Powder Item Hardening

## Why

Minecraft 原版已會讓放置在世界中的混凝土粉末方塊在接觸水時硬化，但掉落物形式的混凝土粉末不會轉換。這使大量混凝土製作仍需逐格放置與破壞，無法利用水流進行簡單的批次處理。

本變更新增一個獨立的生存品質改善：混凝土粉末 ItemEntity 實際浸入水中時，整個 ItemStack 以 1:1 比例轉換成相同顏色的混凝土物品。

## What Changes

- 支援全部 16 種原版混凝土粉末與對應混凝土。
- 只處理世界中的掉落物 `ItemEntity`。
- ItemEntity 接觸 water-tagged fluid 時，由伺服器執行轉換。
- 整疊物品一次轉換，數量保持不變。
- 使用同一個 ItemEntity 替換其 ItemStack，不另外生成新實體。
- 應保留可安全沿用的 ItemStack Components，例如自訂名稱。
- 不影響物品欄、容器、背包或銅傀儡內的混凝土粉末。
- 不影響原版已存在的混凝土粉末方塊硬化行為。
- 雨水、潑水瓶、含水藥水粒子與未實際接觸 water fluid 的情況不觸發。
- 水鍋不在第一版範圍內，除非目前版本的實體 fluid 判定自然將其視為水中。

## Impact

### Affected code

預計新增或修改：

- `ItemEntity` server-side tick hook 或等效 Fabric callback。
- 混凝土粉末到混凝土的不可變映射。
- ItemStack 安全轉換 helper。
- GameTest／單元測試。
- 玩家功能文件與版本變更紀錄。

### Compatibility

- 不新增持久化資料，不需要世界 migration。
- 不修改 vanilla block、item identifier 或 recipe。
- 不應與資料包配方產生衝突。
- 若其他模組同時替換同一個 ItemEntity ItemStack，DeadRecall 必須在每次轉換前重新讀取目前 stack，且只對仍是支援粉末的 stack 操作。

### Risks

- Client 與 Server 同時轉換造成閃爍或幽靈物品。
- 生成新 ItemEntity 導致拾取延遲、Owner、速度或數量被重設。
- 使用全世界掃描造成大量掉落物時的 tick 成本。
- Components 複製錯誤導致自訂名稱或其他資料遺失。
- 水接觸條件過寬，導致雨中或靠近水但未浸入時轉換。
- Mixin 與其他修改 `ItemEntity.tick` 的模組發生注入衝突。