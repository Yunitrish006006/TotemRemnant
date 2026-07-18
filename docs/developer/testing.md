# 測試與回歸驗證

DeadRecall 使用三層自動測試：純 JVM 的 JUnit、會啟動 Minecraft GameTest Server 的 Fabric GameTest，以及使用正常 Dedicated Server world 的跨程序重啟探針。

## JUnit

適合測試不需要完整世界生命週期的純邏輯，例如：

- 映射表完整性。
- 成本與折抵公式。
- 狀態分類與訊息 key。
- JSON 資源結構。
- 公開 addon inventory provider registry 的順序與重複 ID 防護。

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

## Discord 本地化回歸

Discord 的四組 Dedicated Server GameTests 已明確註冊於測試模組的 `fabric-gametest` entrypoint：

```text
DiscordAdvancementLocalizationGameTest
DiscordVillagerLocalizationGameTest
DiscordSystemEventLocalizationGameTest
DiscordLocalizationReloadGameTest
```

Reload fixture 位於：

```text
src/gametest/resources/data/deadrecall/deadrecall/discord_zh_tw/reload_test.json
```

測試先確認 Server 啟動時已合併 datapack override，再安裝 stale in-memory snapshot，呼叫 Minecraft 真實 `reloadResources`，最後確認 override 被重新載入且 bundled entity 翻譯仍存在。JUnit 另以兩個執行緒交替發布兩份 snapshot 並渲染 nested Component，任何新舊 template 混合都會失敗。

## 死亡背包 addon inventory 回歸

第三方 player-owned slot 相容性由三組測試覆蓋：

```text
DeathBackpackAddonInventoryRegistryTest
DeathBackpackAddonInventoryGameTest
DeathBackpackTrinketsGameTest
```

目前自動覆蓋：

- Provider registry 依註冊順序產生 immutable snapshot。
- 重複 provider ID 直接拒絕。
- 通用 addon SPI 的 addon-only 成功提交。
- Item count 與自訂 `CUSTOM_NAME` Component 保存。
- 第二個 addon slot 拒絕 commit 時，先前已清除的 addon slot、原版 Inventory 與未完成死亡背包全部 rollback。
- Provider snapshot 例外只隔離該 provider，不阻止原版 Inventory 擷取。
- Addon slot 中的 DeadRecall 可攜式容器不會巢狀進死亡背包，也不會被 DeadRecall 擅自清除。
- Trinkets Updated 4.1.x 與 Yumi 在 Minecraft 26.2 GameTest runtime 實際載入。
- Optional Trinkets adapter 正確註冊。
- 真實 Trinkets player `DROP` slot 被 exactly-once 擷取，source 清空，Components 保留且不產生 loose duplicate ItemEntity。

Trinkets fixture 位於：

```text
src/gametest/resources/data/trinkets/slots/deadrecall_test/
src/gametest/resources/data/trinkets/entities/deadrecall_gametest.json
```

DeadRecall 測試只宣告 `DROP` slot。`KEEP`／`DESTROY` 的選擇由 Trinkets 自己的 `forEachDroppable`、slot rule、callback、enchantment 與 keepInventory 邏輯負責；DeadRecall 不複製上游規則。

公開 SPI、transaction 順序與 addon 實作要求記錄於 `docs/developer/death-backpack-addon-inventory-api.md`。

## 可攜式容器巢狀回歸

容器安全由 `PortableContainerNestingGameTest`、`PortableContainerDropperGameTest`、`BackpackMenuNestingInteractionGameTest`、死亡交易與 legacy fixture 共同驗證：

- DeadRecall 一般／死亡背包與 Bundle、17 種 Shulker Box 的雙向拒絕矩陣。
- Shulker Menu 與六個 sided-insertion faces 都拒絕背包。
- 真實 Hopper→Shulker、Hopper Minecart→Hopper→Shulker 與 Dropper→Shulker 路徑保留拒絕物 exactly once，普通物品 control 仍可移動。
- Dispenser 保持 Vanilla ejection 語意：背包與普通 control 都成為唯一的世界 ItemEntity，不會被誤當成 Dropper transfer 而寫入 Shulker。
- 游標、drag、shift-click、number-key、double-click 與兩名玩家同 tick 的 Server 操作都不會插入、刪除、複製或跨玩家污染受限容器。
- 舊世界中已存在的非法巢狀內容可向外取出、不可重新插入，且 Components 不被改寫。
- 死亡擷取與失敗 rollback 不會把 Bundle／Shulker 遞迴包進死亡背包，world fallback 維持 exactly once。

## 好友 PLAYER 傳送回歸

`DirectFriendPlayerTeleportGameTest` 使用同一個 Dedicated GameTest Server 中、已註冊於 `MinecraftServer.getPlayerList()` 的多個真實 `ServerPlayer`，驗證：

