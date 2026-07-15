# Totem Remnant：Offline Player Body 離線玩家身體規格

## 1. 目標

玩家離開伺服器後，世界中仍保留一具代表該玩家的離線身體。離線身體承接玩家登出時的生存狀態，能受到環境、怪物及符合規則的玩家攻擊影響，並在玩家重新登入、身體死亡、伺服器重啟或管理員介入時保持資料一致。

此系統屬於 Totem Remnant，因為它管理玩家死亡、物品保全、死亡紀錄與離線殘留狀態。Nexus、Discord Bridge 與其他模組只能透過事件或公開 API 取得結果，不得直接修改離線身體資料。

## 2. 範圍

包含：

- 玩家登出後建立離線身體。
- 離線身體保存玩家生命、飢餓、裝備、物品、經驗、狀態效果與位置。
- 玩家重新登入時重新接回仍存活的身體。
- 離線身體死亡時執行一次且僅一次死亡流程。
- 與死亡背包、死亡紀錄、Nexus 死亡節點及 Discord Bridge 死亡事件整合。
- Server restart 後恢復仍存在的離線身體。
- 管理員查詢、傳送、移除及修復異常資料。

不包含：

- 離線玩家自動戰鬥、採集、交易、使用物品或執行 AI。
- 將離線身體做成可操控 NPC。
- 讓普通玩家直接瀏覽離線玩家完整背包。
- 跨伺服器或 proxy 網路中的身體同步。
- Client 端自行建立、移除或修改離線身體。

## 3. 啟用規則

世界規則：

```text
deadrecall:offline_player_bodies
```

Boolean。建議預設 `false`，避免既有世界在升級後突然改變登出風險。伺服器管理員可在確認玩法需求後開啟。

建議設定：

```text
offlineBodyReconnectGraceTicks = 0
offlineBodyMaxLifetimeTicks = 0
offlineBodyForceLoadChunk = false
offlineBodyDirectLooting = DISABLED
offlineBodyCreateDuringServerStop = false
```

- `offlineBodyReconnectGraceTicks`：玩家重新登入前的保護／延遲窗口。預設 0，避免被用來規避戰鬥。
- `offlineBodyMaxLifetimeTicks`：0 表示不自動消失；非 0 時到期後由管理規則處理。
- `offlineBodyForceLoadChunk`：預設不強制載入區塊，避免大量離線身體造成伺服器負擔。
- `offlineBodyDirectLooting`：預設禁止直接打開離線身體背包；物品只透過死亡流程掉落或進入死亡背包。
- `offlineBodyCreateDuringServerStop`：必須維持 false；伺服器正常關閉時不得因玩家連線被中斷而替全體線上玩家建立新身體。

## 4. 建立條件

Server 在玩家離線事件中判斷是否建立離線身體。Client 沒有任何建立權限。

必須全部成立：

- Gamerule 已開啟。
- 玩家是 Survival 或 Adventure 模式。
- 玩家不是已死亡、正在切換維度、正在完成登入流程或 fake player。
- Server 不是正在正常關閉。
- 同一玩家 UUID 目前沒有有效離線身體。
- 玩家資料可以成功切換到離線身體權威狀態。

不得建立：

- Creative 或 Spectator 玩家。
- 已在死亡畫面或死亡流程中的玩家。
- Server shutdown、crash recovery 或 reload 造成的連線清理。
- 無法安全序列化玩家資料時。

建立位置必須使用 Server 端登出當下的實際 Dimension、座標、yaw、pitch 與姿勢，不信任任何 Client 座標。

## 5. 離線身體模型

建議建立專用 Entity：

```text
OfflinePlayerBodyEntity
```

實體至少保存：

