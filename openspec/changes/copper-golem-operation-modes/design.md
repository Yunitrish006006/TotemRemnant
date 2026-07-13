# Design: Copper Golem Operation Modes

## 1. Goals

- 保留現有箱子分類能力並隔離為 `SORTING`。
- 新增不會破壞物品一致性的 `GATHERING`。
- Shift＋右鍵銅傀儡直接綁定板手並開啟 GUI。
- 建立清楚的模式、活動狀態與持久化資料邊界。
- 伺服器作為模式、物品、區域與 LLM 決策的權威來源。
- 避免每 tick 全區掃描與大量粒子封包。

## 2. Non-goals

- 跨維度工作或自動載入區塊。
- 放置方塊、補種作物、重新造林。
- 攻擊生物或主動收集生物掉落。
- 多工具欄、多格採集背包。
- 讓 LLM 決定座標、路徑或直接繞過安全規則。
- Home 滿時把物品丟地上。

## 3. Architecture

```text
CopperGolemController
├── CopperGolemData
├── CopperGolemPermissionService
├── CopperGolemFuelService
├── SortingModeController
│   ├── SortingBindingService
│   ├── SortingInventoryService
│   └── ItemLlmClassifier
├── GatheringModeController
│   ├── GatheringAreaService
│   ├── GatheringInventoryService
│   ├── GatheringTargetScanner
│   ├── GatheringBreakService
│   └── BlockLlmClassifier
├── CopperGolemWrenchHandler
├── CopperGolemNetworkHandler
└── CopperGolemVisualization
```

`TransportItemsBetweenContainersMixin` 只能在 `SORTING` 介入。`GATHERING` 由獨立控制器驅動，不得重用「主手非空即為分類貨物」的判斷。

## 4. Domain model

```java
public enum CopperGolemMode {
    SORTING,
    GATHERING
}

public enum CopperGolemActivity {
    STOPPED,
    IDLE,
    SEARCHING,
    MOVING_TO_TARGET,
    WORKING,
    RETURNING_HOME,
    DEPOSITING,
    BLOCKED_NO_FUEL,
    BLOCKED_NO_TOOL,
    BLOCKED_TOOL_BROKEN,
    BLOCKED_NO_AREA,
    BLOCKED_NO_HOME,
    BLOCKED_HOME_UNAVAILABLE,
    BLOCKED_HOME_FULL,
    BLOCKED_NO_VALID_TARGET,
    BLOCKED_SORTING
}
```

### Common state

```text
data_version
mode
running
activity
fuel_stack
fuel_ticks
```

### Sorting state

```text
bindings[]
binding_llm_configs[]
remembered_source
tried_destinations[]
sorting_blocked_snapshot
```

### Gathering state

```text
tool_stack
storage_stack
area.dimension
area.corner_a
area.corner_b
home_copper_chest
prompt
prompt_revision
manual_allowed_block_ids[]
manual_denied_block_ids[]
llm_allowed_block_ids[]
llm_denied_block_ids[]
llm_allowed_tags[]
llm_denied_tags[]
target_position
scan_cursor
```

## 5. Persistent data

建議仍使用 Entity `DataComponents.CUSTOM_DATA`，加入版本化根節點：

```text
deadrecall_copper_golem:
  data_version: 2
  mode: "sorting"
  running: true
  activity: "idle"
  common:
    fuel_stack: <ItemStack codec>
    fuel_ticks: 0
  sorting:
    bindings: [...]
    llm_bindings: [...]
    remembered_source: {...}
    tried_destinations: [...]
    blocked: {...}
  gathering:
    tool_stack: <ItemStack codec>
    storage_stack: <ItemStack codec>
    area: {...}
    home: {...}
    prompt: ""
    prompt_revision: 0
    manual_allowed_blocks: [...]
    manual_denied_blocks: [...]
    llm_allowed_blocks: [...]
    llm_denied_blocks: [...]
    llm_allowed_tags: [...]
    llm_denied_tags: [...]
```

