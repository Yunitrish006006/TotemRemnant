# Copper Golem Specification

## Purpose

定義 DeadRecall 銅傀儡的板手綁定、共通運作狀態、箱子分類模式、資源採集模式、LLM 分類、資料安全、GUI、網路驗證與客戶端視覺化。

## Requirements

### Requirement: Copper ingot repair

系統 SHALL 允許玩家以手持銅錠右鍵受傷的銅傀儡來修復該銅傀儡。每次成功修復 SHALL 消耗 1 個銅錠，創造模式玩家除外。

#### Scenario: Repair damaged copper golem

- **GIVEN** 玩家手持銅錠
- **AND** 目標銅傀儡目前生命值低於最大生命值
- **WHEN** 玩家右鍵該銅傀儡
- **THEN** 伺服器 SHALL 修復該銅傀儡 4 點生命值，且不超過最大生命值
- **AND** 非創造模式玩家 SHALL 消耗 1 個銅錠
- **AND** 板手綁定、模式、燃料、工具、採集倉庫與容器設定 SHALL NOT 被修改

#### Scenario: Full health consumes nothing

- **GIVEN** 玩家手持銅錠
- **AND** 目標銅傀儡生命值已滿
- **WHEN** 玩家右鍵該銅傀儡
- **THEN** DeadRecall SHALL NOT 消耗銅錠
- **AND** DeadRecall SHALL NOT 修改銅傀儡設定資料

### Requirement: Wrench binding

系統 SHALL 讓玩家以手持銅板手 Shift＋右鍵銅傀儡的方式，將該板手綁定至目標銅傀儡，並立即開啟目標銅傀儡的管理 GUI。

#### Scenario: Bind an unbound wrench

- **GIVEN** 玩家手持尚未綁定的銅板手
- **WHEN** 玩家 Shift＋右鍵一隻銅傀儡
- **THEN** 伺服器 SHALL 將該銅傀儡 UUID 寫入觸發互動的板手
- **AND** SHALL 傳送該銅傀儡的最新管理資料
- **AND** 客戶端 SHALL 開啟管理 GUI

#### Scenario: Rebind to another golem

- **GIVEN** 板手已綁定銅傀儡 A
- **WHEN** 玩家 Shift＋右鍵銅傀儡 B
- **THEN** 板手綁定 SHALL 改為銅傀儡 B
- **AND** 銅傀儡 A 的設定與運作狀態 SHALL NOT 被修改

#### Scenario: Open the same golem again

- **GIVEN** 板手已綁定銅傀儡 A
- **WHEN** 玩家再次 Shift＋右鍵銅傀儡 A
- **THEN** 綁定 SHALL 保持不變
- **AND** GUI SHALL 使用伺服器最新資料重新開啟

### Requirement: Removed legacy selection interaction

系統 SHALL NOT 使用普通左鍵銅傀儡作為板手選擇操作，且 SHALL NOT 使用 Shift＋左鍵銅傀儡作為視覺化入口。

#### Scenario: Left-click no longer binds

- **GIVEN** 玩家手持銅板手
- **WHEN** 玩家普通左鍵銅傀儡
- **THEN** 板手 UUID SHALL NOT 因 DeadRecall 邏輯而改變
- **AND** DeadRecall SHALL NOT 開啟 GUI

### Requirement: Server-authoritative operations

系統 SHALL 在每次板手或 GUI 操作時，於伺服器重新驗證銅傀儡 UUID、玩家距離、維度、權限、板手綁定及目前模式。

#### Scenario: Forged golem UUID

- **GIVEN** 玩家板手綁定銅傀儡 A
- **WHEN** 客戶端傳送修改銅傀儡 B 的 payload
- **THEN** 伺服器 SHALL 拒絕操作
- **AND** 銅傀儡 B 的狀態 SHALL NOT 改變

#### Scenario: Bound golem is in another dimension

- **GIVEN** 板手綁定的銅傀儡位於另一維度
- **WHEN** 玩家嘗試修改區域、容器或模式資料
- **THEN** 伺服器 SHALL 拒絕操作
- **AND** SHALL 保留板手 UUID

### Requirement: Operation modes

每隻銅傀儡 SHALL 具有且僅具有一個目前模式：`SORTING` 或 `GATHERING`。未含 mode 的既有銅傀儡 SHALL 預設為 `SORTING`。

#### Scenario: Existing golem migration

- **GIVEN** 舊世界中的銅傀儡沒有 mode
- **WHEN** 新版本首次讀取該銅傀儡
- **THEN** mode SHALL 設為 `SORTING`
- **AND** 現有箱子綁定、燃料、LLM 與來源資料 SHALL 被保留

### Requirement: Shared source copper chest

每隻銅傀儡 SHALL 最多綁定一個來源銅箱。此來源銅箱在 `SORTING` 模式下作為取出待分類物品的來源，在 `GATHERING` 模式下作為採集物存放的 Home。銅箱綁定 SHALL 與一般容器綁定使用相同板手流程，但伺服器 SHALL 依方塊是否為銅箱決定寫入來源銅箱或分類目的地。

#### Scenario: Bind source copper chest

