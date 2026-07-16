# 死亡節點管理介面

DeadRecall 提供 OP／管理員專用的死亡節點管理介面，用於處理舊世界留下的孤立 Death Node、已回收但仍保留的停用紀錄，以及大量玩家死亡節點的資料整理。

## 開啟方式

需要 Minecraft 管理員指令權限：

```text
/deadrecall deathnodes
```

相容別名：

```text
/deadrecall deathpoints
```

指令必須由遊戲內玩家執行，Console 不會開啟 GUI。

## 篩選

介面可依下列條件篩選：

- 玩家：使用左右按鈕逐一切換節點 Owner，也可回到「全部玩家」。
- 狀態：全部、`ACTIVE`、`DISABLED`。

每筆資料會顯示：

- 死亡節點自動名稱。
- Owner 名稱；離線且無可用名稱時顯示 UUID 前 8 碼。
- Dimension 與方塊座標。
- 節點 UUID 前 8 碼。
- 目前狀態。

## 安全操作

### 停用節點

`ACTIVE` 節點只能先執行「停用節點」。停用後：

- 節點不再出現在一般玩家的 Nexus 地圖。
- Space Unit record 仍保留，方便追查及避免立即破壞死亡背包 binding。
- 對應死亡背包若稍後被回收，不會重新啟用節點。

### 永久刪除

只有非 `ACTIVE` 節點可以永久刪除。按下「永久刪除」後必須再按一次「再次確認」。永久刪除會：

- 從 Space Unit SavedData 移除該 Death Node record。
- 從所有玩家的 discovery 與 favorite 集合移除對應 UUID。
- 寫入伺服器管理 log。

管理員不能直接永久刪除 `ACTIVE` 節點，必須先停用。

## 命名限制

Death Node 會使用自動名稱，例如：

```text
Death Echo 120, 64, -32
```

目前重新命名功能只適用於可管理的固定磁石 `LODESTONE`。Death Node 不支援重新命名，管理介面也不提供改名功能。
