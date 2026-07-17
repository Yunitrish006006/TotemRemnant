# Design: Death Backpack Pre-Drop Capture

## 1. Injection point

使用 `Player.dropEquipment(ServerLevel)` 中呼叫 `Inventory.dropAll()` 前的 Mixin 注入點。

此時原版已完成：

- 判斷 `keepInventory=false`。
- 移除 Inventory／Equipment 中具有原版防掉落／消失效果的物品。

但尚未：

- 生成任何玩家 Inventory 死亡 ItemEntity。
- 清空玩家 Inventory。

因此這是能同時維持原版語意並避免世界掉落競態的最窄切入點。游標、玩家 2×2 crafting inputs、工作站暫存 inputs 與 addon-owned player slots 不屬於原版 Inventory，服務會在此切點將它們納入同一個 Server-side transaction。

## 2. Capture plan

### Inventory 與 Equipment

從 `player.getInventory()` 的 `0 .. getContainerSize()-1` 建立 `CapturedSlot(slot, stackCopy)`：

- 空 stack：忽略。
- `PortableContainerPolicy.mayInsertIntoBackpack(stack) == false`：忽略，保留在原始來源或交由既有排除流程。
- 其他 stack：加入死亡背包內容。

### Transient player-owned stacks

另外建立 transient 快照：

- `player.containerMenu.getCarried()`：目前 active menu 的游標 stack。
- `player.inventoryMenu.getCraftSlots()`：玩家固定 2×2 crafting inputs。
- Active vanilla workstation 中，原版 `removed(Player)` 會透過 `clearContainer` 返還玩家的 input slots。

工作站使用嚴格的 class／slot-range 白名單：

- `CraftingMenu`：3×3 input grid，不含 result。
- `ItemCombinerMenu`：Anvil／Smithing 的 `0 .. resultSlot-1`。
- `GrindstoneMenu`：兩個 repair inputs。
- `StonecutterMenu`：單一 input。
- `LoomMenu`：banner／dye／pattern 三個 inputs。
- `CartographyTableMenu`：map／additional 兩個 inputs。
- `EnchantmentMenu`：item／lapis 兩個 inputs。

處理規則：

- 一般物品：加入同一個死亡背包 transaction。
- `PREVENT_EQUIPMENT_DROP`／消失詛咒：直接從 transient source 清除，不保留也不掉落。
- DeadRecall 一般背包、死亡背包或其他受限制可攜式容器：不得巢狀，直接生成獨立世界掉落物；若實體建立失敗則安全回填 Inventory，交給原版 `dropAll()`。
- Result preview：不屬於玩家已取得物品，不擷取。
- 箱子、熔爐、釀造台、漏斗、坐騎、村民等持久 block／entity inventory：不讀取、不修改、不擷取。

### Addon-owned player inventory

第三方整合走公開 SPI，不掃描未知 Menu，也不反射猜測第三方私有 inventory：

- `DeathBackpackAddonInventoryProvider`：每個 addon 以唯一 `Identifier` 註冊 provider，回傳該玩家目前應參與死亡掉落的 slot。
- `DeathBackpackAddonSlot`：提供穩定 `sourceKey`、不可變 `snapshot()`、commit-time `clearIfUnchanged(expected)` 與 rollback `restoreIfEmpty(stack)`。
- Registry 使用 deterministic insertion order；重複 provider ID 直接拒絕。
- Provider snapshot 例外只隔離該 provider並記錄警告，不得阻止原版 Inventory／Equipment 擷取。
- 同一 provider 不得回傳重複 source key；重複來源視為 provider snapshot 失敗並隔離。
- addon stack 仍套用 `PortableContainerPolicy`，DeadRecall 背包與受限制容器不得成為死亡背包內容。

### Trinkets Updated adapter

Minecraft 26.2 內建 optional adapter：

- 只在 Fabric Loader 偵測到 `trinkets_updated` 後，以隔離 class-loading 方式載入 adapter。
- 使用 `TrinketsApi.getAttachment(player).forEachDroppable(..., false)`。
- Trinkets 判定為 `DROP` 的 slot 才進入 DeadRecall transaction。
- `KEEP` 與 `DESTROY` 不由 DeadRecall重新判定，繼續遵守 Trinkets 的 slot、callback、enchantment 與 keepInventory 規則。
- Commit 前以 Item、count 與完整 Components 比對目前 slot；內容已改變或 slot 已失效時拒絕提交並進入 rollback。
- Trinkets Updated 與 Yumi 僅是開發／相容性 runtime dependency，不會在 `fabric.mod.json` 成為必要依賴；未安裝時 DeadRecall 可獨立啟動。

Accessories 在目前 Minecraft 26.2 沒有直接可編譯版本，因此不以脆弱反射維護特定舊 API；Accessories 或其他 addon 可透過公開 SPI 提供其權威 droppable slots。

## 3. Transaction order

正常流程：

