# Safe backpack actions

Owner: TotemRemnant. VanillaTweaks owns the configurable sort key.

## Implemented and validated

- Middle-click sorting routes to the hovered backpack storage, E-screen panel,
  chest storage or player inventory. Functional slots and the cursor are excluded.
- Sorting a chest/player surface first deposits loose items into ordinary
  backpacks on that same surface which already contain an exact matching item
  and Data Components, then sorts the remainder. Hotbar backpacks may receive
  player-main-inventory items; loose hotbar/equipment/offhand items are not drained.
- Backpacks stay pinned to their original objects and positions. Destinations are
  deterministic; full bags retain source remainders. Backpacks are never nested.
- Death backpacks open the production BackpackScreen and safely sort storage.
  One-click equip fills only empty HEAD/CHEST/LEGS/FEET slots, preserving all item
  components; occupied equipment is never overwritten or dropped.
- Hidden/inactive/overflow contents survive writes and close. Staged operations
  validate menu/state, ownership, item policies, capacity and backpack identity
  before mutation. Commit failures restore all sources and destinations.
- Plain-item compression is staged on detached contents; orphaned rows and custom
  components retain their original representation. Open/panel views refresh in
  the same tick so later writes cannot undo new deposits.
- Owner Observer provider protocol1 supports ordinary and death variants using
  the production renderer. Snapshots/cursors are monotonic for the exact variant;
  the death equip control and every mutation path stay disabled in read-only mode.

## Optional sorting contract v1

No hard Remnant/VanillaTweaks dependency is introduced.

- `totem:container_sort_client_v1` uses
  `BiFunction<AbstractContainerScreen<?>, Slot, Boolean>`: true consumes the
  configured sort action; false permits native fallback. Read-only input consumes
  without packets. The owner sends a bounded request.
- `totem:container_sort_server_v1` uses
  `BiFunction<ServerPlayer, Boolean, Boolean>`: the input boolean selects player
  storage; null means unowned, any non-null result consumes, including rejection.
- Remnant requests bind menu ID, state ID, hovered slot, selected inventory slot
  and expected backpack. Generic chest requests are rejected; chest matching
  deposits use the dedicated bound request. Unknown menu classes remain denied.

## Validation evidence (2026-10-15)

- Remnant: all 71 required server GameTests; unit/build checks; formal clients with
  explicit VanillaTweaks-present and -absent assertions; separate-JVM seed/reload.
- VanillaTweaks: 24 server GameTests, 10 unit tests and build; actual key/mouse
  integration exercised by the Remnant formal client. Main commit: `bd75fbf`.
- Native three-JVM owner-UI scoped E2E passed, including ordinary and death
  backpack initial/later snapshots, rendered remote cursor, disabled death equip,
  variant close, Stop and server cleanup. Death screenshot reviewed visually.
- The owner-UI profile excludes only the unrelated account-v1 admission test
  coordinator and visibility client. It does not modify production authentication
  or whitelist rules. Full account-v1 E2E still has an unresolved policy close1008
  and is not reported as passing.
- Independent review cleared production transactions/routing, rollback failure
  cases, test runner integrity and both backpack E2E phases.
- Source/JAR identities, logs, scoped profile, test-fixture patch and screenshots
  are preserved locally at: /tmp/remnant-mainline-evidence-20261015-xehf6_7d
- Reusable server/client/restart runner: `scripts/verify-safe-backpack-actions.sh`.
  It rejects reused artifact directories and failure markers; both client runs
  assert optional-module presence. Its manual CI workflow has not been dispatched.
- No version bump or publication is part of this integration.

## Remaining development plan

1. Bundle-style direct item interaction: inspect current native Bundle slot,
   cursor and tooltip semantics; add left/right deposit/withdraw and wheel item
   selection while preserving whole-backpack pickup, carried-stack limits,
   anti-nesting, server identity checks and Observer input suppression.
2. Other workstation sidebars: crafting/enchanting and other supported native
   screens expose real backpack slots. Preserve functional slot indices,
   quick-move and recipes; provide native owner semantic Observer reconstruction.
3. Death sidebar: allow explicit death-backpack selection and storage operations,
   preserve owner/recovery lifecycle and access to large/overflow contents.
4. Embedded material sourcing: the existing embedded crafting grid may stage
   recipe ingredients from permitted backpack slots. Determine the actual
   enchanting UI owner/existence before adding its integration. Validate target
   item, lapis, XP and owner costs server-side; consume output ingredients once.

For each next stage, narrow ownership/contracts with intelligence, stage and
validate atomic mutations, cover full/partial/changed inputs, repeated actions,
close/reopen and reload, then perform focused client/Observer tests and review.
These four stages are not implemented by the safe-actions mainline integration.
