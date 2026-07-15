# Design: Teleport Interface Item Specializations

## Goals

- 讓四種原版物品都能作為 Nexus 傳送介面入口。
- 每種物品有清楚且不重疊的定位。
- 保持 Server authoritative，不信任 Client 提交的物品、地圖或加成資料。
- 不破壞普通羅盤既有的磁石管理、探索與好友互動流程。
- 讓既有成本、紫水晶催化、偏差、安全落點與結構磨損公式可組合且可測試。

## Interface types

```java
COMPASS
RECOVERY_COMPASS
BOOK
FILLED_MAP
```

物品對應：

| 介面類型 | 原版物品 | 定位 |
|---|---|---|
| `COMPASS` | `minecraft:compass` | 通用與管理工具 |
| `RECOVERY_COMPASS` | `minecraft:recovery_compass` | 死亡節點精準化 |
| `BOOK` | `minecraft:book` | 固定磁石路線規劃 |
| `FILLED_MAP` | `minecraft:filled_map` | 地圖覆蓋區域導航 |

空白地圖 `minecraft:map` 沒有 Dimension、中心與比例尺資料，初期不視為有效傳送介面。

## Interaction capability matrix

| 行為 | 普通羅盤 | 回生羅盤 | 書本 | 已繪製地圖 |
|---|---:|---:|---:|---:|
| 右鍵空氣開啟玩家來源傳送介面 | 是 | 是 | 是 | 是 |
| 右鍵已註冊磁石開啟磁石來源傳送介面 | 是 | 是 | 是 | 是 |
| 註冊未註冊磁石 | 是 | 否 | 否 | 否 |
| 左鍵探索／啟用磁石 | 是 | 否 | 否 | 否 |
| 右鍵玩家邀請／接受好友 | 是 | 否 | 否 | 否 |
| 變更磁石名稱、可見性、名單與校準 | 是 | 否 | 否 | 否 |

非普通羅盤右鍵未註冊磁石時，Server 應提示需要普通羅盤，不得建立 pending registration。

## Server-side interface context

開啟介面時，Server 建立短期 `TeleportInterfaceContext`：

```text
player_id
interface_type
source_type
source_id
interaction_hand
map_id?              // 只有 FILLED_MAP
created_game_time
expires_game_time
```

要求：

- Context 只存在 Server 記憶體，不需要永久 SavedData。
- Client 可回傳 context token 或來源 UUID，但不得提交可信的加成數值。
- 開始傳送前，Server 重新檢查玩家仍持有相符介面物品。
- `FILLED_MAP` 必須重新解析相同 map ID；其他介面至少需維持相同介面類型。
- `TeleportSession` 保存已驗證的 `interfaceType` 與必要的 map ID。
- 倒數與完成前若介面物品不再有效，取消傳送並使用專用取消原因。
- 同一趟傳送只採用開啟介面的那一件物品，不讀取另一隻手的其他介面物品。

## Quote calculation order

建議固定順序：

1. 解析來源、目標、權限、探索與路線基礎穩定度。
2. 計算目標類型懲罰、距離、Dimension 與石碑結構效果。
3. 套用既有紫水晶催化折抵。
4. 套用單一傳送介面特化。
5. 套用最小值、最大值與整數捨入。
6. 由 Server 回傳最終報價並驗證玩家資源。

Client 只顯示 Server 回傳的最終結果與加成說明。

## Ordinary compass specialization

普通羅盤為基準介面：

```text
food_cost_multiplier = 1.00
deviation_multiplier = 1.00
prepare_time_multiplier = 1.00
structure_wear_multiplier = 1.00
```

它不提供數值優惠，但擁有完整磁石與好友互動能力，並作為所有其他介面效果的回歸基準。

## Recovery compass specialization

當且僅當目標為請求玩家可見且屬於自己的 `DEATH` Space Unit：

```text
final_deviation = floor(base_deviation × 0.50)
```

規則：

