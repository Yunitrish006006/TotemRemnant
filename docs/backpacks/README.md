# 背包系統

DeadRecall 提供四級一般背包，以及獨立的死亡背包與整理功能。

## 文件

- [背包等級與防護](tiers.md)
- [死亡背包與 `/back`](death-backpack.md)
- [可攜式容器安全規則](container-safety.md)
- [整理功能](sorting.md)

## 快速說明

一般背包右鍵開啟，內容使用原版 Container Data Component 儲存。背包不可堆疊，並支援 Shift+點擊快速移動。

Bundle、Shulker Box 與設定型可攜式容器不能放入 DeadRecall 背包；DeadRecall 背包也不能放入 Bundle 或 Shulker Box。舊世界已存在的非法巢狀內容仍可取出，但不能重新插入。

死亡背包會在原版生成世界掉落物之前，直接從玩家的 Server 權威物品欄與裝備資料封裝一般物品；DeadRecall 背包與其他可攜式容器本身維持獨立掉落。死亡背包具備永久保存、紅色光柱與虛空保護。
