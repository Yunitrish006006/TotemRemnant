# TotemRemnant

TotemRemnant 是 Totem 系列的背包、死亡物品保護與可攜式容器安全模組。
目前版本為 **0.2.11**，精確搭配 TotemCore **0.6.0**。

## 安裝

將下列 JAR 放入 Client 與 Server 的 `mods/`：

1. Fabric API `0.154.2+26.2`
2. TotemCore `0.6.0`
3. TotemRemnant `0.2.11`

| 項目 | 需求 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3+ |
| Java | 25+ |
| 選配 | Trinkets Updated 4.1.0-beta.2+ |

使用 DeadRecall 2.4.11 整合 JAR 時不要再安裝獨立 TotemRemnant；整合包已
內含相同模組。

## 遊戲內 Totem 手冊

手持普通書對鍛造台按右鍵，可將一本書記錄為原版成書形式的
`Totem 手冊`。手冊會整合所有已安裝 Totem 模組登記的章節；若另一手已
持有手冊，只會刷新該手冊而不消耗普通書。兩手各持一本系統生成的手冊時，
對鍛造台使用會保留主動手的一本並合併另一手。Nexus 產生的舊版說明書則
需對磁石使用一次才會安全升級。

## 一般背包教學

所有背包都在鍛造台升級。前三階使用空的 Bundle 作為 template；裝有物品的
Bundle 不會被配方接受，避免其中內容在合成時遺失：

| 結果 | Template | Base | Addition | 容量 | 擴充格 |
| --- | --- | --- | --- | ---: |
| 基礎背包 | Bundle | Bundle | 皮革 | 9 | 1 |
| 標準背包 | Bundle | 基礎背包 | 鐵錠 | 18 | 2 |
| 進階背包 | Bundle | 標準背包 | 鑽石 | 27 | 3 |
| 獄髓背包 | 獄髓升級模板 | 進階背包 | 獄髓錠 | 36 | 4 |

右鍵背包即可開啟。內容保存於物品 Data Components，支援原版拖曳與
Shift-click。只要玩家物品欄內帶著一般分級背包，按 `E` 開啟原版物品欄
時，旁邊也會同步顯示其中的真實格位、物品與數量。攜帶多個背包時，把
游標移到原版格位中的某個背包即可切換。側邊面板是伺服器驗證的真實格位，
可直接拿放、右鍵拆分、拖曳、數字鍵交換、丟棄、雙擊收集與 Shift-click；
禁止巢狀容器與容量規則和右鍵背包畫面相同。死亡背包不加入側邊面板，維持
獨立的回收流程。

右鍵開啟一般背包時，原版風格的擴充區會顯示在容器右側。模組可以直接
拖入或取出，同一功能不能重複安裝：

- 工作台模組：在右鍵背包介面的擴充欄旁，直接啟用內嵌的 `3×3` 合成區；
  配方與世界目前的動態配方一致，關閉介面時未取出的材料會退回玩家物品欄。
- 金屬壓縮模組：內容變更時依目前世界的 3×3 配方，壓縮原鐵／原銅／原金、
  鐵粒／金粒，以及鐵／銅／金／獄髓錠。可安全連續壓縮（例如 81 粒成 1 方塊）；
  配方被移除或改成其他產物時不執行，也不會吞掉不足一組或放不下的材料。
- 同類收納模組：物品進入玩家物品欄時，只把背包已存在的相同物品與
  Data Components 變體收進去，並可使用後續空格延續堆疊。
- 容量擴充模組：每個占用一個擴充格並增加一排 `9` 格，可重複裝滿背包的
  擴充格。拆除一個前必須清空當次即將消失的最後一排；連續拆除時會逐排重新
  檢查，已失效的格位也不能再放入物品。超過六排時介面維持玩家原本的 GUI
  大小，以滑鼠滾輪瀏覽後續儲存列；玩家物品欄、升級區與合成區保持固定。
- 一次性靈魂綁定模組：死亡時保留該背包及全部內容，重生後還原到物品欄，
  同時消耗模組。多個已安裝的背包會各自保留並各自消耗一個模組。
- 終界箱存取模組：在右鍵背包介面旁新增原版風格按鈕，經伺服器再次驗證後
  開啟該玩家自己的 27 格終界箱；內容沿用原版資料，不會複製進背包。
- 防爆與仙人掌模組：只占一格，同時免疫仙人掌、爆炸與重生點爆炸。
- 防火模組：讓基礎、標準與進階背包免疫火、熔岩與熱地板；獄髓背包不需要。
- 防消失模組：地面上的背包不會在原版五分鐘期限後自然消失。
- 虛空防護模組：一般背包越過世界底部刪除界線時會被向上救回，並免疫
  直接虛空傷害；模組不消耗，但會占一個擴充格，獄髓背包不會免費自帶。

背包等級決定容量與擴充格數；獄髓背包另外因材質永久自帶防火，而且不占
擴充格。十種功能模組共用最多四格，玩家可依用途混搭。仙人掌與爆炸共用
一個模組，自然消失則使用另一個模組。死亡背包維持系統級完整保護且不占擴充格。

