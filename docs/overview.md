# 模組概覽

DeadRecall 是 Minecraft Fabric 模組，整合攜帶式儲存、死亡物品保護、銅魁儡自動化、Discord 聊天橋接與生存煉金系統。

## 主要功能

| 系統 | 用途 |
| --- | --- |
| 多等級背包 | 提供 9、18、27、36 格的可升級攜帶空間 |
| 死亡背包 | 收集玩家死亡後產生的一般掉落物，並在死亡地點生成可取回的背包 |
| `/back` | 記錄死亡維度與座標，支援跨維度返回死亡地點 |
| 銅魁儡 | 透過銅板手管理分類與採集模式，可選配 LLM 判斷 |
| Discord Bridge | 將遊戲聊天、玩家動態、管理稽核、公開事件及伺服器狀態經 Cloudflare Worker 傳送到 Discord |
| Totem Nexus | 使用磁石與羅盤註冊、探索 Space Unit，透過地圖與 Server 驗證進行傳送 |
| 附魔台 | 雕紋書櫃依實際書本與附魔等級提供最高 64 點附魔力 |
| Discord Bridge | 將遊戲聊天及伺服器狀態經 Cloudflare Worker 傳送到 Discord |
| 生存煉金 | 包含豬糞、木灰、硝石、缽與煉藥鍋配方流程 |
| 背包整理 | 預設使用滑鼠中鍵整理目前開啟的物品介面 |

## 玩家入口

- 背包與死亡保護：參閱 [背包系統](backpacks/README.md) 與 [死亡背包與 `/back`](backpacks/death-backpack.md)。
- 銅魁儡：參閱 [銅魁儡使用指南](copper-golem/README.md)。
- Totem Nexus：參閱 [Totem Nexus／Space Unit](nexus/README.md)。
- 附魔台：參閱 [附魔台與雕紋書櫃](enchanting/README.md)。
- 煉金內容：參閱 [煉金系統](alchemy/README.md)。

## 技術基礎

- Minecraft：26.2
- Loader：Fabric
- 必要依賴：Fabric API
- 授權：BSD-3-Clause

版本號與相依版本可能隨發佈更新，請以專案設定檔與 Release／Modrinth 頁面為準。