- Owner UUID。
- Owner 最新名稱與 GameProfile 顯示資料。
- Body UUID。
- Dimension、座標、旋轉、姿勢與建立時間。
- Health、absorption、air、fire ticks、freezing、fall distance。
- Food level、saturation、exhaustion。
- Inventory、armor、offhand、selected slot。
- XP level、total XP、score。
- Potion effects。
- Game mode snapshot。
- Death state、processed flag 與 dataVersion。

離線身體應具有可碰撞實體、重力與基本受傷邏輯。Client renderer 可使用玩家外觀呈現，但 renderer 必須只存在於 client source set；Dedicated Server 不得載入 client class。

離線身體不是 `ServerPlayerEntity`，不得觸發玩家登入、聊天、advancement、統計、配方解鎖或權限檢查流程。

## 6. 資料權威與防複製

離線身體存在期間，玩家可變生存狀態的權威來源是離線身體資料，不是原本的 playerdata 背包副本。

建立離線身體時必須完成原子化交接：

1. 從 ServerPlayer 讀取生命、食物、物品、裝備、經驗、狀態效果與位置。
2. 將資料寫入離線身體與 SavedData record。
3. 在玩家 playerdata 寫入 body lock，記錄 `bodyUuid`、`dataVersion` 與最後安全快照。
4. 確認 body record 可讀後，才允許玩家連線完全移除。

不允許同一組 ItemStack 同時以「可恢復玩家背包」與「可掉落離線身體背包」兩種權威狀態存在。任何登入、死亡、管理移除或重啟修復流程都必須先檢查 body lock 與 body record。

若發現 playerdata 有 body lock，但 SavedData 或 entity 不一致，Server 必須進入修復流程，而不是直接讓玩家取得可能重複的背包。

## 7. 行為規則

離線身體在區塊已載入時 tick。預設不保持區塊載入；所在區塊卸載時，身體狀態暫停並隨區塊保存。

離線身體應受到：

- 窒息、火焰、岩漿、溺水、粉雪、虛空、摔落及爆炸等環境傷害。
- 怪物攻擊。
- 投射物傷害。
- 玩家攻擊，但必須尊重原版 PVP 設定與伺服器保護規則。

離線身體不得：

- 自動移動、攻擊、挖掘或使用物品。
- 自動消耗食物進行一般活動。
- 自動觸發 advancement 或統計。
- 被普通玩家直接開啟背包，除非伺服器明確啟用 direct looting 設定。

狀態效果、火焰、空氣值與冰凍等持續性狀態可依原版 tick 遞減或造成效果。自然回血與飢餓消耗建議預設關閉，避免離線時間造成難以預期的背景資源變化；若未來提供設定，必須寫入本規格後再實作。

## 8. 重新登入

玩家登入時，Server 必須先檢查是否存在該 UUID 的有效離線身體 record。

若身體仍存活：

- 暫停玩家一般出生點放置流程。
- 以 Server thread 載入身體所在 Dimension 與區塊。
- 將身體的生命、食物、物品、裝備、經驗、狀態效果、位置與旋轉交還給玩家。
- 移除離線身體 entity。
- 清除 body lock 與 SavedData record。
- 玩家回到身體目前位置，而不是登出前的舊 playerdata 位置。

若身體已死亡且死亡流程已處理：

- 玩家不得取回死亡前背包。
- 若原版 `keepInventory` 或 Remnant 設定要求保留物品，必須依死亡處理結果恢復。
- 玩家登入後進入正常死亡後狀態或被放置到合法重生點，並收到離線死亡摘要。
- body lock 與 pending death record 必須清理。

若身體資料遺失或損壞：

- Server 必須拒絕直接套用不可信背包。
- 記錄非敏感錯誤。
- 讓 OP 使用修復指令選擇 restore、remove 或 drop。

同一玩家多重登入、快速斷線重連或身體死亡與登入同 tick 競爭時，Server thread 上必須只允許一個結果完成：重新接回或死亡處理，不得兩者同時發生。

## 9. 身體死亡

離線身體死亡時，Server 必須執行一次死亡交易：