- **GIVEN** 板手綁定有效銅傀儡
- **WHEN** 玩家右鍵一個有效銅箱容器
- **THEN** 該銅箱 SHALL 成為此銅傀儡的來源銅箱
- **AND** 若原本已有來源銅箱，SHALL 被新銅箱取代
- **AND** 分類目的地清單 SHALL NOT 新增該銅箱

#### Scenario: Only one source copper chest

- **GIVEN** 銅傀儡已綁定來源銅箱 A
- **WHEN** 玩家右鍵來源銅箱 B
- **THEN** 來源銅箱 SHALL 改為 B
- **AND** A SHALL 不再是該銅傀儡的來源銅箱

#### Scenario: Remove source copper chest

- **GIVEN** 銅箱已綁定為來源銅箱
- **WHEN** 玩家以已綁定板手左鍵該銅箱
- **THEN** 來源銅箱綁定 SHALL 被清除

#### Scenario: Source copper chest removed

- **GIVEN** 來源銅箱所在區塊已載入
- **WHEN** 來源銅箱方塊被破壞或不再是有效銅箱容器
- **THEN** 來源銅箱綁定 SHALL 被刪除

### Requirement: Separate running and mode

系統 SHALL 將工作模式與運作開關分開保存。切換 running SHALL NOT 自動切換模式，切換模式 SHALL NOT 自動啟動銅傀儡。

#### Scenario: Change mode while stopped

- **GIVEN** 銅傀儡已停止
- **WHEN** 模式成功從 SORTING 切換為 GATHERING
- **THEN** 銅傀儡 SHALL 保持停止
- **AND** 只有玩家明確啟動後才能開始採集

### Requirement: Sorting-to-gathering transition guard

系統 SHALL 只在銅傀儡已停止、分類主手貨物為空，且沒有未完成返回或存放作業時，允許從 `SORTING` 切換至 `GATHERING`。

#### Scenario: Reject switch while carrying cargo

- **GIVEN** 銅傀儡處於 SORTING
- **AND** 主手持有分類貨物
- **WHEN** 玩家要求切換至 GATHERING
- **THEN** 伺服器 SHALL 拒絕切換
- **AND** mode SHALL 保持 SORTING
- **AND** GUI SHALL 顯示貨物尚未清空

#### Scenario: Preserve sorting configuration

- **GIVEN** 分類主手與進行中工作已清空
- **WHEN** 模式成功切換至 GATHERING
- **THEN** 分類箱綁定、分類 Prompt 與分類 LLM 快取 SHALL 被保留

### Requirement: Gathering-to-sorting transition guard

系統 SHALL 只在銅傀儡已停止、工具欄為空、採集倉庫為空，且沒有進行中的破壞、返回或存放作業時，允許從 `GATHERING` 切換至 `SORTING`。

#### Scenario: Reject switch while tool remains

- **GIVEN** 銅傀儡處於 GATHERING
- **AND** 工具欄非空
- **WHEN** 玩家要求切換至 SORTING
- **THEN** 伺服器 SHALL 拒絕切換
- **AND** 工具 SHALL 保持原狀

#### Scenario: Reject switch while storage remains

- **GIVEN** 採集倉庫仍有物品
- **WHEN** 玩家要求切換至 SORTING
- **THEN** 伺服器 SHALL 拒絕切換
- **AND** 倉庫物品 SHALL NOT 被移動、刪除或掉落

#### Scenario: Preserve gathering configuration

- **GIVEN** 工具欄與倉庫已清空且工作已停止
- **WHEN** 模式成功切換至 SORTING
- **THEN** 工作區、Home、手動規則、採集 Prompt 與 LLM 快取 SHALL 被保留

### Requirement: Sorting destination binding

在 `SORTING` 模式下，玩家 SHALL 能使用已綁定板手右鍵一般非銅箱容器以新增分類目的地，並左鍵已綁定容器以解除目的地。銅箱 SHALL 被解讀為來源銅箱綁定，不得加入分類目的地。

#### Scenario: Add a destination

- **GIVEN** 板手綁定有效銅傀儡
- **AND** 銅傀儡處於 SORTING
- **WHEN** 玩家右鍵有效且非銅箱的容器
- **THEN** 容器 SHALL 被加入分類目的地
- **AND** 重複加入 SHALL NOT 建立重複項目

#### Scenario: Remove a destination

- **GIVEN** 容器已綁定為分類目的地
- **WHEN** 玩家以已綁定板手左鍵該容器
- **THEN** 該容器 SHALL 從目的地移除

#### Scenario: Copper chest becomes source instead of destination

- **GIVEN** 銅傀儡處於 SORTING
- **WHEN** 玩家右鍵銅箱
- **THEN** 該銅箱 SHALL 成為來源銅箱
- **AND** 分類目的地清單 SHALL NOT 包含該銅箱

### Requirement: Sorting source requirement

分類模式 SHALL 只從已綁定來源銅箱取出物品；沒有來源銅箱或來源銅箱不可用時，銅傀儡 SHALL 不進行分類搬運。

#### Scenario: Sorting without source

- **GIVEN** 銅傀儡處於 SORTING
- **AND** 尚未綁定來源銅箱
- **WHEN** running 為 true
- **THEN** 銅傀儡 SHALL 不從其他銅箱或容器取出物品
- **AND** activity SHALL 顯示缺少來源銅箱

