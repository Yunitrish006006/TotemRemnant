# Mixin 參考

DeadRecall 使用 Mixin 攔截部分無法單靠事件 API 完成的原版行為。

| Mixin 類型 | 用途 |
| --- | --- |
| Entity below-world handling | 防止死亡背包被虛空直接移除 |
| ItemEntity damage／discard | 依背包等級處理防護與內容物掉落 |
| Pig goal registration | 將豬糞相關 AI 加入豬的目標選擇器 |
| Copper golem transport | 攔截並擴充銅魁儡容器搬運行為 |
| ItemEntity rendering | 為地上的死亡背包加入紅色光柱 |
| Enchantment menu | 使用雕紋書櫃內實際書本力量重新計算三個附魔選項 |
| Enchantment helper | 移除原版 15 書櫃限制，將附魔力與成本上限提高到 64 |
| Enchanting table animation | 讓雕紋書櫃產生依普通書與附魔書力量縮放的附魔粒子 |

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
- client-only rendering Mixin 不得修改伺服器權威資料。