1. 將 body state 標記為 `DEATH_PROCESSING`。
2. 建立死亡原因與原版風格 death message。
3. 根據 gamerule、Remnant 設定與模組整合決定物品保留、掉落或死亡背包。
4. 寫入死亡紀錄與 pending login death state。
5. 發布 Remnant death event。
6. 移除離線身體 entity。
7. 將 record 標記為 `DEATH_PROCESSED` 或清除可安全清除的資料。

死亡背包啟用時，背包位置使用離線身體死亡位置。若死亡背包建立成功，物品不得再以掉落物形式出現；若死亡背包建立失敗，必須依安全 fallback 掉落或保留，且不得遺失或複製。

死亡訊息可指出玩家在離線狀態死亡，但不得公開座標、背包內容或其他私密資訊。Discord Bridge 只接收可公開的死亡事件。

## 10. Direct Looting

預設 `offlineBodyDirectLooting = DISABLED`。

若未來啟用直接搜刮，必須滿足：

- 只由 Server 開啟容器。
- 尊重 PVP、領地保護與權限 API。
- GUI payload 不同步玩家無權查看的資料。
- 被取出的物品立刻從 body inventory 刪除並標記髒資料。
- 玩家重新登入時只恢復剩餘物品。
- 所有操作有 revision，避免同時搜刮與登入造成複製。

直接搜刮不屬於第一階段必要功能。第一階段只要求身體可被殺死，並透過死亡流程處理物品。

## 11. 管理指令

所有管理指令至少需要 OP permission level 2，並由 Server 執行權限檢查。

建議指令：

```text
/deadrecall offlinebody list [player]
/deadrecall offlinebody tp <player>
/deadrecall offlinebody remove <player> restore
/deadrecall offlinebody remove <player> drop
/deadrecall offlinebody remove <player> delete
/deadrecall offlinebody repair <player>
```

- `list`：列出有效身體、Dimension、粗略座標、狀態與存在時間。
- `tp`：將執行者傳送到身體附近安全位置。
- `remove restore`：移除身體並把仍存活的 body state 還原到玩家待登入資料。
- `remove drop`：移除身體並在身體位置掉落其物品。
- `remove delete`：移除身體與物品，只能用於資料修復或管理懲戒，必須記錄 audit log。
- `repair`：嘗試修復 body lock、SavedData record 與 entity 之間的不一致。

管理指令不得把 API secret、完整 NBT 或未授權玩家背包內容輸出到一般聊天。

## 12. SavedData

建議 SavedData：

```text
DeadRecallOfflineBodySavedData
```

索引：

```text
ownerUuid -> OfflineBodyRecord
bodyUuid -> ownerUuid
dimension + blockPos -> bodyUuid list
```

`OfflineBodyRecord` 至少包含：

- `dataVersion`
- `ownerUuid`
- `lastKnownName`
- `bodyUuid`
- `dimension`
- `blockPos`
- `createdGameTime`
- `lastUpdatedGameTime`
- `state`
- `deathProcessed`
- `playerDataLocked`
- `inventoryChecksum`

狀態：

```text
ACTIVE
REATTACHING
DEATH_PROCESSING
DEATH_PROCESSED
RECOVERY_REQUIRED
REMOVED
```

所有格式變更必須提供 migration。載入世界時必須清理 orphan index，並把 entity 存在但 SavedData 缺失、SavedData 存在但 entity 缺失、playerdata lock 指向不存在 body 的情況標記為 `RECOVERY_REQUIRED`。

## 13. Networking

第一階段不需要普通玩家 Clientbound 管理 payload。玩家重新登入、死亡摘要與錯誤訊息可以使用既有聊天／系統訊息或專用簡短 payload。

禁止任何 Clientbound 或 Serverbound payload 讓普通玩家：

- 建立離線身體。
- 移除離線身體。
- 修改身體座標、生命、物品或死亡狀態。
- 查看未授權的 body inventory。