### Requirement: Sorting transport limit

分類模式每次 SHALL 最多從來源取出 16 個相同 ItemStack，並 SHALL 保存來源 dimension、位置與 slot。

#### Scenario: Source stack exceeds 16

- **GIVEN** 來源 slot 有 64 個物品
- **WHEN** 銅傀儡開始一次分類搬運
- **THEN** 銅傀儡 SHALL 最多取出 16 個
- **AND** 來源資訊 SHALL 被保存

### Requirement: Sorting destination selection

分類模式 SHALL 依相同物品可合併、DeadRecall 背包可合併、LLM 已允許且有空間的順序搜尋目的地。

#### Scenario: Existing matching stack

- **GIVEN** 已綁定容器內已有相同 Item＋Components 且未滿
- **WHEN** 銅傀儡尋找目的地
- **THEN** 該容器 SHALL 可成為目的地而不需要 LLM

#### Scenario: Shared reject cache overrides matching stack

- **GIVEN** 某 item id 或 tag id 位於該容器拒絕快取
- **AND** 容器內已有相同 Item＋Components
- **WHEN** 銅傀儡評估該容器
- **THEN** 該容器 SHALL NOT 成為該物品的目的地

#### Scenario: Shared allow cache can use empty slot

- **GIVEN** 某 item id 或 tag id 位於該容器接受快取
- **AND** 容器內沒有相同 Item＋Components
- **AND** 容器有可放入的空 slot
- **WHEN** 銅傀儡評估該容器
- **THEN** 該容器 SHALL 可成為該物品的目的地

#### Scenario: Unknown LLM decision

- **GIVEN** 容器 Prompt 已啟用
- **AND** 該物品沒有快取
- **WHEN** 銅傀儡評估該容器
- **THEN** 系統 SHALL 非同步提出一次分類請求
- **AND** 本輪 SHALL NOT 在未知結果前放入物品

#### Scenario: Successful destination deposit clears source memory

- **GIVEN** 銅傀儡持有從來源銅箱取出的分類貨物
- **AND** 目前目的容器可以接收至少部分貨物
- **WHEN** 銅傀儡執行放入目的容器
- **THEN** 系統 SHALL 直接寫入目的容器或目的容器內的一般背包
- **AND** 銅傀儡手上物品 SHALL 更新為未放入的剩餘數量
- **AND** 若貨物已全部放入，來源記憶 SHALL 被清除
- **AND** 已成功放入目的地的貨物 SHALL NOT 被再次放回來源銅箱

### Requirement: Sorting return safety

找不到可用目的地時，系統 SHALL 嘗試將分類貨物完整放回來源容器，且 SHALL NOT 靜默刪除或丟棄貨物。

#### Scenario: No destination accepts cargo

- **GIVEN** 銅傀儡持有分類貨物
- **AND** 所有目的地皆不可用或拒絕
- **WHEN** 分類流程結束
- **THEN** 系統 SHALL 嘗試返回來源 slot
- **AND** 原 slot 不可用時 SHALL 嘗試來源容器其他合法位置
- **AND** 仍無法放回時 SHALL 保持持有並進入阻塞狀態

### Requirement: Sorting blocked behavior

當來源容器有物品但沒有任何可分類項目時，系統 SHALL 停止移動並呈現 blocked 狀態，直到來源內容、綁定或目的容器內容改變。

#### Scenario: All source items are unsortable

- **GIVEN** 來源銅箱有物品
- **AND** 所有物品皆無有效目的地
- **WHEN** 銅傀儡評估來源
- **THEN** 銅傀儡 SHALL 進入 sorting blocked
- **AND** SHALL NOT 重複取出同一批無法處理的物品

### Requirement: Sorting mode isolation

原版搬運 Mixin SHALL 只在 `SORTING` 模式執行 DeadRecall 自訂分類行為。

#### Scenario: Gathering tool is not cargo

- **GIVEN** 銅傀儡處於 GATHERING
- **AND** 工具欄含有工具
- **WHEN** 原版搬運 AI tick
- **THEN** DeadRecall 分類 Mixin SHALL NOT 將該工具視為分類貨物

### Requirement: Gathering work area

在 `GATHERING` 模式下，玩家 SHALL 能使用右鍵設定 Corner A，使用 Shift＋右鍵設定 Corner B。工作區 SHALL 為兩角形成的同維度封閉長方體。

#### Scenario: Configure two corners

- **GIVEN** 板手綁定有效且處於 GATHERING 的銅傀儡
- **WHEN** 玩家右鍵方塊 A 並 Shift＋右鍵方塊 B
- **THEN** 系統 SHALL 保存兩角
- **AND** SHALL 正規化 min/max 座標

#### Scenario: Reject oversized area

- **GIVEN** Corner A 已設定
- **WHEN** Corner B 會使區域超過伺服器上限
- **THEN** 伺服器 SHALL 拒絕 Corner B
- **AND** 原有效區域 SHALL 保持不變

#### Scenario: Unloaded chunks are skipped

