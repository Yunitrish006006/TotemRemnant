# Totem Nexus／Space Unit

最新的 DeadRecall 已加入 Totem Nexus 的第一階段實作。此系統使用磁石、羅盤與 Space Unit 世界資料，讓玩家探索可見節點、開啟相對位置地圖，並以伺服器計算的成本與安全條件進行傳送。

## 文件

- [磁石註冊、探索與羅盤操作](space-units.md)
- [Space Unit 地圖](map.md)
- [傳送成本與安全條件](teleportation.md)
- [目前完成與未完成內容](status.md)
- [完整 OpenSpec](../../openspec/specs/space-unit-lodestone/spec.md)

## 快速操作

| 操作 | 結果 |
| --- | --- |
| 手持羅盤右鍵未註冊磁石 | 建立私有磁石 Space Unit，並將羅盤綁定到該節點 |
| 手持羅盤右鍵已註冊磁石 | 綁定羅盤；已探索時以該磁石作為來源開啟地圖 |
| 手持羅盤左鍵已註冊磁石 | 將該節點標記為已探索 |
| 手持羅盤右鍵空氣 | 以玩家目前位置作為臨時來源開啟地圖 |
| 在地圖選擇節點並按傳送 | 送出伺服器驗證的傳送請求 |

磁石來源必須在玩家附近，預設檢查半徑為 8 格。Client 不能指定可信座標、成本或安全落點，這些資料全部由 Server 重新查詢與計算。