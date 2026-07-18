# 講台替代配方遊戲內驗證

## 驗證目標

DeadRecall 以 `data/minecraft/recipe/lectern.json` 覆寫 Vanilla 的 `minecraft:lectern` recipe ID。自動測試不只檢查 JSON，而是使用 Minecraft 26.2 的實際 `RecipeManager`、`LecternBlockEntity`、`LecternMenu`、紅石排程與村民 POI 系統。

測試入口：

```text
src/gametest/java/com/adaptor/deadrecall/recipe/LecternGameplayGameTest.java
```

## 配方矩陣

`replacementRecipeMatchesWoodFamiliesAndMixedSlabs` 建立 3×3 `CraftingInput`，並要求匹配的 recipe ID 必須是 `minecraft:lectern`、產物必須恰好是一個講台。

已驗證：

- 四個橡木半磚。
- 四個竹半磚。
- 四個緋紅蕈木半磚。
- 四個扭曲蕈木半磚。
- 橡木、竹、緋紅蕈木與扭曲蕈木各一個的混合配方。
- 舊版「木半磚＋書櫃」輸入不再產出講台。

這會同時證明 `#minecraft:wooden_slabs` 在 runtime recipe manager 中接受 Overworld、Nether 與竹木家族，且 Vanilla namespace 覆寫只保留新的 recipe 定義。

## 書本、Menu 與紅石

`lecternAcceptsBookPulsesRedstoneAndSupportsReadingMenu` 使用真實講台 BlockEntity：

1. 建立帶三頁 `WRITABLE_BOOK_CONTENT` 的書本。
2. 經由 `LecternBlock.tryPlaceBook` 放入講台。
3. 驗證 `HAS_BOOK`、BlockEntity book slot 與頁面 Components。
4. 由 `LecternMenu` 跳到第三頁。
5. 驗證 authoritative page 同步至 BlockEntity。
6. 驗證 Comparator 值由第一頁的 1 上升至最後一頁的 15。
7. 驗證翻頁產生強度 15 的 `POWERED` 脈衝。
8. 驗證排程兩 tick 後脈衝復位，但 Comparator 頁面訊號維持。
9. 經由 Menu 取書，驗證書本 exactly-once 回到玩家 Inventory，且 `HAS_BOOK` 與 BlockEntity slot 清空。

## 圖書管理員 POI

`unemployedVillagerClaimsLecternAndBecomesLibrarian` 生成未就業村民與真實講台，先要求 `PoiManager.take` 能以 `PoiTypes.LIBRARIAN` 認領該座標，再把取得的 `GlobalPos` 寫入 `POTENTIAL_JOB_SITE`，最後執行 Vanilla `AssignProfessionFromJobSite` Brain 行為。這避免依賴村民隨機 AI 排程造成高負載 suite 超時，同時保留真實 POI 類型、Memory transition 與職業解析。

此測試沒有直接呼叫 `setVillagerData`；它要求 `POTENTIAL_JOB_SITE` 轉成相同座標的 `JOB_SITE`，且 profession holder 由 Vanilla 行為變成 `VillagerProfession.LIBRARIAN`。因此仍可驗證講台被登錄為圖書管理員工作站，而不是只檢查靜態常數。

## 執行

```bash
./gradlew runGameTest --no-daemon --stacktrace
```

完整發布驗證：

```bash
./gradlew build --no-daemon --stacktrace
./gradlew runRestartProbe --no-daemon --stacktrace
./gradlew runCopperRestartProbe --no-daemon --stacktrace
```

配方覆寫沒有新增 Java runtime hook、Mixin、SavedData 或 identifier migration。測試通過代表資源覆寫沒有破壞講台原版 BlockEntity、Menu、紅石及村民工作站行為。
