# Client Rendering

DeadRecall 的 client-only 視覺功能包含死亡背包光柱、銅板手粒子提示與自訂 GUI。

## 死亡背包光柱

- 只在 client render path 提交光柱。
- 使用死亡背包 ItemStack／Components 判定，不改變實體資料或拾取邏輯。
- 渲染狀態不得洩漏到其他 ItemEntity，避免一般物品錯用光柱或模型狀態。

## 銅板手提示

手持已綁定銅魁儡的板手時，可顯示：

- 來源銅箱。
- 分類目的地。
- 採集 Corner A／B 與工作區。
- 手動採集目標。
- 無效、阻塞或跨維度狀態。

粒子與覆蓋提示只負責視覺回饋，實際綁定與有效性仍由伺服器判定。

## GUI

- 畫面應依伺服器同步資料重建。
- 切換頁籤或滾動時不得改動伺服器資料。
- API Key 等秘密欄位應遮蔽，且不應出現在 tooltip 或普通除錯輸出。
- Minecraft 升級後應檢查 GUI scale、文字裁切、slot hitbox 與 scissor 區域。