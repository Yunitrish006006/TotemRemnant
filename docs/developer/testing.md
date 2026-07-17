# 測試與回歸驗證

DeadRecall 使用三層自動測試：純 JVM 的 JUnit、會啟動 Minecraft GameTest Server 的 Fabric GameTest，以及使用正常 Dedicated Server world 的跨程序重啟探針。

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

## 銅傀儡回歸

銅傀儡的 Server GameTests 分為八組：

```text
CopperGolemRegressionGameTest
CopperGolemPersistenceGameTest
CopperGolemTransportGameTest
CopperGolemSortingRegressionGameTest
CopperGolemLlmAsyncGameTest
CopperGolemGatheringLifecycleGameTest
CopperGolemChunkPersistenceGameTest
CopperGolemAuthorityStressGameTest
```

目前自動覆蓋：

- 模式切換的 stopped、carried cargo、pending source、工具、倉庫與 active target 拒絕矩陣。
- 成功切換後的 revision、activity、scanner state 與 AI memory 重設入口。
- 採集倉庫 16 個上限、Data Components 保存，以及不同 item／component 的拒絕。
- 工作區軸長、體積與跨 Dimension 角點重設。
- 手動採集規則優先於 cached LLM deny。
- LLM pending query 的原子去重、失敗 cooldown、精確重試邊界與 query generation 分離。
- 採集 Prompt revision 改變後拒絕舊 allow／deny 回呼。
- 分類 Prompt 改變或停用後拒絕舊回呼，避免重新污染目前快取。
- 未綁定板手、偽造 UUID、距離與 running slot-edit 權威檢查。
- 兩名玩家的板手綁定彼此隔離。
- stale revision 的 mode、running 與 gathering LLM 操作全部拒絕，且不改變 revision。
- 兩名已綁定玩家以相同 revision 連續提交操作時，只有第一個 mutation 生效。
- 已開啟 Menu 在 running 或 mode 改變後，真實 `ContainerInput.PICKUP` 點擊仍重新驗證 live slot 權限。
- Copper Golem Entity NBT round-trip 的 mode、running、revision、工具耐久、倉庫 Components、區域與手動規則。
- 真實 Chest source／destination 的 16 個取貨、來源 exactly-once 回滾與目的地存放。
- blocked snapshot 的建立與解除、箱內 DeadRecall 背包目的地、解除最後目的地時的 exactly-once 回滾。
- 未載入目的地綁定保留，區塊載入後恢復使用，只有已載入且容器消失時才修剪。
- Home 滿載 preflight、相容 stack 合併與工具最後耐久的原子結果。
- 真實 Server tick 的採集掃描、尋路、可視破壞、掉落入倉、返回 Home 與存放。
- 返回途中 Home 消失時保留採集倉庫並進入 `BLOCKED_HOME_UNAVAILABLE`。
- 工具最後耐久耗盡後清空工具欄、保留掉落，並跨多個 Controller tick 維持 `BLOCKED_TOOL_BROKEN`。
- 真實遠端 chunk unload／reload 保留相同 Entity UUID、Home、target、activity、fuel、工具、倉庫 Components、手動規則與 scanner cursor。
- 128 隻 Copper Golem 同批執行 512-block scanner fixture，並驗證 controller 對 96 隻已移除實體的清理及 32 隻存活實體的保留。
- 遠端 chunk 卸載後移除 controller tracking，重載相同 UUID 的 managed entity 後在 20 個 controller tick 內重新發現。

LLM 非同步測試的詳細約束記錄於 `docs/developer/copper-golem-llm-testing.md`。Chunk 與跨 JVM 保存證據記錄於 `docs/developer/copper-golem-persistence-testing.md` 及 `openspec/changes/copper-golem-operation-modes/13.7-persistence-evidence.md`。

這些測試使用 Server GameTest 的 mock players 與可重播世界 fixture，驗證伺服器權威及同一 Server thread 內的競態順序；它們不等同於真人 Client 網路延遲測試或無上限的生產環境效能基準。

## Dedicated Server restart probes

兩套探針都位於 `gametest` source set，不會被打包進正式模組 JAR。CI 會為每套探針使用獨立 world、marker、環境變數與日誌。

### 死亡背包

Loom run configuration：

```text
runRestartProbe
```

固定世界：

```text
run/restartProbe/world
```

三次獨立 JVM：

1. `seed`：保存 ACTIVE death node、discovery 與綁定的 death-backpack ItemEntity。
2. `recover`：重新載入世界，以相同 UUID 的 replacement `ServerPlayer` 回收背包並停用節點。
3. `verify`：再次重新載入，確認節點仍為 `DISABLED`、探索資料存在，而且已刪除的背包實體不會復活。

環境變數：

```text
DEADRECALL_RESTART_PROBE_PHASE
DEADRECALL_RESTART_PROBE_MARKER_DIR
```

### 銅傀儡

Loom run configuration：

```text
runCopperRestartProbe
```

固定世界：

```text
run/copperRestartProbe/world
```

三次獨立 JVM：

1. `seed`：建立持久化 Copper Golem，保存 Entity UUID、Home、target、mode、running、revision、activity、fuel、工具、倉庫 Components、手動規則與 scanner cursor。
2. `recover`：重新載入並驗證 seed 狀態，再修改工具耐久、倉庫 Components、Home inventory、target block、activity、revision 與 cursor。
3. `verify`：第三次重新載入，確認 recover 階段的修改全部落盤。

環境變數：

```text
DEADRECALL_COPPER_RESTART_PROBE_PHASE
DEADRECALL_COPPER_RESTART_PROBE_MARKER_DIR
```

兩套 probe 都會 force-load 測試 chunk、等待 entity manager 完成載入，執行操作後再等待保存邊界，最後使用正常伺服器關閉路徑。每一階段必須產生 success marker；entrypoint 未執行、world 沒有重用、entity region 未載入或保存狀態錯誤都會讓 CI 失敗。

這些探針驗證實際 Dedicated Server world reload，但不模擬真人 Client 網路登入、封包交換或 UI 操作。

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

GitHub Actions 使用 Java 25 執行完整 `build`，接著依序執行死亡背包與 Copper Golem 的三階段正常 Dedicated Server restart probe。失敗時會上傳：

```text
build.log
restart-*.log
copper-restart-*.log
restart-probe/
copper-restart-probe/
**/build/reports/
**/build/test-results/
run/gametest/
run/restartProbe/
run/copperRestartProbe/
```

新增或修改 Mixin、世界生命週期、Entity、BlockEntity、SavedData 或網路流程時，至少應新增一項可自動重現的 JUnit、GameTest 或正常 Dedicated Server probe。真人多人測試與長時間壓力測試仍可作為發布前補充驗收，但不能取代伺服器權威自動回歸。
