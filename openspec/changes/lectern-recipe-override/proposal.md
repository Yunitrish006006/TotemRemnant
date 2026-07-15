# Change: Lectern Recipe Override

## Why

目前世界設定使原版講台配方中的書櫃取得不合理或過度受限。講台又是村民職業與書本互動的重要功能方塊，因此需要提供不依賴書櫃的替代配方。

## What Changes

- 覆寫原版 `minecraft:lectern` 配方，而不是新增第二個重複配方。
- 保留原版講台形狀：上排 3 個木半磚、中央材料、下方中央 1 個木半磚。
- 中央材料由書櫃改為一本書。
- 接受 `minecraft:wooden_slabs` Tag 中的任意木半磚。
- 產出 1 個講台。
- 不修改講台方塊、村民職業或紅石行為。

## Recipe

```text
SSS
 B 
 S 
```

- `S`：任意木半磚
- `B`：書

## Impact

### Affected resources

- `src/main/resources/data/minecraft/recipe/lectern.json`
- OpenSpec gameplay recipe 文件
- 發佈變更紀錄

### Compatibility

- 只覆寫配方資料，不需要世界 migration。
- 既有講台不受影響。
- 資料包若也覆寫 `minecraft:lectern`，最後載入順序將決定結果。

### Risks

- 26.2 recipe JSON schema 或資源目錄名稱錯誤。
- 其他資料包覆寫同一 recipe ID。
- 配方過度便宜；初版以一本書取代書櫃，其他材料數量保持原版形狀。
