# TotemRemnant

TotemRemnant 是 Totem 系列的背包、死亡物品保護與可攜式容器安全模組。
目前版本為 **0.1.7**，精確搭配 TotemCore **0.4.0**。

## 安裝

將下列 JAR 放入 Client 與 Server 的 `mods/`：

1. Fabric API `0.154.2+26.2`
2. TotemCore `0.4.0`
3. TotemRemnant `0.1.7`

| 項目 | 需求 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3+ |
| Java | 25+ |
| 選配 | Trinkets Updated 4.1.0-beta.2+ |

使用 DeadRecall 2.4.7 整合 JAR 時不要再安裝獨立 TotemRemnant；整合包已
內含相同模組。

## 一般背包教學

所有背包都在鍛造台升級。前三階使用 Bundle 作為 template：

| 結果 | Template | Base | Addition | 容量 |
| --- | --- | --- | --- | ---: |
| 基礎背包 | Bundle | Bundle | 皮革 | 9 |
| 標準背包 | Bundle | 基礎背包 | 鐵錠 | 18 |
| 進階背包 | Bundle | 標準背包 | 鑽石 | 27 |
| 獄髓背包 | 獄髓升級模板 | 進階背包 | 獄髓錠 | 36 |

右鍵背包即可開啟。內容保存於物品 Data Components，支援原版拖曳與
Shift-click。

四級一般背包都可以和任意一個以上的原版染料放進工作台染色；再次加入
不同染料時會沿用原版皮革物品的混色規則。染色合成會保留背包內容、自訂
名稱與其他 Data Components。拿染色背包右鍵裝水煉藥鍋可洗回原本的棕色，
並消耗一層水。死亡背包的紅色專用外觀不可染色。

## 物品 ID 相容遷移

新取得的背包使用 Remnant canonical ID：
`totem:remnant/backpack_basic`、`backpack_standard`、`backpack_advanced`、
`backpack_netherite` 與 `death_backpack`（後四者同樣位於
`totem:remnant/` 路徑）。

Remnant standalone 只註冊上述 canonical ID。安裝 DeadRecall 2.4.7
整合包時，外層相容主機才會解碼 `deadrecall:backpack_*` 與
`deadrecall:death_backpack`；右鍵使用舊背包時會就地換成 canonical
物品，保留內容、名稱、染色與其他 Data Components。系統不會在啟動時
掃描離線玩家或未載入區塊。

| 等級 | 額外防護 |
| --- | --- |
| 基礎 | 無 |
| 標準 | 仙人掌 |
| 進階 | 仙人掌、爆炸 |
| 獄髓 | 火、熔岩、仙人掌、爆炸與自然消失 |

一般背包掉入虛空仍可能遺失；虛空保護只屬於死亡背包。

## 死亡背包

玩家死亡且 `keepInventory=false` 時，Server 會在原版生成掉落物之前
封裝物品欄、裝備、副手、游標、玩家合成格與支援工作站輸入：

- 容量依死亡物品數動態調整，最多 54 格。
- 死亡背包不會自然消失，免疫一般傷害，並在虛空下方被向上救回。
- 地面上的背包會顯示紅色定位光柱。
- DeadRecall 背包與其他可攜式容器維持獨立掉落，不會被巢狀封裝。
- 背包完全清空並關閉後會移除；任何協助回收的玩家都可完成此流程。
- 若 Nexus 已將玩家上次成功傳送使用的介面物品標記為有效靈魂綁定，
  Remnant 會在死亡交易前保留其中一個，並於重生後 exactly once 還原。
  同堆疊其餘物品照常進死亡背包；消失詛咒仍優先。

若同時安裝 TotemNexus，Remnant 會透過 TotemCore 的選配生命週期建立
死亡 Space Unit，保存死亡節點 ↔ 背包 Entity UUID 的雙向關聯。沒有
Nexus 時死亡背包仍可獨立使用。

死亡背包成功建立或回收時，Remnant 會發布 TotemCore 的型別事件。安裝
TotemDiscordBridge 時由 Bridge 自行訂閱並送出通知；Remnant 不直接依賴
Discord，也不需要 DeadRecall 額外安裝 listener。

> `/back` 是 DeadRecall 相容整合包的額外功能，不是 TotemRemnant
> standalone API 的一部分。

## 可攜式容器安全

Remnant 禁止 Bundle、Shulker Box、DeadRecall 背包與
`deadrecall:portable_containers` tag 物品互相非法巢狀。限制涵蓋 GUI、
Shift-click、漏斗、漏斗礦車、投擲器／發射器與相容自動化。

舊世界已存在的非法內容不會被刪除：可以取出，但不能再次放回。

管理員可執行唯讀診斷：

```text
/deadrecall containers scan
/deadrecall containers scan <player>
```

掃描不移動物品、不自動修復資料，也不載入未載入區塊。

## 選配整合

- **TotemNexus**：建立、綁定與回收死亡 Space Unit。
- **TotemDiscordBridge**：透過 TotemCore event bus 接收死亡背包通知。
- **Trinkets Updated**：擷取已驗證的選配飾品 inventory。
- **TotemAutomata**：透過 Remnant 公開 policy 阻止銅魁儡把背包塞入
  不安全容器。

Remnant 不直接依賴 Nexus 或 Automata；所有跨模組行為都必須安全地
在對方不存在時停用。

## 開發與驗證

```bash
./gradlew build
```

正式 JAR 輸出至 `build/libs/`。測試涵蓋死亡擷取／回收、restart、
背包操作、Shulker Box、Hopper、Hopper Minecart、Dropper、容器診斷
與 legacy data。所有權與相容契約見 [EXTRACTION.md](EXTRACTION.md)。

## 驗證狀態

0.1.7 的 Java 25 build 與 28/28 required Fabric GameTests 已通過；完整
整合 Dedicated Server 也已確認 canonical 與舊 ID 各只有一份 authority。