管理 GUI 若未來加入，必須使用明確 payload，且每個修改操作都要重新驗證 OP 權限、body UUID、revision 與目前狀態。

## 14. 模組整合

### Remnant

離線身體死亡必須走 Remnant 死亡背包與死亡紀錄整合流程。這是此功能的主要相依模組。

### Nexus

離線身體死亡且成功建立死亡背包時，可發布死亡節點事件。死亡節點只對死亡玩家可見，且不得因玩家離線而公開座標。

Nexus 的人體磁石不得把離線身體當作線上玩家目標。離線身體不是 `PLAYER` Space Unit。

### Discord Bridge

離線身體死亡可轉播為玩家死亡事件。訊息不得包含座標、背包內容、body UUID 或修復資訊。

玩家加入／離開通知仍只代表連線狀態；不得因身體存在而把玩家視為線上。

### Core / Permission

管理指令、未來 GUI 與 direct looting 必須使用公開 permission API。沒有 Core 時，DeadRecall 2.x 可暫時使用 Fabric command permission level。

## 15. 伺服器重啟與關閉

正常關閉：

- 不為正在被關服流程踢出的線上玩家建立新離線身體。
- 已存在的離線身體必須隨 entity 與 SavedData 正常保存。
- 正在 `DEATH_PROCESSING` 或 `REATTACHING` 的 record 必須在下次啟動時進入 recovery。

啟動恢復：

- 掃描 SavedData、body entity 與 playerdata body lock。
- 修復可自動修復的 index。
- 對不能安全判斷權威來源的資料標記 `RECOVERY_REQUIRED`，等待 OP 處理。
- 不得因資料不一致直接刪除玩家物品。

Crash recovery 必須以避免複製為優先，其次才是自動便利恢復。

## 16. 安全與隱私

- Client 不可信，所有建立、死亡、重連及管理動作都由 Server 決定。
- 離線身體資料不得公開精確座標給無權限玩家。
- 一般 log 不輸出完整 inventory NBT。
- 管理 audit log 只記錄操作者、目標玩家、動作與結果。
- Renderer 不得造成 Dedicated Server 載入 client class。
- Fake player、bot 或自動化實體預設不建立離線身體。

## 17. 驗收條件

- Gamerule 關閉時，玩家登出維持原版行為，不建立離線身體。
- Gamerule 開啟時，Survival／Adventure 玩家登出後，世界中建立一具離線身體。
- Creative／Spectator、fake player、死亡中的玩家及 server shutdown 期間不建立離線身體。
- 玩家重新登入時，若身體仍存活，玩家回到身體目前位置並恢復身體保存的生命、食物、物品、裝備、經驗與狀態效果。
- 身體存在期間，玩家不得因 playerdata 舊副本取回重複物品。
- 身體死亡時，死亡流程只執行一次。
- 身體死亡後，玩家登入不得取回死亡前背包，除非 gamerule 或 Remnant 設定明確保留。
- 死亡背包啟用時，離線身體死亡會在身體死亡位置建立死亡背包，且不額外掉落同一批物品。
- 死亡背包回收後，相關死亡節點依 Nexus 規格失效或隱藏。
- Discord Bridge 可收到離線死亡事件，但訊息不包含座標或背包內容。
- PVP 關閉時，玩家不能用直接攻擊傷害其他玩家的離線身體。
- 身體所在區塊卸載時，預設不強制載入區塊；重新載入後身體仍存在。
- Server 正常關閉不會替所有線上玩家建立新身體；已存在身體會在重啟後恢復。
- playerdata body lock、SavedData 與 entity 不一致時，Server 進入 recovery，不直接套用可能造成複製的背包。
- OP 可列出、傳送、移除或修復離線身體。
- 偽造封包不能建立、移除、修改或搜刮離線身體。
- Dedicated Server 啟動時不載入離線身體 client renderer。
