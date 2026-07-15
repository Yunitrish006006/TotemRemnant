# Change: Amethyst Catalyst Teleport Discount

## Why

跨維度傳送目前只依路線穩定度計算紫水晶碎片成本，傳送石碑中使用的紫水晶結構方塊不會降低實際消耗。玩家投入更多傳送陣材料後，成本沒有得到對應回饋。

本變更讓固定磁石石碑內的紫水晶催化方塊降低跨維度傳送所需的紫水晶碎片。

## What Changes

- 新增資料驅動 Tag：`deadrecall:space_unit_amethyst_catalysts`。
- 初版預設包含 `minecraft:amethyst_block`；其他水晶方塊可由資料包擴充。
- 現有石碑掃描器記錄催化方塊數量，不額外每 tick 掃描。
- 每累積 4 個有效催化方塊，跨維度成本減少 1 個紫水晶碎片。
- 來源與目標固定磁石的催化數量可合併計算。
- 最終跨維度成本最低為 1，不允許完全免費。
- `PLAYER`、`DEATH` 與其他沒有固定石碑的端點不提供該端折抵。
- 報價 GUI 顯示原始成本、石碑折抵與最終成本。
- 傳送開始與完成前都重新掃描相關固定石碑，避免使用過期折抵。

## Impact

### Affected code

- `SpaceStructureSnapshot`
- `DeadRecallSpaceUnitSavedData` 石碑掃描／codec
- `SpaceUnitHandler.calculateTeleportQuote`
- `TeleportQuote` 與 `SpaceUnitMapPayload`
- `SpaceUnitMapScreen`
- block tag resources
- Nexus 文件與測試

### Compatibility

- `SpaceStructureSnapshot` 新欄位必須使用 optional codec，舊世界預設為 0。
- 不更改現有 Space Unit UUID、位置、Owner 或權限。
- 資料包可增減催化方塊類型。

### Risks

- 折抵數量使用過期結構快照。
- 玩家在報價後拆除水晶仍取得折扣。
- `PLAYER` 端點被錯誤視為具有催化方塊。
- Payload 新欄位造成 Client／Server codec 不一致。