- **GIVEN** 工作區包含未載入區塊
- **WHEN** scanner 搜尋目標
- **THEN** 系統 SHALL 跳過未載入區塊
- **AND** SHALL NOT 因掃描而載入區塊

### Requirement: Gathering source copper chest

採集模式 SHALL 要求一個有效來源銅箱作為 Home，且右鍵或 Shift＋右鍵銅箱 SHALL 優先設定來源銅箱，而非設定工作區角點。

#### Scenario: Set source chest as home

- **GIVEN** 銅傀儡處於 GATHERING
- **WHEN** 玩家以已綁定板手右鍵銅箱
- **THEN** 該銅箱 SHALL 成為來源銅箱與採集 Home
- **AND** Corner A SHALL NOT 改變

#### Scenario: Source home is removed

- **GIVEN** 採集倉庫非空
- **AND** 來源銅箱被移除
- **WHEN** 銅傀儡準備存放
- **THEN** 倉庫 SHALL 保持不變
- **AND** activity SHALL 變為 `BLOCKED_HOME_UNAVAILABLE`

### Requirement: Gathering tool slot

採集模式 SHALL 提供一個伺服器權威工具欄，並完整保存工具 ItemStack Components、附魔與耐久。

#### Scenario: Insert a tool

- **GIVEN** 玩家主手或 inventory 內有合法工具
- **WHEN** 玩家將工具放入工具欄
- **THEN** 伺服器 SHALL 優先使用主手合法工具，否則從玩家真實 inventory 搜尋第一個合法工具並移動該工具
- **AND** 客戶端 SHALL NOT 能以 payload 創造工具副本

#### Scenario: Invalid tool

- **GIVEN** 玩家主手與 inventory 內皆沒有合法採集工具
- **WHEN** 玩家嘗試插入工具欄
- **THEN** 伺服器 SHALL 拒絕
- **AND** 玩家物品 SHALL 保持不變

#### Scenario: Tool breaks

- **GIVEN** 工具剩餘耐久不足
- **WHEN** 一次合法採集使工具損壞
- **THEN** 工具 SHALL 依原版規則損壞
- **AND** 採集 SHALL 停止
- **AND** activity SHALL 顯示工具已損壞

### Requirement: Gathering storage slot

採集模式 SHALL 提供一個採集倉庫 ItemStack，且數量 SHALL 不超過 16。

#### Scenario: Store matching drops

- **GIVEN** 倉庫已有 10 個物品 A
- **AND** 新掉落為 4 個與 A 相同 Item＋Components 的物品
- **WHEN** 採集成功
- **THEN** 倉庫 SHALL 變為 14 個 A
- **AND** 銅傀儡 SHALL 繼續搜尋可合併的採集目標而非立即返回 Home

#### Scenario: Continue until storage is full

- **GIVEN** 採集倉庫已有未滿 16 個物品 A
- **AND** 工作區仍有可採集且掉落 A 的目標
- **WHEN** 銅傀儡完成一次採集
- **THEN** 銅傀儡 SHALL 繼續採集可合併目標
- **AND** SHALL NOT 在倉庫未滿且仍有目標時返回 Home

#### Scenario: Reject overflow

- **GIVEN** 倉庫已有 14 個物品 A
- **AND** 候選方塊預期掉落 3 個 A
- **WHEN** 銅傀儡評估該方塊
- **THEN** 該方塊 SHALL NOT 被破壞

#### Scenario: Reject mismatched drop

- **GIVEN** 倉庫已有物品 A
- **AND** 候選方塊預期掉落物品 B
- **WHEN** 銅傀儡評估該方塊
- **THEN** 該方塊 SHALL NOT 被破壞

#### Scenario: Reject mixed drops

- **GIVEN** 倉庫為空
- **AND** 候選方塊預期產生兩種不同 ItemStack
- **WHEN** 銅傀儡評估該方塊
- **THEN** 該方塊 SHALL NOT 被破壞

### Requirement: Manual gathering targets

玩家 SHALL 能在 GATHERING 模式下以板手左鍵普通方塊，切換該 Block ID 的手動允許狀態，且此操作 SHALL 取消原版方塊攻擊。

#### Scenario: Add manual target

- **GIVEN** Block ID 尚未手動允許
- **WHEN** 玩家以已綁定板手左鍵該方塊
- **THEN** Block ID SHALL 加入手動允許列表
- **AND** 方塊 SHALL NOT 被玩家破壞

#### Scenario: Remove manual target

- **GIVEN** Block ID 已在手動允許列表
- **WHEN** 玩家再次左鍵相同 Block ID
- **THEN** Block ID SHALL 從手動允許列表移除

#### Scenario: Manual target appears in accepted list

- **GIVEN** Block ID 已在手動允許列表
- **WHEN** GUI 顯示採集目標
- **THEN** 該 Block ID SHALL 顯示在接受目標欄位

#### Scenario: Remove gathering target from GUI

- **GIVEN** GUI 顯示採集接受目標或拒絕目標圖示
- **WHEN** 玩家右鍵該圖示
- **THEN** 該手動目標或 Prompt 快取 SHALL 從對應清單移除
- **AND** 採集搜尋 SHALL 重新計算

### Requirement: Gathering target rule priority

