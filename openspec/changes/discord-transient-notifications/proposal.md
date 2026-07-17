# Proposal: Discord Transient Notifications

## Summary

玩家加入、首次加入、死亡背包建立、死亡背包回收，以及 Dedicated Server 狀態通知屬於短期觀測訊息。這些通知送達 Discord 後保留 10 分鐘，之後由 Discord Bot 自動刪除，避免狀態頻道長期堆積。

## Scope

Temporary notifications:

- `player_join`
- `player_first_join`
- `death_backpack_created`
- `death_backpack_recovered`
- `POST /api/mc/server/status` 所產生的 Discord 狀態訊息

Not temporary:

- 玩家離開
- 玩家聊天與死亡訊息
- advancement
- 管理稽核與健康告警
- Space Unit、Boss、raid、村民升級

## Contract

DeadRecall MAY send `delete_after_seconds: 600` for temporary text events and server status reports. The Worker/Bot remains authoritative and MUST apply a 600-second lifetime to the supported temporary event set even when older DeadRecall versions omit the field.

After Discord returns a created message ID, the Worker MUST enqueue `{ channel_id, message_id }` into a delayed deletion queue. The queue consumer deletes the message after 600 seconds. A missing message is treated as already deleted; rate limits and transient Discord failures are retried without affecting the Minecraft server.

## Compatibility

Workers that do not understand `delete_after_seconds` ignore the field, so the Minecraft request remains backward-compatible. Actual automatic deletion requires the updated `discord-bot` Worker deployment and its Cloudflare Queue binding.