- 雙向好友可直接建立 PLAYER session。
- 非好友、單向 pending invite 與 self target 均不得建立 session。
- 解除好友時，同一呼叫內立即取消雙方向 session，且保留第三名玩家的無關 session。
- 目標死亡或從 PlayerList 離線後，下一個 authoritative session tick 取消且不扣成本。
- 倒數期間目標移動後，完成時重新解析最新座標並搜尋安全落點。
- 倒數期間目標切換至 Nether 後，Requester 跟隨最新 Dimension 與安全落點。
- 成功傳送只扣除一次成本，完成後額外 Server ticks 不會再次扣款。

詳細 fixture、事件順序與邊界記錄於 `docs/developer/direct-friend-player-teleport-testing.md`。

## 紫水晶催化傳送回歸

`AmethystCatalystTeleportGameTest` 透過正式 `SpaceUnitHandler.startTeleport`、倒數完成與資源扣款路徑驗證：

- 固定磁石到固定磁石的跨維度報價會合併兩端催化方塊，並只扣一次折抵後的紫水晶碎片。
- 報價後拆除目標催化方塊時，完成前會重新掃描結構、提高成本並按最新報價扣款。
- 玩家來源、玩家目標與死亡節點即使保存了不可能的舊催化數，也不會提供該端折抵。
- Payload codec round-trip 保留 base、兩端催化數、discount 與 final cost；超過 128 個節點、負數／超量長度及不一致報價都會被拒絕。

`runRestartProbe` 另會在 seed 正常關閉後直接移除實際 `space_units.dat` 內的 `amethyst_catalyst_blocks`，再以兩個獨立 JVM 驗證舊世界預設 0、實際石碑重掃為 4，以及更新後 snapshot 再次持久化。

## 傳送介面 Phase A 回歸

`TeleportInterfacePhaseAGameTest` 經 Fabric 的正式 `UseItemCallback`、`UseBlockCallback` 與 `SpaceUnitHandler.startTeleport` 路徑驗證：

- 普通羅盤、回生羅盤、書本與具有 map ID 的已繪製地圖都能建立 Server-only `PLAYER` 與已註冊 `LODESTONE` 來源 context。
- 空白地圖、缺少 map ID 的已繪製地圖與不支援物品保持 `PASS`，不建立 context。
- 四種介面物品對非磁石方塊保持 `PASS`，讓原版方塊互動先處理。
- 非普通羅盤不能註冊未註冊磁石或執行磁石管理；普通羅盤既有能力不變。
- context 保存確切使用手、介面類型與 map ID；物品換手、換類型或 map ID 改變會使 context／session 失效，且不先扣除資源。

純 JUnit 另驗證 filled-map identity 的 map ID invariant。這些測試不取代真人 Client 的 GUI、輸入優先序與多人網路延遲驗收。

## 傳送介面 Phase B 回歸

`TeleportInterfaceQuotePolicyTest` 以純 JVM 矩陣驗證：

- 普通羅盤完整保留基準時間、偏差與磨損率。
- 回生羅盤只對自己的 `DEATH` 目標套用 50% 偏差並使用 floor；他人死亡節點與非死亡目標維持基準。
- 書本只對 `LODESTONE` 目標套用 20% 準備時間與 25% 固定石碑磨損率降低，分別使用 ceil／floor，且準備時間最低 30 ticks。
- 書本不改變偏差；非固定目標的時間與磨損率不變。
- 負數與超量輸入會先 clamp 至合法的時間、偏差與機率範圍。

`TeleportInterfacePhaseBGameTest` 另透過正式 `startTeleport` 建立普通羅盤與書本 session，確認書本 final prepare ticks 已真正寫入 Server session。`SpaceUnitMapPayloadCodecTest` 驗證介面 enum、active flag、說明 key 與獨立 final structure-wear 欄位 round-trip，並拒絕未知 enum、非法範圍與空白／過長 key。

## 傳送介面 Phase C 回歸

`FilledMapCoverageTest` 與 `TeleportInterfaceQuotePolicyTest` 驗證：

- 128×128 像素覆蓋使用最小邊 inclusive、最大邊 exclusive，比例尺 0–4 每級將範圍加倍。
- Dimension 不符永不啟用加成，非法比例尺會被拒絕。
- 覆蓋目標的食物成本使用 ceil 80% 並維持最低 1；偏差使用 floor 80%，準備時間與結構磨損不變。
- 超量食物與其他報價輸入先 clamp 至合法範圍；紫水晶計算仍走既有獨立催化公式。

`TeleportInterfacePhaseCGameTest` 使用真實 `MapItem.create`、Server `MapItemSavedData`、好友 `PLAYER` 解析與正式 session tick 驗證：

- Server map ID／Dimension／中心／比例尺真的會啟用地圖報價，而不是信任 Client 座標。
- Payload 只包含好友的 64 格網格座標與距離級距；精確 Server 位置只用於覆蓋判斷。
- 目標 Dimension 的地圖可覆蓋跨 Dimension 好友並降低食物成本，但紫水晶成本與既有催化最低值完全不變。
- 已啟用加成的好友移出覆蓋範圍會在付款前取消 session。
- 移除啟動地圖的 map ID 也會在付款前取消，食物成本不會先行扣除。

