# Design: Amethyst Catalyst Teleport Discount

## Goals

- 讓石碑中的紫水晶材料實際降低跨維度消耗。
- 沿用現有結構掃描，不增加每 tick 世界掃描。
- 保持資料驅動、舊世界相容與 Server authoritative。

## Data model

`SpaceStructureSnapshot` 新增：

```text
amethyst_catalyst_blocks: int = 0
```

Codec 使用 optional field，舊資料讀取為 0。掃描器只計算現有石碑掃描範圍中符合 `deadrecall:space_unit_amethyst_catalysts` 的方塊。

## Cost formula

```text
baseCost = max(2, 2 + ceil((1 - routeStability) × 4))
sourceCatalysts = 固定磁石來源的有效催化方塊數，否則 0
targetCatalysts = 固定磁石目標的有效催化方塊數，否則 0
discount = floor((sourceCatalysts + targetCatalysts) / 4)
finalCost = max(1, baseCost - discount)
```

範例：

| 原始成本 | 兩端催化方塊 | 折抵 | 最終成本 |
|---:|---:|---:|---:|
| 4 | 0–3 | 0 | 4 |
| 4 | 4–7 | 1 | 3 |
| 4 | 8–11 | 2 | 2 |
| 4 | 12+ | 3+ | 1 |

## Quote model

報價應分開保存：

```text
base_amethyst_cost
amethyst_catalyst_blocks
amethyst_discount
amethyst_cost
amethyst_available
```

Client 不得自行計算折抵。地圖初始報價、選取目標刷新、啟動傳送與完成傳送前都由 Server 計算。

## Rescan policy

- 來源為固定磁石時，報價與啟動前重掃來源。
- 目標為固定磁石時，報價與啟動前重掃目標。
- 完成傳送前再重掃相關端點並重新扣款。
- 玩家與死亡節點沒有固定石碑時，該端催化數量固定為 0。

## Tag

```text
data/deadrecall/tags/block/space_unit_amethyst_catalysts.json
```

初版只包含完整紫水晶方塊，避免芽晶與晶簇被重複、方向或成長狀態影響計數。資料包可擴充其他方塊。