方塊目標判斷 SHALL 依永久安全拒絕、手動拒絕、手動允許、LLM Block ID、LLM Tag、未知 LLM 請求的順序執行。

#### Scenario: Manual allow overrides LLM deny

- **GIVEN** Block ID 被手動允許
- **AND** 舊 LLM 快取為拒絕
- **WHEN** scanner 評估該方塊
- **THEN** 手動允許 SHALL 優先
- **AND** 永久安全規則仍 SHALL 適用

#### Scenario: Permanent safety deny overrides manual allow

- **GIVEN** 容器方塊被手動允許
- **WHEN** scanner 評估該位置
- **THEN** 系統 SHALL 拒絕採集

### Requirement: LLM gathering targets

採集 Prompt 啟用時，系統 SHALL 以 Block ID、名稱、Block Tags、預期掉落與工具需求摘要分類方塊類型，並 SHALL 快取結果。

#### Scenario: Classify an unknown block type

- **GIVEN** 方塊不在手動規則或 LLM 快取中
- **AND** 採集 Prompt 與 LLM 設定有效
- **WHEN** scanner 遇到該 Block ID
- **THEN** 系統 SHALL 至多建立一個 pending 請求
- **AND** 本輪 SHALL 跳過該方塊直到取得結果

#### Scenario: Warm up gathering cache after area is complete

- **GIVEN** 銅傀儡處於 GATHERING 模式且停止運作
- **AND** 採集 Prompt 與 LLM API 設定有效
- **AND** 採集工作區兩個角點已完成
- **AND** 工具欄有工具
- **WHEN** 伺服器 tick 掃描工作區
- **THEN** 系統 SHALL 對未快取且工具可合法取得掉落的方塊送出採集 LLM 判定
- **AND** LLM 結果 SHALL 寫入採集接受或拒絕快取

#### Scenario: Tool-gated gathering cache

- **GIVEN** 採集工作區內有工具等級不足或無法取得合法掉落的方塊
- **WHEN** 採集快取預熱或 scanner 評估該方塊
- **THEN** 系統 SHALL NOT 對該方塊送出 LLM 判定
- **AND** SHALL NOT 因目前工具不足將該方塊寫入拒絕快取

#### Scenario: Prompt changes

- **GIVEN** 採集 LLM 快取已有資料
- **WHEN** 玩家修改採集 Prompt
- **THEN** 採集 LLM 快取 SHALL 被清除
- **AND** 手動規則 SHALL 被保留

#### Scenario: Stale asynchronous response

- **GIVEN** LLM 請求使用 Prompt revision 4
- **AND** 玩家已修改至 revision 5
- **WHEN** revision 4 回應返回
- **THEN** 系統 SHALL 丟棄該回應
- **AND** SHALL NOT 修改 revision 5 快取

### Requirement: Gathering target safety

即使方塊由手動規則或 LLM 允許，系統仍 SHALL 拒絕不可破壞方塊、流體、容器、Home、分類綁定容器、未載入方塊及伺服器保護拒絕的位置。

#### Scenario: LLM allows a protected block

- **GIVEN** LLM 快取允許某 Block ID
- **AND** 伺服器保護拒絕該位置
- **WHEN** 銅傀儡嘗試採集
- **THEN** 方塊 SHALL 保持不變
- **AND** 工具、燃料與倉庫 SHALL 不變

### Requirement: Incremental target scanning

scanner SHALL 使用固定每 tick budget 增量掃描已載入工作區，不得在單一 tick 掃描完整大型區域。scanner SHALL 每 tick 最多檢查 512 個候選方塊。scanner SHALL 從工作區 `maxY` 開始由上往下尋找有效目標，避免先採下層造成上層資源懸空或漏採。

#### Scenario: Large valid area

- **GIVEN** 工作區包含大量方塊
- **WHEN** 銅傀儡搜尋目標
- **THEN** 每 tick 檢查數 SHALL 不超過設定 budget
- **AND** scan cursor SHALL 在後續 tick 繼續

#### Scenario: Successful gathering preserves cursor

- **GIVEN** scanner 已選定採集目標 P
- **AND** 銅傀儡成功採集 P
- **WHEN** 銅傀儡搜尋下一個採集目標
- **THEN** scanner SHALL 從 P 之後的 cursor 繼續
- **AND** scanner SHALL NOT 每次成功採集後從工作區 `maxY` 重新開始掃描

#### Scenario: Highest valid layer is preferred

- **GIVEN** 工作區內不同高度同時有多個有效採集目標
- **WHEN** 銅傀儡搜尋下一個採集目標
- **THEN** 銅傀儡 SHALL 優先選擇 Y 較高的有效目標
- **AND** 不應只因下層目標較近而先採下層

#### Scenario: Upward range target can be mined

- **GIVEN** 有效採集目標位於銅傀儡頭頂上方有限採集範圍內
- **AND** 目標下方或周圍有可站位置
- **WHEN** 銅傀儡移動到可站位置
- **THEN** 銅傀儡 SHALL 能從下方或側面採集該目標
- **AND** 頭頂上方有限採集範圍外的方塊 SHALL NOT 僅因位於上方而可被採集

