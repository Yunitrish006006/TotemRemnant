# Design: Discord Transient Notifications

## Architecture

```text
DeadRecall Server
  └─ POST event/status to Cloudflare Worker
       └─ Discord Create Message
            ├─ permanent event: finish
            └─ temporary event: enqueue deletion job (delay 600s)
                 └─ Queue consumer
                      └─ DELETE /channels/{channel_id}/messages/{message_id}
```

## Event policy

The Worker owns the temporary-event allowlist:

- `player_join`
- `player_first_join`
- `death_backpack_created`
- `death_backpack_recovered`
- all messages produced by `/api/mc/server/status`

The caller cannot make arbitrary events temporary by supplying an unrestricted duration. For supported events, the effective lifetime is exactly 600 seconds.

## Delivery and deletion

1. Resolve requested Discord channel IDs, falling back to configured sync channels and then the default environment channel.
2. Send the Discord message using the Bot Token.
3. Parse the returned Discord message object and retain only `channel_id` and `message_id`.
4. Publish one queue message per successfully-created Discord message with `delaySeconds: 600`.
5. The queue consumer calls Discord Delete Message.
6. HTTP 204 and 404 are terminal success states.
7. HTTP 429 and 5xx responses are retried with bounded delay.
8. Authentication, permission and malformed-job failures are logged without retry loops.

## Failure isolation

- Failure to enqueue deletion does not roll back the already-sent Discord notification.
- Failure to delete does not affect Minecraft gameplay or server lifecycle.
- No Discord token, webhook secret or API key is included in queue payloads or response diagnostics.

## Deployment

The `discord-bot` Worker requires a producer and consumer binding for `discord-message-deletions`. The queue must exist before deployment. DeadRecall does not directly call Discord and does not need Cloudflare credentials.