板手可以保留既有序列化 key `deadrecall_selected_golem`，但程式內視為 `TAG_BOUND_GOLEM_UUID`，避免舊板手失效。

## 6. Wrench interactions

### Entity

| 操作 | 結果 |
|---|---|
| Shift＋右鍵銅傀儡 | 綁定目前板手、同步資料、開啟 GUI |
| 右鍵銅傀儡 | 不執行 DeadRecall 管理行為 |
| 左鍵銅傀儡 | 不再選擇，交回原版攻擊流程 |
| Shift＋左鍵銅傀儡 | 不再顯示路徑 |

重新綁定只覆寫板手 UUID，不修改舊銅傀儡。

### SORTING block interactions

| 操作 | 結果 |
|---|---|
| 右鍵一般容器 | 新增分類目的容器 |
| 左鍵已綁定容器 | 解除分類目的容器 |
| 右鍵銅箱 | 拒絕作為分類目的地 |
| 左鍵非容器 | 不攔截 |

### GATHERING block interactions

| 操作 | 結果 |
|---|---|
| 左鍵普通方塊 | 切換該 Block ID 的手動允許狀態 |
| 右鍵普通方塊 | 設定 Corner A |
| Shift＋右鍵普通方塊 | 設定 Corner B |
| 右鍵或 Shift＋右鍵銅箱 | 設定 Home，優先於角點設定 |

## 7. GUI

頂層包含：銅傀儡識別、模式切換、啟停、activity、阻塞原因、共用燃料，以及 `分類`、`採集`、`LLM` 分頁。

### Sorting tab

- 分類目的容器列表與載入狀態。
- 每個容器的 LLM 開關、Prompt 與快取摘要。
- 目前來源、目的地與 blocked 狀態。

### Gathering tab

- 工具欄。
- 採集倉庫欄與 `count / 16`。
- Home 銅箱位置及狀態。
- Corner A、Corner B、正規化尺寸。
- 手動允許／拒絕列表。
- 採集 Prompt 與 LLM 快取。
- 目前目標與 scanner 狀態。

模式切換不得 optimistic update：客戶端送出請求後等待伺服器完整回傳。

## 8. Mode switching

### SORTING → GATHERING

必須全部成立：

- `running == false`。
- 分類主手貨物為空。
- 沒有 remembered source 待返回。
- 沒有進行中的目的存放。

### GATHERING → SORTING

必須全部成立：

- `running == false`。
- 工具欄為空。
- 採集倉庫為空。
- 沒有正在破壞方塊。
- 沒有返回 Home 或存放中的交易。

切換成功後停止 navigation、清除模式易失 AI memories、保持新模式停止。設定資料保留。

## 9. SORTING mode

### Start conditions

Mixin 只有在下列條件全部成立時介入：

- Entity 是 CopperGolem。
- `mode == SORTING`。
- `running == true`。
- 至少有一個分類綁定。
- 不處於 sorting blocked。
- 有燃料，或主手已持有需完成的分類貨物。

### Transport

- 銅箱是來源，不得作為目的地。
- 取貨時記錄 dimension、來源位置與來源 slot。
- 每次最多取 16 個相同 ItemStack。
- 目的地不得等於來源。
- 依序嘗試其他有效綁定。

### Destination order

1. 容器已有相同 Item＋Components 且可合併。
2. 容器內 DeadRecall 背包已有相同物品且可合併。
3. LLM 已快取允許且存在合法空間。
4. 未知時建立非同步 LLM 請求，本輪不放入。
5. 全部拒絕時返回來源。

### Safety

- 優先回原來源 slot，再嘗試同一來源容器其他合法位置。
- 完全無法放回時保留主手貨物並阻塞。
- 停止、清除最後綁定或準備切換模式時先嘗試返回來源。
- 任何情況不得靜默刪除或自動丟地。

