# 測試與回歸驗證

DeadRecall 使用兩層自動測試：純 JVM 的 JUnit，以及會啟動 Minecraft Dedicated Server 的 Fabric GameTest。

## JUnit

適合測試不需要完整世界生命週期的純邏輯，例如：

- 映射表完整性。
- 成本與折抵公式。
- 狀態分類與訊息 key。
- JSON 資源結構。

執行：

```bash
./gradlew test
```

## Fabric Server GameTest

GameTest 原始碼位於：

```text
src/gametest/java/
src/gametest/resources/fabric.mod.json
```

測試模組 ID：

```text
deadrecall-gametest
```

Loom 設定使用 `fabricApi.configureTests` 建立獨立 `gametest` source set。Server GameTests 已接入 `build`，因此以下指令會同時執行 JUnit、編譯模組並啟動 GameTest Server：

```bash
./gradlew build --no-daemon --stacktrace
```

只執行 GameTest Server：

```bash
./gradlew runGameTest --no-daemon --stacktrace
```

目前 Client GameTests 保持停用；需要畫面、滑鼠或 Client-only 渲染驗證時，應另行建立 Client GameTest 或人工測試矩陣。

## 混凝土粉末回歸

`ConcretePowderItemHardeningGameTest` 驗證：

- 水源會使掉落物形式的混凝土粉末硬化。
- 非水源流動水也會硬化。
- 隔著方塊靠近水但沒有接觸時不會硬化。
- 世界正在下雨，但物品沒有浸水時不會硬化。
- 64 格數量與自訂名稱 Component 保留。
- 使用同一個 ItemEntity，不建立替代實體。
- age、位置範圍、速度與 pickup delay 不會被轉換流程重設。

## CI

GitHub Actions 使用 Java 25 執行完整 `build`。失敗時會上傳：

```text
build.log
**/build/reports/
**/build/test-results/
run/gametest/
```

新增或修改 Mixin、世界生命週期、Entity、BlockEntity、SavedData 或網路流程時，至少應新增一項可自動重現的 JUnit 或 GameTest。真人多人測試與壓力測試不能由目前的單一 GameTest Server 完全取代，仍需保留在 OpenSpec 驗收矩陣中。
