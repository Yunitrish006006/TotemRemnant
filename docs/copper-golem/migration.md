# 銅魁儡資料遷移

本文件記錄 DeadRecall 2.3.0 銅魁儡資料格式與舊世界相容規則。

## 遷移時機

銅魁儡資料仍存放在銅魁儡實體的 `DataComponents.CUSTOM_DATA`。2.3.0 採用 lazy migration：伺服器第一次讀取或操作銅魁儡時，會補齊缺少的版本與模式欄位，不需要玩家手動執行指令。

升級前仍建議備份世界。

## 版本欄位

2.3.0 會寫入：

- `deadrecall_data_version = 2`
- `deadrecall_mode = "sorting"`，只在舊資料沒有 mode 時補上
- `deadrecall_revision = 0`，只在舊資料沒有 revision 時補上

`revision` 用於 GUI 操作防止舊畫面覆寫新狀態。非 slot 設定操作會檢查 revision；燃料、工具與採集倉庫的物品移動由 Menu slot transaction 同步。

## 保留的舊資料

下列舊資料在升級後會保留原值：

| 資料 | 相容行為 |
| --- | --- |
| 板手 `deadrecall_selected_golem` | 保留既有 UUID，已綁定板手仍指向同一隻銅魁儡 |
| `deadrecall_transport_enabled` | 保留運作開關狀態 |
| 來源銅箱座標 | 保留為 shared source copper chest，分類模式作為來源，採集模式作為 Home |
| 分類目的地 | 舊單一目的地與新列表會在首次 lazy migration 時合併，並收斂成列表格式 |
| 分類 LLM API 與容器 Prompt / cache | 保留既有設定與快取 |
| blocked / source slot / tried destinations | 保留分類工作恢復所需狀態 |
| fuel ticks / fuel stack | 保留既有燃料狀態；新的燃料 slot 由同一組 server 權威資料驅動 |

舊世界中的銅魁儡沒有 mode 時會被視為 `SORTING`。因此升級後既有分類箱、來源銅箱、LLM 設定與正在處理的分類狀態不會被改成採集模式資料。

## 新增資料

2.3.0 會新增採集模式資料：

- 工作區 dimension 與 Corner A / Corner B。
- 手動採集目標列表。
- 採集掃描游標、暫時跳過目標與 retry tick。
- 採集工具 slot。
- 採集倉庫 slot，最多 16 個物品。
- 採集 LLM 開關、Prompt、Prompt revision 與允許／拒絕快取。

這些欄位只在玩家設定採集模式或系統執行採集工作後寫入。舊銅魁儡不會在升級時自動生成採集區域、工具或倉庫內容。

## 移除的舊流程

2.3.0 移除舊的燃料／工具／採集倉庫 serverbound payload 與主手自動放入流程。物品進出銅魁儡內部欄位一律透過容器 Menu slot transaction。

這項變更不需要資料轉換，但會改變玩家操作方式：

- 燃料改由 GUI 中的燃料 slot 拖曳或 Shift-click。
- 採集工具改由 GUI 中的工具 slot 拖曳或 Shift-click。
- 採集倉庫只能透過 GUI slot 取回，不允許玩家手動放入。

## 回退注意事項

從 2.3.0 回退到舊版不受支援。舊版不認得 `deadrecall_mode`、採集工作區、採集工具、採集倉庫與採集 LLM 快取等資料；若必須回退，請使用升級前的世界備份。
