# Design: Concrete Powder Item Hardening

## 1. Goals

- 讓掉落物形式的混凝土粉末在實際浸入水中時轉成同色混凝土。
- 保持整疊數量、實體位置、速度、Owner 與拾取延遲。
- 僅由伺服器修改權威 ItemStack。
- 不掃描所有區塊或所有 ItemEntity。
- 不改變原版混凝土粉末方塊硬化流程。

## 2. Non-goals

- 不加入新的混凝土配方或方塊。
- 不處理物品欄、容器、背包或機器內的粉末。
- 不讓雨、潑水瓶或視覺水粒子觸發轉換。
- 第一版不特別支援水鍋。
- 不加入乾燥、反向轉換或不同顏色混合。
- 不以世界 tick 全量搜尋掉落物。

## 3. Mapping

實作 SHALL 使用明確、不可變且完整的 16 色映射：

| Concrete powder | Concrete |
|---|---|
| white concrete powder | white concrete |
| orange concrete powder | orange concrete |
| magenta concrete powder | magenta concrete |
| light blue concrete powder | light blue concrete |
| yellow concrete powder | yellow concrete |
| lime concrete powder | lime concrete |
| pink concrete powder | pink concrete |
| gray concrete powder | gray concrete |
| light gray concrete powder | light gray concrete |
| cyan concrete powder | cyan concrete |
| purple concrete powder | purple concrete |
| blue concrete powder | blue concrete |
| brown concrete powder | brown concrete |
| green concrete powder | green concrete |
| red concrete powder | red concrete |
| black concrete powder | black concrete |

不得依 registry path 字串替換猜測目標物品，避免名稱規則或其他模組 identifier 造成錯誤。

## 4. Runtime flow

建議使用 `ItemEntity` 的 server-side tick hook，或目前 Fabric／Minecraft 26.2 中等效且不需要全量掃描的實體更新入口。

```text
ItemEntity tick
  → server-side only
  → read current ItemStack
  → lookup supported concrete powder mapping
  → verify entity is currently in water-tagged fluid
  → create corresponding concrete stack
  → replace stack on the same ItemEntity
```

執行順序必須在每次操作前重新讀取 ItemEntity 的目前 stack，避免其他模組已先修改物品後仍套用過期結果。

## 5. Water condition

實作時必須依 Minecraft 26.2 mappings 確認正確 API，不得直接套用其他版本的方法名稱。

觸發條件 SHALL 符合：

- ItemEntity 的碰撞體或原版水中狀態確認它實際接觸 `FluidTags.WATER`。
- Client side 不執行 ItemStack 變更。
- 靠近水但沒有接觸 fluid 不觸發。
- 雨中不觸發。
- 水流與水源都可觸發。
- 若 bubble column 在目前版本被原版 fluid API 視為 water-tagged fluid，則應自然支援。

## 6. ItemStack conversion

優先使用目前版本提供的 `ItemStack.transmuteCopy(targetItem)` 或等效 API，保留數量與可安全沿用的 Components。

轉換必須：

- 1:1 保留 count。
- 不超過目標物品最大堆疊數量；原版混凝土粉末與混凝土均為相同堆疊上限。
- 不複製不適用或會造成非法狀態的 block-specific component。
- 在同一個 ItemEntity 上執行 `setItem` 或等效安全替換。
- 不建立第二個 ItemEntity。
- 替換後不再符合粉末映射，因此同一實體不會重複轉換。

因為保留同一 ItemEntity，下列實體狀態應自然保留：

- UUID。
- position。
- velocity。
- pickup delay。
- thrower／owner。
- age 與 despawn timer。

## 7. Performance

- 每個 ItemEntity tick 最多進行一次常數時間 map lookup 和一次水狀態判定。
- 非混凝土粉末應在 map lookup 後立即返回。
- 不建立每 tick 臨時映射。
- 不使用 `ServerLevel#getEntities` 全量查詢。
- 不載入未載入區塊。

自動壓力回歸在同一個水中 fixture 建立 512 個帶不同自訂名稱、因此不可合併的 ItemEntity：一半為支援粉末、一半為普通物品。測試要求 10 tick 內完成，且每個粉末都在原實體上轉換、普通物品保持不變。這是偵測明顯退化與錯誤全量掃描的 deterministic regression，不以易受 CI 主機負載影響的 wall-clock 門檻充當 microbenchmark。

若實際 profile 顯示每 tick fluid 判定成本過高，可在保持行為一致的前提下降低檢查頻率，但第一版不應加入會造成長時間延遲的節流。

## 8. Effects

聲音與粒子不是第一版必要條件。若加入：

- 只能由 Server 廣播一次。
- 不得每 tick 重複播放。
- 大量物品同時轉換時應避免過量粒子或聲音。
- 視覺效果失敗不得影響物品轉換。

## 9. Test matrix

至少驗證：

1. 16 種粉末各自轉成正確顏色。
2. 單個與 64 個整疊皆保持數量。
3. 水源與流動水都能轉換。
4. 靠近水但未接觸不轉換。
5. 雨中不轉換。
6. 玩家物品欄與容器中的粉末不轉換。
7. 自訂名稱等 Components 在合法情況下保留。
8. ItemEntity 的位置、速度與拾取延遲不因轉換重設。
9. Client 不產生幽靈副本。
10. 大量 ItemEntity 同時存在時不進行全量世界掃描。