1. 建立 Inventory／Equipment、active cursor、玩家 2×2 crafting inputs、白名單工作站 inputs 與 addon slots 的不可變快照。
2. 清除 transient 消失詛咒物品；將 transient DeadRecall 背包或受限制容器改為獨立世界掉落。
3. 建立死亡背包 ItemStack，寫入唯一 ID 與 `DataComponents.CONTAINER`。
4. 暫時從玩家 Inventory 與 transient sources 移除捕獲 stack。
5. 依 deterministic provider／slot 順序呼叫 addon `clearIfUnchanged`；每個成功移除的 addon slot立即記錄到 rollback 清單。
6. 生成死亡背包 ItemEntity。
7. 建立死亡 Space Unit 節點並寫入背包 binding。
8. 原版 `Inventory.dropAll()` 繼續執行，只掉落未捕獲或 rollback 回填的 Inventory 物品。
9. `AFTER_DEATH` 不再執行任何附近掉落物收集，也不保留 completed-capture 狀態 Set。
10. Discord 與玩家通知在交易完成後執行，通知失敗只記錄警告，不得回滾背包或物品。

失敗流程：

1. 若已建立死亡節點，將節點設為 `DISABLED` 並移除該玩家的探索／收藏引用。
2. 若已生成死亡背包實體，立即 discard。
3. 將 Inventory CapturedSlot copy 還原到原槽位；若槽位已被占用，使用 Inventory 安全回填。
4. 將捕獲的 cursor／crafting／workstation stack 回填 Inventory，而不是放回 transient source，確保接續的原版 `Inventory.dropAll()` 能生成世界掉落物。
5. 以反向順序對已成功清除的 addon slots 呼叫 `restoreIfEmpty`。
6. Addon 原位復原拒絕、失效或拋例外時，將 stack 回填玩家 Inventory，交給原版死亡掉落；不得靜默刪除。
7. 原版 `Inventory.dropAll()` 正常掉落還原後的物品。
8. 不執行任何附近 ItemEntity 掃描或死亡背包 fallback，避免重新引入誤收與跨玩家競態。

## 4. Data integrity invariants

- 每個被捕獲 stack 只存在於原始 player-owned source、rollback Inventory、原位 addon rollback 或死亡背包其中之一。
- 正常完成後，捕獲的 Inventory、游標、crafting、工作站及 addon sources 必須為空，不得再次掉落同一 stack。
- Addon slot 只有在目前 Item、count 與 Components 仍等於 snapshot 時才能被清除。
- 任一 addon slot 拒絕 commit 時，整個死亡背包 transaction 必須失敗；先前已清除的 addon slots 必須回滾。
- Provider snapshot 失敗不得影響其他 provider 或原版 Inventory 擷取。
- 失敗時死亡背包實體不得留在世界，所有捕獲 stack 必須可由原位 addon source、接續的原版 `dropAll()` 或已安全生成的世界掉落找到。
- 失敗後不得留下可見／可傳送的 ACTIVE 死亡節點或探索引用。
- 背包類物品不得成為另一個死亡背包的內容，包括第三方 slot 中的受限制可攜式容器。
- Result preview 與外部持久容器 storage 不屬於玩家死亡交易，不得被讀取、移除或寫回。
- 不以「所有非 Inventory slots」推測玩家所有權；新增 Menu 必須明確加入白名單或使用 addon integration SPI。
- ItemStack 使用 `copy()` 保存完整 Data Components，不經 ItemEntity 合併或反序列化回注。
- Client 不得決定或修改捕獲內容。
- 通知、Discord 或記錄輸出不是交易的一部分，不得影響物品提交狀態。

## 5. Death node recovery lifecycle

死亡背包建立後會在 Custom Data 中保存綁定的 death node UUID。背包被清空並關閉時：

1. 從背包 binding 讀取 node UUID。
2. 查詢 SavedData 中的實際 `DEATH` record。
3. 使用 record 的原 owner 驗證並停用該 node，而不是要求目前清空背包的玩家必須是原 owner。
4. 將空死亡背包從目前持有者 Inventory／手中移除。
5. 玩家與 Discord recovery 通知在停用完成後執行，任何通知例外只記錄警告。

此順序保證：

- 原 owner 離線後，死亡背包實體與 ACTIVE node 仍可由其他玩家回收。
- 清空某個綁定背包只會停用該 UUID 對應的 node，不會以位置或附近實體推測目標。
- 通知失敗不得留下空背包，也不得把已停用 node 重新啟用。
- `DISABLED` node 與 discovery reference 可經 SavedData 保存；可見地圖仍以 ACTIVE status 過濾。

### Dedicated Server restart 驗證

CI 使用 `gametest` source set 中的 server-only probe，以及 Loom 的正常 Dedicated Server run configuration `runRestartProbe`。Probe 不會進入 production JAR。測試共啟動三個彼此獨立的 Dedicated Server JVM，並共用同一個 `run/restartProbe/world`：

