# Design: Lectern Recipe Override

## Resource strategy

使用 Vanilla namespace 覆寫同一 recipe ID：

```text
src/main/resources/data/minecraft/recipe/lectern.json
```

這會讓 recipe manager 載入 DeadRecall 提供的 `minecraft:lectern` 定義，不需要 Mixin 或 Java 註冊。

## Recipe definition

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "SSS",
    " B ",
    " S "
  ],
  "key": {
    "S": "#minecraft:wooden_slabs",
    "B": "minecraft:book"
  },
  "result": {
    "id": "minecraft:lectern",
    "count": 1
  }
}
```

實作前以 Minecraft 26.2 專案現有 recipe schema 為準，確認 Tag ingredient 寫法；不得套用舊版 `item`／`tag` 物件格式。

## Validation

- Gradle `processResources` 與 `build` 通過。
- Dedicated Server 啟動時 recipe manager 無 JSON parse error。
- 任意木種半磚皆可使用。
- 書櫃版本的原版配方不再同時出現。
- 產出講台可正常放置、指派圖書管理員與輸出紅石訊號。
