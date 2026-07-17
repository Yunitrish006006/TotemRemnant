# Tasks: Death Backpack Pre-Drop Capture

## 1. Specification and API verification

- [x] 1.1 確認 Minecraft 26.2 `Player.dropEquipment(ServerLevel)` 在 `keepInventory=false` 且消失詛咒處理後呼叫 `Inventory.dropAll()`。
- [x] 1.2 定義第一階段擷取範圍為原版 Inventory 與 Equipment slots。
- [x] 1.3 定義所有 DeadRecall 背包排除並交由原版掉落。
- [x] 1.4 確認 active menu 游標 stack、玩家 2×2 crafting slots 與外部持久容器槽位的死亡語意。
- [x] 1.5 確認原版 Crafting／ItemCombiner／Grindstone／Stonecutter／Loom／Cartography／Enchantment 暫存輸入槽的死亡語意。
- [x] 1.6 確認第三方飾品槽與其他 addon 自訂 inventory API 的死亡語意。
  - [x] 公開 `DeathBackpackAddonInventoryProvider`／`DeathBackpackAddonSlot` transaction SPI。
  - [x] Trinkets Updated 4.1.x 只提供其 `forEachDroppable` 判定為 `DROP` 的玩家飾品槽；`KEEP`／`DESTROY` 繼續由 Trinkets 處理。
  - [x] 未安裝 Trinkets Updated 時不載入 adapter class，DeadRecall 維持可獨立啟動。

## 2. Direct capture implementation

- [x] 2.1 新增 `DeathBackpackCaptureService` 與不可變 CapturedSlot 快照。
- [x] 2.2 在 `Inventory.dropAll()` 前執行 Server-side 直接擷取。
- [x] 2.3 使用 stack copy 寫入 `DataComponents.CONTAINER`，不經世界 ItemEntity 回收。
- [x] 2.4 保留死亡背包唯一 ID、死亡節點 binding、拾取延遲與 unlimited lifetime。
- [x] 2.5 成功後只讓原版掉落排除的背包類物品。
- [x] 2.6 例外時 discard 未完成的死亡背包並還原玩家槽位。
- [x] 2.7 通知與 Discord 失敗不得回滾已完成的死亡背包交易。
- [x] 2.8 死亡節點建立後失敗時停用節點並移除探索引用。
- [x] 2.9 將 active menu 游標與玩家 2×2 crafting inputs 納入同一 transaction；暫存背包改為獨立掉落，暫存消失詛咒物品直接銷毀。
- [x] 2.10 移除 legacy scanner 刪除後已無消費者的 completed-capture Set。
- [x] 2.11 以 class／slot-range 白名單擷取原版工作站關閉時會返還玩家的 inputs，並排除 result preview 與持久容器。
- [x] 2.12 清空死亡背包時依背包綁定的 node ID 停用節點，不要求清空者必須是原 owner；回收通知失敗不得中斷節點停用或空背包移除。
- [x] 2.13 將 addon-owned player slots 納入同一 transaction：snapshot、commit-time compare-and-clear、反向 rollback 與 Inventory fallback。
- [x] 2.14 provider snapshot 例外只隔離該 provider；不得阻止原版 Inventory／Equipment 的死亡背包擷取。
- [x] 2.15 addon slot 中的 DeadRecall 可攜式容器不得巢狀進死亡背包，且不得由 DeadRecall 擅自清除。

## 3. Legacy compatibility

- [x] 3.1 直接擷取成功時取消舊 `handlePlayerDeath` 掃描器。
- [x] 3.2 死亡前的附近 ItemEntity UUID 掃描已在執行期停用。
- [x] 3.3 直接擷取失敗時只回到原版 `Inventory.dropAll()`，不再執行 nearby-drop fallback。
- [x] 3.4 舊半徑掃描器、雙重 server task 與 UUID 差集已從實際死亡路徑停用。
- [x] 3.5 已從 `Deadrecall` 刪除舊半徑常數、Map、Set、record、雙重 task、掃描方法與相容 Mixin。

## 4. Automated tests