#### Scenario: Standing space uses golem collision box

- **GIVEN** 採集站位腳下有穩固支撐
- **AND** 銅傀儡本身碰撞箱在該站位沒有碰撞
- **WHEN** 系統評估該站位是否可用
- **THEN** 該站位 SHALL 可用
- **AND** 系統 SHALL NOT 要求該站位上方固定保留玩家兩格高空間

#### Scenario: Downward range target can be mined

- **GIVEN** 有效採集目標位於銅傀儡腳下方有限採集範圍內
- **AND** 目標上方有可站位置
- **WHEN** 銅傀儡移動到目標上方
- **THEN** 銅傀儡 SHALL 能從上方往下採集該目標
- **AND** 站位選擇 SHALL 優先嘗試目標上方可站位置，再嘗試側面或下方位置

#### Scenario: Full scan finds no target

- **GIVEN** scanner 已檢查完整工作區且無有效目標
- **WHEN** 掃描結束
- **THEN** activity SHALL 變為 `BLOCKED_NO_VALID_TARGET`
- **AND** 後續 SHALL 使用低頻重試

#### Scenario: Partial storage returns after no target

- **GIVEN** 採集倉庫已有未滿 16 個物品
- **AND** scanner 已檢查完整工作區且沒有可繼續合併的目標
- **WHEN** 掃描結束
- **THEN** 銅傀儡 SHALL 返回 Home 存放目前倉庫內容

#### Scenario: Partial storage return is not interrupted

- **GIVEN** 採集倉庫已有未滿 16 個物品
- **AND** 銅傀儡已因找不到更多目標而開始返回 Home
- **WHEN** 下一個伺服器 tick 執行
- **THEN** 銅傀儡 SHALL 繼續返回或存放倉庫內容
- **AND** SHALL NOT 因倉庫未滿 16 個而重新掃描工作區
- **AND** 主手顯示物 SHALL NOT 在採集工具與採集物之間反覆切換

### Requirement: Atomic gathering break

採集破壞 SHALL 在修改世界前重新驗證方塊、區域、工具、掉落容量、權限及安全規則，並以不可部分提交的方式更新世界、工具、燃料及倉庫。

#### Scenario: Block changes before arrival

- **GIVEN** scanner 選定位置 P
- **AND** 玩家在銅傀儡抵達前替換 P 的方塊
- **WHEN** 銅傀儡準備破壞
- **THEN** 系統 SHALL 重新驗證新的 BlockState
- **AND** 不符合規則時 SHALL 取消破壞

#### Scenario: Successful break

- **GIVEN** 所有驗證通過
- **WHEN** 銅傀儡完成一次採集
- **THEN** 方塊 SHALL 被合法破壞
- **AND** 掉落 SHALL 完整加入倉庫
- **AND** 工具耐久與燃料 SHALL 各扣除一次

#### Scenario: Failed validation changes nothing

- **GIVEN** 任一破壞前驗證失敗
- **WHEN** 交易被取消
- **THEN** 世界、工具、燃料與倉庫 SHALL 全部保持不變

### Requirement: Gathering break speed

銅傀儡採集方塊 SHALL 依方塊硬度與工具挖掘速度累積工作進度，不得抵達後立即破壞所有方塊。

#### Scenario: Tool speed controls break time

- **GIVEN** 銅傀儡工具欄有可採集工具
- **WHEN** 銅傀儡抵達目標方塊
- **THEN** 方塊破壞所需 tick SHALL 依該工具對該 BlockState 的挖掘速度與採集相關附魔計算
- **AND** 銅傀儡實際採集效率 SHALL 為同工具玩家效率的 50%
- **AND** 銅傀儡 SHALL 只在一般水平採集距離約 2 格內、頭頂上方有限採集範圍內，或腳下方有限採集範圍內時開始破壞
- **AND** 非瞬間破壞方塊 SHALL 顯示短暫可見的破壞裂痕進度
- **AND** 只有進度完成後才 SHALL 破壞方塊、扣燃料與依附魔扣工具耐久

#### Scenario: Gathering display item cannot be duplicated

- **GIVEN** 銅傀儡處於 GATHERING 模式
- **AND** 主手顯示採集工具或採集倉庫物品
- **WHEN** 玩家空手右鍵該銅傀儡
- **THEN** 伺服器 SHALL 阻止原版銅傀儡丟出該顯示物
- **AND** 工具與採集倉庫 SHALL 只能透過 GUI 欄位取回

#### Scenario: Tool render is only for active mining

- **GIVEN** 銅傀儡處於 GATHERING 模式
- **WHEN** 銅傀儡搜尋或走向採集目標
- **THEN** 主手 SHALL 不顯示採集工具
- **WHEN** 銅傀儡正在破壞目標方塊
- **THEN** 主手 SHALL 顯示採集工具並播放揮動
- **WHEN** 銅傀儡返回 Home 存放採集物
- **THEN** 主手 SHALL 顯示採集倉庫物品

### Requirement: Gathering deposit

倉庫滿、找不到更多目標或工作需要結束時，銅傀儡 SHALL 返回來源銅箱並將倉庫完整存入銅箱。

#### Scenario: Deposit into matching stack

