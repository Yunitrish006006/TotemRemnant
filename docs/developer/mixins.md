# Mixin 參考

DeadRecall 使用 Mixin 攔截部分無法單靠事件 API 完成的原版行為。

| Mixin 類型 | 用途 |
| --- | --- |
| Entity below-world handling | 防止死亡背包被虛空直接移除 |
| ItemEntity damage／discard | 依背包等級處理防護與內容物掉落 |
| Pig goal registration | 將豬糞相關 AI 加入豬的目標選擇器 |
| Copper golem transport | 攔截並擴充銅魁儡容器搬運行為 |
| ItemEntity rendering | 為地上的死亡背包加入紅色光柱 |
| Structure template | 將結構中的普通書櫃轉成預先帶有物品 NBT 的雕紋書櫃 |
| Enchantment menu | 使用雕紋書櫃內實際書本力量重新計算三個附魔選項 |
| Enchantment helper | 移除原版 15 書櫃限制，將附魔力與成本上限提高到 64 |
| Enchanting table animation | 讓雕紋書櫃產生依普通書與附魔書力量縮放的附魔粒子 |

## 結構生成與雕紋書櫃

`StructureTemplateMixin` 在 `processBlockInfos` 回傳階段處理結構方塊資訊：

- 普通書櫃會轉換成雕紋書櫃。
- 沒有既有 BlockEntity NBT 的雕紋書櫃會填入最多六本普通書。
- 已附帶 NBT 的雕紋書櫃保持原內容，避免覆蓋資料包或結構作者設定。
- 槽位內容使用脫離世界的 `ChiseledBookShelfBlockEntity` 產生目前版本的合法 NBT。
- 所有已填入槽位的 block-state occupied properties 會同步設為 `true`。

不得在 `StructureTemplate.placeInWorld` 的 worldgen 階段對世界中的雕紋書櫃呼叫 `setItem(...)`。該方法會執行 `updateState(...)` 並要求 BlockEntity 已掛接有效 `Level`；結構生成可能仍在 worker thread 或 BlockEntity 尚未完成掛接，會造成 Feature placement 崩潰。

## 附魔系統入口

- `EnchantingPowerHelper`：掃描原版書櫃 offset 上的雕紋書櫃，普通書計 1，附魔書依附魔等級總和計算，總力量上限 64。
- `EnchantmentMenuMixin`：完整取代 `slotsChanged`，使用自訂力量產生成本與提示附魔。
- `EnchantmentHelperMixin`：完整取代 `getEnchantmentCost`，移除 15 書櫃截斷。
- `EnchantingTableBlockMixin`：修改原版粒子來源判斷並增加依書本力量縮放的粒子。

這三個附魔 Mixin 使用 `@Overwrite` 或精確 Redirect／Inject，Minecraft 升版時屬於高風險檢查項目。應驗證原版成本公式、`BOOKSHELF_OFFSETS`、Data Component 名稱與附魔書實際儲存欄位。

## 維護原則

- 優先使用 Fabric event 或公開 API；只有缺乏穩定入口時才使用 Mixin。
- 每個注入點都應記錄目標方法、注入時機與取消原版行為的條件。
- Minecraft 升級時先驗證 method descriptor、mapping 與 locals，不可假設舊注入點仍有效。
- 取消原版流程前要確認替代流程完整處理資料、同步與實體生命週期。
- worldgen worker thread 不得直接呼叫會修改 `Level` 或 BlockEntity 世界狀態的 API。
- client-only rendering Mixin 不得修改伺服器權威資料。