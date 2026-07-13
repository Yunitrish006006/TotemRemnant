# DeadRecall

DeadRecall 是 Minecraft Fabric 26.2 模組，整合死亡物品保護、可升級背包、銅魁儡分類與採集、雕紋書櫃附魔、煉藥鍋配方、Discord Bridge 與跨維度 `/back`。

## 專案資訊

| 項目 | 內容 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3+ |
| Fabric API | 0.154.2+26.2 |
| Java | 25 |
| 授權 | BSD-3-Clause |

版本號與依賴版本以 `gradle.properties`、`fabric.mod.json` 和發佈頁面為準。

## 主要功能

- 四級可升級背包與物品實體防護。
- 死亡背包、紅色定位光柱、永久保存與虛空保護。
- `/back` 跨維度返回最近死亡地點。
- 銅板手管理銅魁儡分類、採集、燃料、工具與 LLM 判斷。
- 雕紋書櫃依實際書本與附魔等級提供最高 64 點附魔力。
- 豬糞、木灰、硝石、缽及資料驅動煉藥鍋配方。
- Minecraft 聊天、玩家動態、管理稽核、公開事件與伺服器狀態轉送到 Discord。

## 文件

完整文件入口：[docs/README.md](docs/README.md)

| 分類 | 文件 |
| --- | --- |
| 發佈 | [Release Notes](docs/releases/README.md) |
| 玩家 | [模組概覽](docs/overview.md) |
| 玩家 | [背包系統](docs/backpacks/README.md) |
| 玩家 | [死亡背包與 `/back`](docs/backpacks/death-backpack.md) |
| 玩家 | [銅魁儡指南](docs/copper-golem/README.md) |
| 玩家 | [附魔台與雕紋書櫃](docs/enchanting/README.md) |
| 玩家 | [煉金系統](docs/alchemy/README.md) |
| 管理員 | [Discord Bridge](docs/discord/README.md) |
| 開發者 | [開發者文件](docs/developer/README.md) |
| 規格 | [OpenSpec 索引](OPENSPEC_INDEX.md) |

## 建置

```bash
./gradlew build
```

正式 JAR 輸出至：

```text
build/libs/
```

## 文件分工

- `README.md`：專案首頁與快速導覽。
- `docs/`：目前可使用功能的玩家、管理員與開發者說明。
- `openspec/specs/`：已採用的系統規格與 invariant。
- `openspec/changes/`：設計中或尚未完成的變更。