- **GIVEN** 來源銅箱有相同 Item＋Components 的未滿 stack
- **WHEN** 銅傀儡存放倉庫
- **THEN** 物品 SHALL 優先合併
- **AND** 完整存放後倉庫 SHALL 清空

#### Scenario: Deposit into empty slot

- **GIVEN** 來源銅箱沒有可合併 stack 但有合法空 slot
- **WHEN** 銅傀儡存放倉庫
- **THEN** 倉庫物品 SHALL 放入空 slot
- **AND** 倉庫 SHALL 清空

#### Scenario: Home is full

- **GIVEN** 來源銅箱無法接收倉庫物品
- **WHEN** 銅傀儡嘗試存放
- **THEN** 倉庫 SHALL 保持不變
- **AND** activity SHALL 變為 `BLOCKED_HOME_FULL`
- **AND** 系統 SHALL NOT 將物品丟到地上

### Requirement: Common fuel

SORTING 與 GATHERING SHALL 共用燃料欄與 fuel ticks，且只有成功取出分類貨物或成功破壞採集方塊時才消耗工作燃料。

#### Scenario: Insert fuel from inventory

- **GIVEN** 玩家主手或 inventory 內有可加入燃料欄的燃料
- **WHEN** 玩家點擊 GUI 燃料欄的放入操作
- **THEN** 伺服器 SHALL 優先使用主手合法燃料，否則從玩家真實 inventory 搜尋第一個合法燃料
- **AND** 已有燃料時 SHALL 只接受相同 Item＋Components 的燃料
- **AND** 客戶端 SHALL NOT 能以 payload 創造燃料副本

#### Scenario: Failed path consumes no fuel

- **GIVEN** 銅傀儡無法抵達候選目標
- **WHEN** 路徑失敗
- **THEN** fuel ticks SHALL 不變
- **AND** 工具耐久與採集倉庫 SHALL 不變
- **AND** 該目標 SHALL 被暫時跳過
- **AND** 銅傀儡 SHALL 繼續搜尋下一個有效目標

#### Scenario: No fuel preserves state

- **GIVEN** 銅傀儡沒有燃料
- **WHEN** 模式工作 tick 執行
- **THEN** 工具、倉庫及分類貨物 SHALL 保持不變
- **AND** activity SHALL 顯示無燃料

### Requirement: GUI state synchronization

GUI SHALL 顯示伺服器權威的 mode、running、activity、燃料、分類資料、採集資料與權限，並使用 revision 防止舊 payload 覆寫新狀態。

#### Scenario: World configuration does not open GUI

- **GIVEN** 玩家手持已綁定板手
- **WHEN** 玩家在世界中設定採集 Corner 或切換手動採集目標
- **THEN** 系統 SHALL 顯示操作提示
- **AND** SHALL NOT 強制開啟或跳出管理 GUI

#### Scenario: Gathering prompt and targets share preview list

- **GIVEN** 採集模式 GUI 顯示採集目標
- **WHEN** 手動目標與採集 LLM 快取同時存在
- **THEN** GUI SHALL 使用同一個採集目標預覽列表顯示手動目標與 Prompt 快取
- **AND** 只有手動目標列 SHALL 顯示刪除操作

#### Scenario: Same-dimension containers hide dimension text

- **GIVEN** 來源銅箱與分類目的箱只允許綁定同維度容器
- **WHEN** GUI 顯示來源銅箱、分類目的箱卡片或其 tooltip
- **THEN** GUI SHALL 顯示容器名稱、座標與狀態
- **AND** GUI SHALL NOT 顯示容器維度文字

#### Scenario: Source chest icon keeps details in tooltip

- **GIVEN** GUI 顯示燃料槽旁的來源銅箱 icon
- **WHEN** 來源銅箱已綁定
- **THEN** icon SHALL 只直接顯示來源銅箱圖示
- **AND** 座標、方塊 ID、載入狀態與可用狀態 SHALL 只在 tooltip 中顯示

#### Scenario: Target container cards keep details in tooltip

- **GIVEN** GUI 顯示分類目的箱清單
- **WHEN** 目的箱卡片未被選取
- **THEN** 卡片 SHALL 以精簡高度顯示，並只直接顯示目的箱圖示與名稱
- **AND** 座標、方塊 ID、載入狀態、可用狀態與 LLM 狀態 SHALL 只在 tooltip 中顯示
- **WHEN** 目的箱卡片被選取
- **THEN** 卡片 SHALL 展開以顯示 LLM 開關與接受 / 拒絕快取
- **AND** 只有該目的箱 LLM 開啟時，卡片 SHALL 顯示 Prompt 輸入框與儲存按鈕

#### Scenario: Prompt editors live with target cards

- **GIVEN** GUI 顯示箱子分頁
- **WHEN** 玩家查看 SORTING 或 GATHERING 模式設定
- **THEN** SORTING 模式的分類 Prompt 輸入框與儲存按鈕 SHALL 只在目前選取的目標容器 LLM 啟用時顯示於該卡片內
- **AND** GATHERING 模式的採集 Prompt 輸入框與儲存按鈕 SHALL 只在採集 LLM 啟用時顯示於採集目標區塊內
- **AND** Prompt 輸入框 SHALL NOT 顯示在上方固定設定列