- [x] 4.1 一般物品與空 stack 擷取 policy 單元測試；背包排除由 Server GameTest 驗證。
- [x] 4.2 64 格、耐久、自訂名稱、裝備與副手 Components 保存 GameTest。
- [x] 4.3 第三方自訂 Components 保存測試。
  - [x] 公開 SPI addon slot 的 count 與 `CUSTOM_NAME` Component 完整保存。
  - [x] 真實 Trinkets Updated `DROP` slot 的 count 與 `CUSTOM_NAME` Component 完整保存，且不產生重複世界掉落。
- [x] 4.4 實體加入後失敗與死亡節點建立後失敗的槽位 rollback 測試。
- [x] 4.5 Inventory 與 Equipment 混合 GameTest；確認捕獲物不生成世界 ItemEntity、排除背包由原版掉落且只建立一個死亡背包。
- [x] 4.6 `keepInventory=true` 與消失詛咒 GameTest。
- [x] 4.7 Java 25 build、Fabric GameTest Server 與相關 Mixin 的實際套用驗證。
- [x] 4.8 rollback 後由原版生成掉落物，且 legacy 掃描器不會再次建立死亡背包。
- [x] 4.9 游標、2×2 crafting inputs、外部箱子隔離、暫存背包排除、暫存消失詛咒與 transient rollback GameTest。
- [x] 4.10 Crafting Table、Anvil、Smithing、Grindstone、Stonecutter、Loom、Cartography Table、Enchanting Table inputs 與 workstation rollback GameTest。
- [x] 4.11 非 owner 回收、其他節點隔離、通知故障與回收後 SavedData codec round-trip GameTest。
- [x] 4.12 以正常 Dedicated Server run 連續執行 seed／recover／verify 三個獨立 JVM，驗證 entity region、Space Unit SavedData、discovery、同 UUID replacement player 與回收刪除跨程序持久化。
- [x] 4.13 addon provider registry 的 deterministic order、immutable snapshot 與 duplicate ID 拒絕 JUnit。
- [x] 4.14 addon 部分 commit 失敗時，已清除 addon slot、Inventory 與未完成死亡背包全部 rollback。
- [x] 4.15 addon provider snapshot 例外隔離與 portable-container 排除 GameTests。

## 5. Integration tests

- [x] 5.1 同位置存在其他 ItemEntity 時不得誤收。
- [x] 5.2 兩名玩家同 tick、同位置死亡不得互相收取。
- [x] 5.3 岩漿、仙人掌、虛空與爆炸死亡回歸。
- [x] 5.4 只持有一般／死亡背包時維持原版掉落且不建立巢狀背包或死亡節點。
- [x] 5.5 active menu 游標與玩家 2×2 crafting inputs 會被擷取；外部箱子實際儲存槽不得被擷取或修改。
- [x] 5.6 原版工作站暫存 inputs 會被擷取；result preview 與持久 block/entity inventory 不得進入死亡 transaction。
- [x] 5.7 第三方飾品模組與 addon 自訂 inventory API 相容測試。
  - [x] Trinkets Updated 4.1.x 在 Minecraft 26.2 GameTest runtime 中實際載入、註冊 adapter 並擷取真實 player slot。
  - [x] 通用 SPI fixture 驗證成功提交、Components、CAS 拒絕、反向 rollback、provider 隔離與背包排除。
  - [x] Accessories 尚無 Minecraft 26.2 直接編譯版本；其他 addon 以公開 SPI 接入，不使用版本脆弱的反射 inventory 掃描。
- [x] 5.8 原 owner 離線後死亡背包實體與節點保持可回收；任意玩家清空綁定背包可停用正確節點；SavedData codec round-trip 保留回收狀態。
- [x] 5.9 Dedicated Server 實際重啟、同 UUID replacement player 與世界檔 reload 回歸。

## 6. Documentation and delivery

- [x] 6.1 建立 proposal、design、tasks 與 delta spec。
- [x] 6.2 更新玩家文件，說明死亡背包在掉落生成前直接封裝與原版 fallback。
- [x] 6.3 更新 changelog／版本變更清單。
- [x] 6.4 更新 Roadmap 狀態。
- [x] 6.5 新增 addon inventory SPI 與 Trinkets Updated compatibility 開發者文件。
