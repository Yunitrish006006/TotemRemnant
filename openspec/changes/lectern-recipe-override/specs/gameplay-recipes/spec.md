# Gameplay Recipes Delta Specification

## ADDED Requirements

### Requirement: Accessible lectern recipe

DeadRecall SHALL 覆寫原版講台配方，使講台可由 4 個任意木半磚與 1 本書製作，不再要求書櫃。

#### Scenario: Craft lectern with a book

- **GIVEN** 工作台上排放置 3 個符合 `minecraft:wooden_slabs` 的木半磚
- **AND** 中央放置 1 本書
- **AND** 下方中央放置 1 個符合 `minecraft:wooden_slabs` 的木半磚
- **WHEN** 配方匹配
- **THEN** 產出 SHALL 為 1 個 `minecraft:lectern`

#### Scenario: Any wooden slab is accepted

- **GIVEN** 配方中的半磚來自不同可用木種
- **WHEN** 所有半磚都符合 `minecraft:wooden_slabs`
- **THEN** 配方 SHALL 可製作

#### Scenario: Vanilla bookshelf recipe is replaced

- **GIVEN** DeadRecall 資源已載入
- **WHEN** recipe manager 載入 `minecraft:lectern`
- **THEN** 生效定義 SHALL 為 DeadRecall 的書本配方
- **AND** SHALL NOT 同時顯示另一個相同 ID 的書櫃配方