## 10. GATHERING mode

### Tool slot

- 一個伺服器權威 ItemStack。
- 必須完整保存 Components、耐久、附魔及自訂資料。
- 只接受可作為方塊採集工具的物品。
- 工具可以在動畫中顯示，但不得寫入分類貨物主手語意。
- 工具損壞後停止並進入 `BLOCKED_TOOL_BROKEN`。

### Storage slot

採集倉庫是一個 ItemStack，最大數量固定為 16：

- 空倉庫可接收第一種掉落。
- 非空倉庫只接受 `ItemStack.isSameItemSameComponents` 相同掉落。
- 預期掉落總數超過剩餘容量時不得破壞。
- 預期產生多種不同 ItemStack 時不得破壞。
- 破壞前必須在伺服器計算預期掉落。
- 不允許先破壞再刪除溢出物。

### Work area

Corner A 與 Corner B 必須在同一維度，並正規化為 min/max 長方體。

建議預設限制：

- 每軸最多 64 格。
- 總體積最多 262,144 方塊。
- 只掃描已載入區塊。
- 不自動載入區塊。

### Home copper chest

- 必須是 `BlockTags.COPPER_CHESTS` 的有效容器。
- 必須與銅傀儡同維度。
- Home 可在工作區內或外。
- Home 被移除、未載入或滿時，倉庫保持不變並阻塞。
- 存放先合併相同 Item＋Components，再找空 slot。
- 工具不得存入 Home。

### Target decision priority

1. 永久安全拒絕。
2. 手動拒絕 Block ID。
3. 手動允許 Block ID。
4. LLM Block ID 快取。
5. LLM Tag 快取。
6. 未知方塊建立 LLM 請求，本輪跳過。
7. 無 Prompt 且未手動允許時拒絕。

永久拒絕至少包含：

- 空氣、流體、不可破壞方塊。
- Block Entity 容器。
- Home 銅箱與分類綁定容器。
- 會造成銅傀儡立即墜落或窒息的關鍵安全方塊。
- 伺服器保護或事件系統拒絕的位置。

### Block LLM classifier

輸入：Block ID、顯示名稱、Block Tags、預期主要掉落、工具需求摘要及玩家 Prompt。

輸出只接受：

```json
{"match": true, "tags": ["minecraft:mineable/pickaxe"]}
```

LLM 只能分類方塊類型，不能決定座標、路徑、物品交易或繞過安全規則。Prompt 改變時清除 LLM 快取，但保留手動規則。

非同步回應寫入前重新驗證銅傀儡、Prompt revision、Block ID 與目前設定。過期回應直接丟棄。

### Scanner

- 保存 scan cursor。
- 每 tick 只檢查固定 budget，目前為 512 個候選位置。
- 從工作區上層往下掃描已載入、可達位置。
- 找到候選後停止掃描並開始 pathing。
- 成功採集後保留下一段 cursor，後續從上次位置後方繼續搜尋。
- 抵達後重新驗證 BlockState、工具、容量與規則。
- 掃描完整區域仍無目標時低頻重試，不得每 tick 從頭掃描。
- 區域、Prompt、手動規則、工具或 Home 改變時重設 cursor。

### Standing position

- 採集站位腳下必須有穩固支撐。
- 站位空間以銅傀儡自身碰撞箱呼叫 `Level.noCollision` 驗證。
- 不得硬性要求玩家兩格高空間；只要銅傀儡實際碰撞箱塞得進去即可。

### Atomic break transaction

```text
重新讀取 BlockState
→ 驗證仍在區域
→ 驗證仍符合規則
→ 驗證工具適用
→ 計算掉落
→ 驗證完整掉落可放入倉庫
→ 呼叫合法破壞／事件流程
→ 扣工具耐久
→ 扣燃料
→ 將掉落寫入倉庫
→ 提交方塊變更
```

