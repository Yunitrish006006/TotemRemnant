# Tasks: Lectern Recipe Override

## 1. Specification

- [x] 1.1 確定替代配方為 4 個木半磚＋1 本書。
- [x] 1.2 確定覆寫 `minecraft:lectern`，不保留重複 recipe ID。

## 2. Resource implementation

- [x] 2.1 新增 `data/minecraft/recipe/lectern.json`。
- [x] 2.2 使用 26.2 現行 shaped recipe schema。
- [x] 2.3 使用 `minecraft:wooden_slabs` Tag。

## 3. Validation

- [x] 3.1 GitHub Actions 使用 Java 25 執行 `./gradlew build --no-daemon --stacktrace` 成功。
- [x] 3.2 Dedicated Server 成功啟動到 `Done`；共載入 1,594 個 recipe，日誌沒有 recipe parse error。
- [x] 3.3 JUnit 資源語意測試鎖定 `data/minecraft/recipe/lectern.json`、`minecraft:crafting_shaped`、4 個 `#minecraft:wooden_slabs`、1 本書與 1 個講台輸出。
- [x] 3.4 `LecternGameplayGameTest` 透過實際 `RecipeManager` 驗證橡木、竹子、緋紅蕈木、扭曲蕈木及混用木種半磚皆匹配唯一的 `minecraft:lectern`，且舊書櫃 ingredient 不再產出講台。
- [x] 3.5 `LecternGameplayGameTest` 驗證可放置三頁書本、開啟 reading menu、翻頁更新 authoritative page、Comparator 1→15、兩 tick 紅石脈衝、取書 exactly-once，以及未就業村民自然認領講台 POI 並成為圖書管理員。

## 4. Documentation

- [x] 4.1 新增 gameplay recipe 規格。
- [x] 4.2 `docs/releases/2.4.1.md`、Roadmap、開發者測試總覽及 `docs/developer/lectern-recipe-testing.md` 已記錄替代配方與遊戲內驗證結果。