- 最終偏差不得小於 0。
- 只調整 Server 安全落點搜尋使用的偏差上限。
- 不降低食物、紫水晶、準備時間、抵達傷害或結構磨損。
- 對 `LODESTONE`、`PLAYER`、`TEMPORARY` 與 `SYSTEM` 目標不提供加成。

## Book specialization: Route Codex

書本定位為「路線典籍／儀式規劃」。當目標為玩家已探索且有權限查看的 `LODESTONE`：

```text
final_prepare_ticks = max(30, ceil(base_prepare_ticks × 0.80))
final_structure_wear_chance = floor(base_structure_wear_chance × 0.75)
```

規則：

- 準備時間降低 20%。
- 對本次路線涉及的固定石碑結構磨損機率降低 25%。
- 若來源為玩家、死亡節點或其他非固定端點，只降低實際存在之固定目標石碑的磨損風險。
- 不降低食物、紫水晶或傳送偏差。
- 對 `PLAYER`、`DEATH`、`TEMPORARY` 與 `SYSTEM` 目標不提供加成。
- 普通書本不寫入路線 NBT，也不消耗耐久或數量。

## Filled map specialization

### Coverage validation

Server 從 map ID 取得地圖資料，並驗證：

- 地圖資料存在。
- 地圖 Dimension 與目標目前 Dimension 相同。
- 目標 Server 座標位於該地圖中心、比例尺及覆蓋邊界內。

目標可為 `LODESTONE`、`DEATH`、`PLAYER`、`TEMPORARY` 或 `SYSTEM`。對動態 `PLAYER` 目標，Server 使用即時精確位置判斷，但 Client 只收到「加成有效／無效」，不得收到精確座標。

### Bonus

地圖覆蓋條件成立時：

```text
final_food_cost = max(1, ceil(base_food_cost × 0.80))
final_deviation = floor(base_deviation × 0.80)
```

規則：

- 食物等價成本降低 20%；此值再分配至飽和度、飢餓值及物品食物。
- 偏差降低 20%。
- 不降低跨 Dimension 必須支付的紫水晶碎片；紫水晶仍由現有公式與催化折抵處理。
- 創造模式維持不消耗資源。
- 地圖未覆蓋目標時仍可作為傳送介面，但不提供加成。
- 若開始或完成前 map ID 不再有效，或動態目標移出覆蓋範圍，Server 應取消並要求重新取得報價，不得在玩家不知情時改用較高成本扣款。

## Rounding and minimums

- 成本乘數使用 `ceil`，避免低成本因折扣變成 0。
- 偏差與磨損機率使用 `floor`，並 clamp 至合法範圍。
- 準備時間使用 `ceil`，書本特化最低 30 ticks。
- 所有最終值仍受既有全域安全上限約束。

## Networking and UI

Server 報價至少新增：

```text
interface_type
interface_bonus_active
interface_bonus_reason
base_food_cost
final_food_cost
base_prepare_ticks
final_prepare_ticks
base_deviation
final_deviation
base_structure_wear_chance
final_structure_wear_chance
```

可分階段導入；第一階段可只回傳 `interface_type`、active flag 與既有最終欄位，後續再補完整比較明細。

介面要求：

- 顯示目前使用的傳送介面物品圖示與名稱。
- 顯示加成是否有效及條件，例如「死亡節點精準化」、「路線典籍」、「目標位於地圖範圍」。
- 無效時顯示原因，但不得暴露未探索節點或好友精確位置。
- 非普通羅盤開啟時，隱藏或停用管理、註冊、探索與好友操作。

## Security and privacy

- Server 從玩家實際手持 ItemStack 解析介面類型。
- Server 從 map ID 與世界地圖資料計算覆蓋範圍。
- Client 不得提交可信座標、map center、scale、Dimension、倍率或最終報價。
- 地圖加成判斷不得使未探索或無權限節點出現在列表中。
- 對 `PLAYER` 目標只能回傳加成狀態，不回傳用於判斷的精確位置。
- 傳送完成前再次驗證介面、目標、成本與安全落點。