任一前置驗證失敗時，世界、工具、燃料及倉庫均不得改變。

### Runtime flow

```text
STOPPED
  → validate
  → IDLE
  → SEARCHING
  → MOVING_TO_TARGET
  → WORKING
  → SEARCHING

storage full / no target with stored items
  → RETURNING_HOME
  → DEPOSITING
  → SEARCHING

invalid config / full home / no fuel
  → corresponding BLOCKED state
```

## 11. Fuel

兩模式共用燃料：

- 分類模式在成功取貨時消耗既有工作成本。
- 採集模式在成功破壞方塊時消耗工作成本。
- 建議 `FUEL_TICKS_PER_GATHER = 200`。
- 掃描、LLM 等待、路徑失敗或取消破壞不消耗燃料。

## 12. Networking and authority

### Clientbound

建議建立 `CopperGolemScreenPayload`：

```text
golemId
mode
running
activity
fuel
sortingState
gatheringState
llmConfig
permissionFlags
revision
```

### Serverbound actions

```text
ChangeCopperGolemModePayload
SetCopperGolemRunningPayload
CopperGolemFuelSlotPayload
CopperGolemToolSlotPayload
CopperGolemStorageSlotPayload
SetGatheringPromptPayload
SetGatheringAreaPayload
SetGatheringHomePayload
ToggleGatheringBlockRulePayload
UpdateSortingBindingLlmPayload
SaveCopperGolemLlmConfigPayload
```

每個操作都必須驗證：

- 玩家在線且 Entity 是有效 CopperGolem。
- 玩家與銅傀儡同維度且在管理距離內。
- 玩家目前持有的板手綁定 UUID 等於 payload UUID。
- 玩家有管理權限。
- payload revision 不會覆寫較新狀態。
- 目前模式允許該操作。
- ItemStack 必須由伺服器從真實 inventory 移動，不能由客戶端提交物品副本。

## 13. Permissions

- Shift＋右鍵綁定可讓一般玩家開啟 GUI。
- LLM API URL、API Key、Model 維持管理員／單人世界擁有者限制。
- 一般模式設定建議只允許 owner 或管理員。
- 若本次不實作 owner，至少要求距離、同維度及持有已綁定板手。

## 14. Visualization

手持已綁定板手且與銅傀儡同維度時顯示。

### SORTING

- 銅傀儡到分類箱的連線。
- 有效綠色、失效紅色、未載入灰色。
- 額外標示目前來源與目的地。

### GATHERING

- 工作區線框與兩角。
- Home 銅箱。
- 目前採集目標。
- blocked 原因。

持續顯示由客戶端世界渲染；伺服器只在資料變更時同步，不得每 tick 傳送整條粒子路徑。

## 15. Migration

資料版本升至 2：

1. 無 mode 時設為 `SORTING`。
2. 現有 transport enabled 搬到 common running。
3. 現有 fuel 搬到 common。
4. 現有 bindings、LLM、source、blocked 搬到 sorting。
5. 建立空 gathering state。
6. 保留板手 `deadrecall_selected_golem` UUID。
7. 重設易失 AI memory，但不清除貨物來源。
8. 若主手已有貨物，維持 SORTING 直到完成或退回。

## 16. Tests

### Unit

- 模式切換矩陣。
- 工作區正規化與上限。
- 單格 16 個倉庫合併與拒絕規則。
- 手動與 LLM 優先序。
- Prompt revision 與過期回應。
- 舊資料遷移。

### Game tests

- Shift＋右鍵綁定並開 GUI。
- 分類取貨、送貨、退回來源。
- 主手非空時拒絕切至採集。
- 工具或倉庫非空時拒絕切至分類。
- 採集搜尋、破壞、返回與存放。
- Home 滿、工具損壞、區塊卸載與重啟。
- 偽造 payload、多人同時操作及多隻銅傀儡壓力測試。