#### Scenario: Source icon stays beside fuel slot

- **GIVEN** GUI 顯示箱子分頁
- **WHEN** 玩家查看 SORTING 或 GATHERING 模式設定
- **THEN** 來源銅箱 SHALL 以單一 icon 顯示於燃料槽旁
- **AND** GUI SHALL NOT 顯示固定的 LLM / Prompt 摘要卡片
- **AND** 下方主要清單 SHALL 直接從 header 下方開始顯示

#### Scenario: Native player inventory UI is deferred

- **GIVEN** 目前銅傀儡管理 GUI 仍為一般 `Screen`
- **WHEN** GUI 顯示箱子分頁
- **THEN** GUI SHALL NOT 顯示可互動的玩家背包 slot 或暫時性的背包預覽
- **AND** 未來若要使用原版背包 UI，SHALL 以 `Menu` / `Slot` / `AbstractContainerScreen` 的容器型 GUI 大改實作
- **AND** 該大改 SHALL 同時處理滑鼠持有物、Shift 點擊、關閉歸還、伺服器同步與權限驗證

#### Scenario: Sorting manual and LLM accept preview share cache row

- **GIVEN** 分類模式 GUI 顯示綁定目的箱
- **WHEN** 目的箱內已有可用傳統分類規則命中的物品或包包內容
- **THEN** 接受預覽 SHALL 以物品圖示顯示手動可分類命中的物品
- **AND** 接受預覽 SHALL 同時合併允許快取
- **AND** 接受預覽 SHALL 以命名牌圖示顯示允許 Tag 快取
- **AND** 拒絕預覽 SHALL 顯示拒絕物品快取與拒絕 Tag 快取
- **AND** 拒絕 Tag 快取 SHALL 以命名牌圖示顯示
- **AND** Prompt 區塊 SHALL NOT 顯示接受或拒絕快取

#### Scenario: Right-click cache icon moves to opposite side

- **GIVEN** 分類模式 GUI 顯示綁定目的箱快取圖示
- **WHEN** 玩家右鍵接受側的 item id 或 tag id 圖示
- **THEN** 該值 SHALL 從接受快取移到拒絕快取
- **WHEN** 玩家右鍵拒絕側的 item id 或 tag id 圖示
- **THEN** 該值 SHALL 從拒絕快取移到接受快取
- **AND** 修改 SHALL 寫回銅傀儡資料並刷新 GUI

#### Scenario: Mode switch rejected

- **GIVEN** 客戶端要求切換模式但伺服器驗證失敗
- **WHEN** 伺服器回應
- **THEN** 客戶端 SHALL 保持伺服器模式
- **AND** SHALL 顯示拒絕原因

### Requirement: Client visualization

玩家手持已綁定板手且與銅傀儡同維度時，客戶端 SHALL 自動顯示目前模式設定，不需要 Shift＋左鍵觸發。

#### Scenario: Sorting visualization

- **GIVEN** 銅傀儡處於 SORTING
- **WHEN** 玩家手持綁定該銅傀儡的板手
- **THEN** 客戶端 SHALL 顯示分類目的箱連線與狀態

#### Scenario: Gathering visualization

- **GIVEN** 銅傀儡處於 GATHERING
- **WHEN** 玩家手持綁定該銅傀儡的板手
- **THEN** 客戶端 SHALL 顯示工作區線框、角點、Home 與目前目標

### Requirement: Visualization efficiency

持續視覺化 SHALL 由客戶端世界渲染完成；伺服器 SHALL NOT 每 tick 為每條路徑傳送完整粒子序列。

#### Scenario: Hold wrench for multiple ticks

- **GIVEN** 玩家持續手持板手
- **WHEN** 世界經過多個 tick
- **THEN** 客戶端 SHALL 使用已同步資料重繪
- **AND** 伺服器 SHALL NOT 每 tick 重傳完整路徑

### Requirement: Item preservation

工具、採集倉庫及分類主手貨物在模式切換失敗、GUI 關閉、區塊卸載、伺服器重啟或 Home／來源失效時 SHALL 保持可恢復，且 SHALL NOT 複製、刪除或無條件掉落。

#### Scenario: Server restart with stored gathering item

- **GIVEN** 採集倉庫含有物品
- **WHEN** 伺服器儲存並重啟
- **THEN** 倉庫 ItemStack 與 Components SHALL 完整恢復

#### Scenario: GUI closes during inventory action

- **GIVEN** 玩家操作工具欄或倉庫欄
- **WHEN** GUI 在伺服器完成操作前關閉
- **THEN** 物品 SHALL 只存在於一個伺服器權威位置

### Requirement: Entity removal inventory handling

銅傀儡永久死亡或移除時，系統 SHALL 依明確且單次的伺服器流程處理工具、採集倉庫及分類貨物，避免重複掉落。

#### Scenario: Golem dies with gathering inventory

- **GIVEN** 工具欄或採集倉庫非空
- **WHEN** 銅傀儡死亡
- **THEN** 每個 ItemStack SHALL 最多產生一次合法掉落
- **AND** 持久化欄位 SHALL 在掉落提交後清空
