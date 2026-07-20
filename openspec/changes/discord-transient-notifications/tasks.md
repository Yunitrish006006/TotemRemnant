# Tasks: Discord Transient Notifications

## 1. Contract

- [x] 1.1 Define the temporary event allowlist.
- [x] 1.2 Fix the lifetime at 600 seconds.
- [x] 1.3 Specify that the Worker remains authoritative and older mod payloads remain compatible.

## 2. DeadRecall

- [x] 2.1 Add `delete_after_seconds: 600` to temporary text event payloads.
- [x] 2.2 Add `delete_after_seconds: 600` to server status payloads.
- [x] 2.3 Add payload-policy unit tests.

## 3. Discord Worker

- [x] 3.1 Route temporary events through a handler that captures Discord message IDs.
- [x] 3.2 Enqueue one delayed deletion job per successfully-sent message.
- [x] 3.3 Add a Queue consumer that deletes messages after 600 seconds.
- [x] 3.4 Treat Discord 204 and 404 as terminal success; retry 429 and transient 5xx failures.
- [x] 3.5 Add producer/consumer queue bindings to Wrangler configuration.
- [x] 3.6 Document queue creation and deployment requirements.

## 4. Verification

- [x] 4.1 DeadRecall Java 25 build and tests pass.
- [x] 4.2 Discord Worker syntax/tests pass.
- [x] 4.3 DeadRecall PR Actions pass.
- [x] 4.4 Worker deployment smoke test confirms a temporary message is deleted after 10 minutes.

## Evidence

- Worker implementation: [`Yunitrish006006/discord-bot#1`](https://github.com/Yunitrish006006/discord-bot/pull/1), merged as `112f0fe`.
- Production secret alias compatibility: [`Yunitrish006006/discord-bot#2`](https://github.com/Yunitrish006006/discord-bot/pull/2), merged as `9467b5e`.
- Production smoke observability and zero-downtime API key rotation: [`Yunitrish006006/discord-bot#3`](https://github.com/Yunitrish006006/discord-bot/pull/3), merged as `31a00ce`.
- Worker unit tests: 18/18 pass, covering the allowlist, 600-second enqueue policy, failure isolation, deletion retry/terminal states, deployed secret aliases and API key rotation.
- Worker bundle verification: `wrangler deploy --dry-run` passes with the D1 and Queue producer/consumer bindings recognized.
- GitHub Actions: Worker `Validate` passed 2/2 checks on all three pull requests.

## Production deployment verification

- Created `discord-message-deletions` with 86,400-second free-tier retention; Cloudflare reports one producer and one consumer.
- Deployed Worker `main` merge commit `31a00ce`; `/health` returned HTTP 200 before and after smoke-test cleanup.
- An authenticated allowlisted `player_join` request returned HTTP 200 with `sent: 1`, `failed: 0`, and `deletionScheduled: 1`.
- The Worker logged `discord_message_deletion_scheduled` for message `1528233874881118401` at `2026-07-19T02:55:50Z` with `delay_seconds: 600`.
- The Queue consumer logged `discord_message_deletion_terminal` for the same message at `2026-07-19T03:06:03Z` with Discord HTTP 204, 613.218 seconds after scheduling.
- The temporary canonical smoke-test API key was deleted from Cloudflare and its local file removed; the original deployed secret aliases remain configured.
