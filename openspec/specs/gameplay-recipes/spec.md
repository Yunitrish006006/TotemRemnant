# DeadRecall Gameplay Recipe Specification

## Scope

此規格記錄 DeadRecall 對原版或模組配方的資料層調整。配方優先使用 JSON resource，不應為單純 shaped／shapeless recipe 引入 Mixin。

## Lectern

DeadRecall 覆寫 `minecraft:lectern`，使用以下配方：

```text
SSS
 B 
 S 
```

- `S`：任意符合 `minecraft:wooden_slabs` 的木半磚。
- `B`：`minecraft:book`。
- 產出：1 個 `minecraft:lectern`。

實作位置：

```text
src/main/resources/data/minecraft/recipe/lectern.json
```

## Compatibility rules

- 覆寫原版配方時使用相同 namespace 與 recipe ID。
- 不建立第二個功能相同但 ID 不同的重複配方。
- 使用 Minecraft 26.2 現行 recipe schema。
- 資料包可能再次覆寫同一 recipe ID；實際結果由資源載入順序決定。
- 配方變更不得修改既有方塊、物品或世界資料。