1. **seed**：建立 ACTIVE death node、discovery reference，以及包含 diamond x11 並綁定該 node 的 death-backpack ItemEntity；正常關服保存世界。
2. **recover**：下一個 JVM 重新載入相同 world、entity region 與 SavedData，建立新的 `ServerPlayer` 物件但沿用相同 UUID，清空死亡背包並停用 node；正常關服保存。
3. **verify**：第三個 JVM再次載入，確認 node 仍為 `DISABLED`、owner／type／discovery 正確，且已移除的 death-backpack ItemEntity 不會復活。

Probe 使用 force-loaded chunk、啟動後載入等待與修改後保存等待，避免把 entity chunk 尚未載入誤判成遺失。每個 phase 都必須寫入獨立 success marker；任一 assertion 或 entrypoint 未執行都會讓 CI 失敗。

這套測試涵蓋真正的 Minecraft world/entity/SavedData save-and-reload 路徑，與單純的 codec round-trip 不同。它已驗證同 UUID replacement player 的回收行為，但不模擬真實網路連線、登入封包或真人客戶端 UI。

## 6. Legacy migration

以下舊流程已從 `Deadrecall` 程式碼與 Mixin 設定中完整刪除：

- `rememberExistingDropsBeforeDeath`。
- 舊 `handlePlayerDeath` nearby-drop collector。
- 半徑 10 格 AABB 掃描。
- 既有 ItemEntity UUID 差集。
- 雙重 `server.execute` 排程。
- 掃描後 `ItemEntity.discard()` 回收。
- `pendingDeathCollections`、`scheduledDeathBackpackCollections`、`PendingDeathCollection` 與相關常數。
- 僅用於停用上述死碼的 `DeadrecallDeathBackpackMixin`。
- Legacy Mixin 刪除後已無消費者的 `COMPLETED_CAPTURES` Set 與 `consumeCompletedCapture`。

死亡背包現在只有 `Player.dropEquipment` 前的直接擷取路徑。直接擷取失敗時只保證物品回到原版掉落，不再嘗試從世界掉落物建立替代死亡背包。

## 7. Test strategy

### Automated

- Java 25 compile 與 Mixin annotation processing。
- Fabric Server GameTest 啟動，確認 `Player.dropEquipment` 注入成功套用。
- Inventory capture policy：一般物品捕獲、背包排除、空 stack 忽略。
- 失敗注入測試：實體加入後失敗時，槽位完整還原且未完成 ItemEntity 被丟棄。
- 死亡節點後失敗測試：節點停用、探索引用移除、槽位還原。
- `keepInventory=true` 與消失詛咒原版語意。
- 64 格、自訂名稱、耐久、盔甲、副手等 Components 保存。
- active menu cursor 與玩家 2×2 crafting inputs 擷取。
- 外部箱子持久 storage 不得被擷取或修改。
- Crafting Table、Anvil、Smithing、Grindstone、Stonecutter、Loom、Cartography Table 與 Enchanting Table inputs 擷取。
- Result preview 不得進入死亡背包。
- transient 背包獨立掉落、transient 消失詛咒銷毀。
- transient／workstation transaction 故障後必須回到原版世界掉落。
- Addon registry deterministic order、immutable snapshot 與 duplicate provider ID 拒絕。
- 通用 addon SPI 成功提交、Components 保存、部分清槽拒絕、反向 rollback、provider snapshot 例外隔離與 portable-container 排除。
- Trinkets Updated 4.1.x／Minecraft 26.2 實際載入、adapter 註冊、真實 player `DROP` slot 擷取、Components 保存、source 清空與無重複 ItemEntity。
- 未安裝 Trinkets Updated 時的 optional class-loading 邊界由 Fabric `suggests` metadata 與反射 adapter 載入設計維持；正式模組沒有 required Trinkets dependency。
- 非 owner 回收綁定死亡背包、其他 death node 隔離與原 owner 離線後實體保留。
- recovery 通知故障不得影響 node disable 或空背包移除。
- 回收後 Space Unit 與 discovery SavedData codec round-trip。
- 三次正常 Dedicated Server JVM 的 seed／recover／verify world reload。
- entity region 中的 death backpack 跨程序載入，以及回收刪除後不得復活。
- 同 UUID replacement `ServerPlayer` 回收並保存 `DISABLED` death node。
- 同位置既有 ItemEntity 不得被修改或收集。
- 兩名玩家同位置、同 tick 死亡時，各自背包內容不得交叉。
- 直接擷取失敗時，原版掉落必須存在，且不得建立第二條死亡背包路徑。
- Legacy scanner 實體刪除後完整 Java 25 build 與 Server GameTests。

### Integration

- 岩漿、仙人掌、虛空、爆炸及死亡點高密度掉落物。
- 只持有一般／死亡背包時的原版掉落。
- Trinkets Updated `DROP` slot 與公開 addon inventory SPI。
- 其他 addon 依公開 SPI 實作後的專屬 integration fixture。
- 與第三方墓碑、keep inventory 與飾品模組的事件優先序仍屬跨模組組合驗收，不取代單一模組自動回歸。
