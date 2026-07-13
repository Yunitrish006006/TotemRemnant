# Change: Copper Golem Operation Modes

## Why

DeadRecall 現有銅傀儡以箱子分類為核心，透過銅板手綁定目的容器、燃料與 LLM 物品分類完成搬運。現有互動仍為「左鍵選擇銅傀儡、Shift＋右鍵開 GUI」，且尚未建立正式的工作模式與資料邊界。

本變更將銅傀儡擴充為可切換的工作平台：

1. `SORTING`：保留現有箱子分類模式。
2. `GATHERING`：新增指定區域內的資源採集模式。
3. Shift＋右鍵銅傀儡時，板手直接綁定該銅傀儡並開啟 GUI。
4. 模式切換前由伺服器檢查目前模式的物品欄位及進行中工作是否已清空。
5. 玩家手持已綁定板手時，自動顯示該銅傀儡的模式資訊。

此變更必須避免分類貨物、採集工具與採集暫存物共用同一主手語意，防止物品錯置、複製或遺失。

## What Changes

### 板手互動

- 移除普通左鍵銅傀儡的選擇功能。
- 玩家手持銅板手 Shift＋右鍵銅傀儡時：
  - 板手綁定目標銅傀儡 UUID。
  - 覆寫板手原本的銅傀儡綁定。
  - 立即同步並開啟目標銅傀儡 GUI。
- 移除 Shift＋左鍵銅傀儡顯示路徑的功能。
- 手持已綁定板手時，由客戶端持續顯示對應模式資訊。

### 模式系統

```java
public enum CopperGolemMode {
    SORTING,
    GATHERING
}
```

模式與運作狀態分離：

- `mode`：工作類型。
- `running`：是否允許執行工作。
- `activity`：目前執行階段或阻塞原因。

### 箱子分類模式

保留目前功能：

- 從銅箱取得待分類物品。
- 每次最多搬運 16 個相同物品。
- 將物品送往已綁定的一般容器。
- 依容器既有內容、空間及 LLM 規則決定目的地。
- 找不到目的地時將貨物放回原來源。
- 整箱無物品可分類時進入 blocked 狀態。
- 支援容器內的 DeadRecall 背包。
- 使用共通燃料欄位。

### 資源採集模式

新增：

- 一個工具欄位。
- 一個採集倉庫欄位，單一 ItemStack，最多 16 個物品。
- 一個 Home 銅箱，用於存放採集結果。
- Corner A 與 Corner B 定義的長方體工作區域。
- 板手左鍵方塊切換該 Block ID 的手動採集規則。
- LLM Prompt 指定要採集的資源類型。
- 採集前檢查工具、保護、掉落物及倉庫容量。
- 倉庫滿、沒有更多目標或工作結束時返回 Home。
- Home 滿或失效時保留物品並阻塞，不得丟棄。

### 模式切換

模式切換只能從 GUI 發起，並由伺服器原子驗證。

`SORTING → GATHERING`：

- 銅傀儡必須停止。
- 主手不得有分類貨物。
- 不得有未完成的來源返回或目的存放作業。

`GATHERING → SORTING`：

- 銅傀儡必須停止。
- 工具欄必須為空。
- 採集倉庫必須為空。
- 不得有正在破壞、返回或存放的作業。

以下設定可保留，不要求清除：

- 分類箱綁定、分類 Prompt、分類 LLM 快取。
- 採集工作區、Home、手動方塊規則、採集 Prompt、採集 LLM 快取。
- 共用燃料與 LLM API 設定。

## Impact

### Affected code

- `CopperGolemWrenchHandler`
- `TransportItemsBetweenContainersMixin`
- `CopperGolemLlmService`
- `CopperWrenchBindingsScreen`
- 銅傀儡 networking payloads
- Server tick handler
- Client world renderer
- 語言檔與 GUI

### Compatibility

- 沒有 mode 的既有銅傀儡預設遷移為 `SORTING`。
- 現有 `deadrecall_transport_enabled` 遷移為共通 `running`。
- 現有板手 `deadrecall_selected_golem` UUID 保留，語意改為「板手綁定」。
- 現有箱子綁定、燃料、來源與 LLM 資料不得遺失。

### Risks

- 原版搬運 AI 將採集工具誤認為分類貨物。
- 模式切換時主手、工具欄或倉庫重複寫入。
- 破壞方塊後才發現掉落物不能完整收納。
- 非同步 LLM 回應寫入過期 Prompt 快取。
- 客戶端偽造 UUID、模式、區域或物品操作。
- 大型區域掃描拖慢伺服器 tick。