未探索與無權限固定節點仍由既有 `visibleDiscoveredUnits` 權限／探索回歸先過濾，地圖 coverage 不會把它們重新加入 Payload。真人 Client 的地圖手持互動、多人延遲與動態移動仍列為手動驗收。

## 傳送介面 Phase D 回歸

`SpaceUnitMapPayloadCodecTest` 會 round-trip 每個項目的基準與最終食物成本、準備時間、最大水平偏差及固定石碑磨損率，也驗證最終食物在飽和度、飢餓值與物品欄的配置。decoder 會拒絕 `final` 高於 `base` 或配置總額與最終成本不一致的報價。

`TeleportInterfacePhaseDGameTest` 經真實 Server 報價路徑驗證：

- 書本對固定磁石的 Payload 同時保存未折抵與折抵後的準備時間／磨損率，未受影響的食物與偏差保持相等。
- 回生羅盤對自己的死亡節點只降低最終偏差，基準值仍保留，其他報價欄位不變。
- Payload 數值符合正式 `TeleportInterfaceQuotePolicy` 的 ceil／floor 與最低值規則，不由 Client 重算。

Phase A–D、既有好友 `PLAYER` 多人 GameTests 與紫水晶跨 Dimension GameTests 共使用 29 個 required Server GameTests；死亡背包、Space Unit 與銅傀儡的獨立 Dedicated Server restart probes 也持續驗證舊世界保存。Phase D 只增加 transient context、session 與網路欄位，沒有變更 SavedData schema。這些自動測試仍不等同於兩名以上真人 Client 的 UI、網路延遲與動態移動驗收，該項保持為發布前手動測試。

## 講台替代配方回歸

`LecternGameplayGameTest` 使用 Minecraft 26.2 的實際 RecipeManager、Lectern BlockEntity、Menu、紅石排程與村民 POI，驗證：

- 橡木、竹、緋紅蕈木、扭曲蕈木及四種混合半磚皆匹配唯一的 `minecraft:lectern`。
- 每次產出恰好一個講台；舊書櫃 ingredient 不再產出講台。
- 三頁 writable book 的 Components 可經 `LecternBlock.tryPlaceBook` 放入並由 Reading Menu 讀取。
- Menu 翻到最後一頁會同步 authoritative page，Comparator 值由 1 上升至 15。
- 翻頁建立強度 15 的 direct redstone pulse，並在排程兩 tick 後復位；Comparator 頁面訊號不被清除。
- 經 Menu 取書會 exactly-once 回到玩家 Inventory，並清除 `HAS_BOOK` 與 BlockEntity book slot。
- 未就業村民取得真實講台 POI 後，會由 Vanilla `AssignProfessionFromJobSite` Brain 行為確定性認領並成為圖書管理員。

詳細 fixture 與斷言記錄於 `docs/developer/lectern-recipe-testing.md`。

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

1. `seed`：保存 ACTIVE death node、discovery、綁定的 death-backpack ItemEntity，以及具有四個催化方塊的固定磁石 snapshot；正常關閉後把 SavedData 改寫成缺少催化欄位的舊格式。
2. `recover`：重新載入世界，以相同 UUID 的 replacement `ServerPlayer` 回收背包並停用節點；同時確認舊磁石 snapshot 先以 0 載入，再從世界結構重掃為 4。
3. `verify`：再次重新載入，確認節點仍為 `DISABLED`、探索資料存在、已刪除的背包實體不會復活，而且催化 snapshot 仍為 4。

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
- 512 個以不同 Component 防止合併的 ItemEntity（粉末與普通物品各半）可在 10 tick 內完成；粉末原地硬化、普通物品常數時間短路，且流程不查詢世界 entity 集合。

這項壓力測試是 deterministic regression，不設定容易受 CI 主機負載影響的 wall-clock microbenchmark 門檻。真人 Client 同時丟入流動水的網路驗收仍保留為發布前手動項目。

## CI

GitHub Actions 使用 Java 25 執行完整 `build`。GameTest runtime 另外載入 Trinkets Updated 4.1.x 與其 Yumi dependency，以驗證 optional compatibility；正式 `fabric.mod.json` 只使用 `suggests`，未把它們改成伺服器必要依賴。

Build 完成後依序執行死亡背包與 Copper Golem 的三階段正常 Dedicated Server restart probe。失敗時會上傳：

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

新增或修改 Mixin、世界生命週期、Entity、BlockEntity、SavedData、網路流程或 addon inventory transaction 時，至少應新增一項可自動重現的 JUnit、GameTest 或正常 Dedicated Server probe。真人多人測試與長時間壓力測試仍可作為發布前補充驗收，但不能取代伺服器權威自動回歸。