四級一般背包都可以和任意一個以上的原版染料放進工作台染色；再次加入
不同染料時會沿用原版皮革物品的混色規則。染色合成會保留背包內容、自訂
名稱與其他 Data Components。拿染色背包右鍵裝水煉藥鍋可洗回原本的棕色，
並消耗一層水。死亡背包的紅色專用外觀不可染色。

## 物品 ID 相容遷移

新取得的背包使用 Remnant canonical ID：
`totem:remnant/backpack_basic`、`backpack_standard`、`backpack_advanced`、
`backpack_netherite` 與 `death_backpack`（後四者同樣位於
`totem:remnant/` 路徑）。

Remnant standalone 只註冊上述 canonical ID。安裝 DeadRecall 2.4.11
整合包時，外層相容主機才會解碼 `deadrecall:backpack_*` 與
`deadrecall:death_backpack`；右鍵使用舊背包時會就地換成 canonical
物品，保留內容、名稱、染色與其他 Data Components。系統不會在啟動時
掃描離線玩家或未載入區塊。

## 掉落保護模組

一般背包的仙人掌與爆炸保護共用一個模組，自然消失與虛空防護各使用另一個模組。
基礎、標準與進階背包也可用防火模組取得火焰／熔岩保護；獄髓背包則永久自帶防火。

## 死亡背包

玩家死亡且 `keepInventory=false` 時，Server 會在原版生成掉落物之前
封裝物品欄、裝備、副手、游標、玩家合成格與支援工作站輸入：

管理員可用世界規則控制是否生成死亡背包；預設為 `true`：

```text
/gamerule totem:remnant_generate_death_backpacks false
```

設為 `false` 後不生成死亡背包，其餘物品恢復原版死亡掉落。裝有一次性
靈魂綁定模組的普通背包及其他獨立保留規則仍照常處理。

死亡背包預設只有死亡玩家本人能從地面撿起。管理員若希望其他玩家也能
代為撿取，可關閉第二條規則：

```text
/gamerule totem:remnant_death_backpack_owner_pickup_only false
```

死亡背包會把主人 UUID 保存在物品資料中；重新啟動伺服器或背包被再次丟出
也不會失去歸屬。舊版本已經存在、沒有主人資料的死亡背包仍允許撿取，避免
升級後形成無法回收的物品。

- 容量依死亡物品數動態調整，最多 54 格；超過的堆疊會在同一死亡位置以原版
  散落物形式掉出，不會被塞進看不見、無法取回的格位。
- 死亡背包不會自然消失，免疫一般傷害，並在虛空下方被向上救回。
- 地面上的背包會顯示紅色定位光柱。
- 巢狀禁止規則開啟時，DeadRecall 背包與其他可攜式容器維持獨立掉落；
  關閉規則後則可一併收進死亡背包。
- 背包完全清空並關閉後會移除；任何協助回收的玩家都可完成此流程。
- 若 Nexus 已安裝，Remnant 會在死亡交易前自動掃描有效傳送介面，不要求
  曾經成功傳送。依主手、副手、其餘快捷列、主物品欄順序保留第一個，並於
  重生後 exactly once 還原。同堆疊其餘物品照常進死亡背包；空白地圖與
  消失行為仍不符合資格，也不會掃描巢狀容器。

若同時安裝 TotemNexus，Remnant 會透過 TotemCore 的選配生命週期建立
死亡 Space Unit，保存死亡節點 ↔ 背包 Entity UUID 的雙向關聯。沒有
Nexus 時死亡背包仍可獨立使用。

死亡背包成功建立或回收時，Remnant 會發布 TotemCore 的型別事件。安裝
TotemDiscordBridge 時由 Bridge 自行訂閱並送出通知；Remnant 不直接依賴
Discord，也不需要 DeadRecall 額外安裝 listener。

> `/back` 是 DeadRecall 相容整合包的額外功能，不是 TotemRemnant
> standalone API 的一部分。

## 可攜式容器安全

Remnant 預設禁止 Bundle、Shulker Box、DeadRecall 背包與
`deadrecall:portable_containers` tag 物品互相非法巢狀。限制涵蓋 GUI、
Shift-click、漏斗、漏斗礦車、投擲器／發射器與相容自動化。

管理員可關閉 Remnant 的額外巢狀限制：

```text
/gamerule totem:remnant_prevent_portable_container_nesting false
```

規則會即時同步給客戶端，不需要重新登入。關閉後只解除 Remnant 加上的限制；
Minecraft 原版本身禁止的容器組合仍維持原樣。死亡背包也會在規則關閉時允許
收納可攜式容器，不再強制將它們分開掉落。

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

0.2.11 的 Java 25 build、單元測試、required Fabric GameTests 與三個
客戶端視覺 GameTest（含擴充區截圖）已通過。0.1.7 的完整整合 Dedicated Server 亦已確認
canonical 與舊 ID 各只有一份 authority。
