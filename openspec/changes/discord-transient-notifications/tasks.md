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

- [ ] 3.1 Route temporary events through a handler that captures Discord message IDs.
- [ ] 3.2 Enqueue one delayed deletion job per successfully-sent message.
- [ ] 3.3 Add a Queue consumer that deletes messages after 600 seconds.
- [ ] 3.4 Treat Discord 204 and 404 as terminal success; retry 429 and transient 5xx failures.
- [ ] 3.5 Add producer/consumer queue bindings to Wrangler configuration.
- [ ] 3.6 Document queue creation and deployment requirements.

## 4. Verification

- [x] 4.1 DeadRecall Java 25 build and tests pass.
- [ ] 4.2 Discord Worker syntax/tests pass.
- [x] 4.3 DeadRecall PR Actions pass.
- [ ] 4.4 Worker deployment smoke test confirms a temporary message is deleted after 10 minutes.

## Blocker

The connected GitHub App can read `Yunitrish006006/discord-bot`, but all branch and file write operations currently return HTTP 403. Worker tasks remain open until that repository is added to the GitHub App installation with write access.
