# Proposal: Death Backpack Pre-Drop Capture

## Problem

目前死亡背包流程先讓原版把玩家物品轉成世界中的 `ItemEntity`，再延後兩個 Server task，掃描死亡點半徑 10 格內的新掉落物並回收。這會產生以下結構性風險：

- 同時間其他玩家、怪物、容器或模組產生的掉落物可能被誤收。
- 掉落物可能在收集前合併、被拾取、燒毀、傳送、移動或被其他模組修改。
- 玩家物品在短時間內同時存在於世界與待建立死亡背包流程中，增加複製與遺失窗口。
- 每次死亡需要附近實體掃描、UUID 差集及雙重排程。
- ItemStack Components 經過 ItemEntity 後再回注，增加資料融合或錯誤回注的可能性。

## Proposed change

在 Minecraft 26.2 的 `Player.dropEquipment(ServerLevel)` 中，於原版 `Inventory.dropAll()` 執行前直接讀取玩家權威 Inventory：

1. 原版先處理 `keepInventory` 與消失詛咒。
2. DeadRecall 建立不可變的槽位快照。
3. 排除所有 DeadRecall 背包，避免背包巢狀；排除物仍交給原版掉落。
4. 從快照建立死亡背包 ItemStack 與死亡 Space Unit 節點。
5. 成功生成死亡背包 ItemEntity 後提交槽位移除。
6. 任何失敗都還原槽位，並讓原版 `Inventory.dropAll()` 與舊掃描流程作為 fallback。

## Scope

第一階段涵蓋原版 `Inventory.dropAll()` 管理的玩家主要物品欄、快捷列、裝備與副手。游標物品、玩家 2×2 合成格、第三方飾品槽與外部容器不在第一階段直接擷取範圍，必須在確認 Minecraft 26.2 與第三方 API 的死亡語意後另行整合。

## Compatibility

- `keepInventory=true` 時不觸發，因為原版不會到達 `Inventory.dropAll()` 呼叫點。
- 消失詛咒維持原版先銷毀的行為。
- 所有 DeadRecall 背包維持世界掉落，不放進新死亡背包。
- 第一階段保留舊 post-drop collector 作安全 fallback；直接擷取成功時由相容 Mixin 取消舊流程。
- 不新增 SavedData migration；既有死亡背包與死亡節點格式不變。

## Expected impact

- 移除正常路徑的附近 ItemEntity 掃描與雙重延後排程。
- 降低誤收、漏收、合併、物品破壞及 Component 回注錯誤。
- 為後續離線玩家身體與 Totem Remnant 的統一死亡交易建立可重用基礎。
