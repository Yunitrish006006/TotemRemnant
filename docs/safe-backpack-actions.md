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
  deterministic; full bags retain source remainders. New backpack nesting remains forbidden.
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

## Authorized workstation/death sidebar scope

The exact native allowlist is InventoryMenu, CraftingMenu, EnchantmentMenu and
ChestMenu. Other UI families are deferred by the user (2026-10-15).
Workstation support means manually taking backpack items into vanilla functional
slots; automatic recipe sourcing and automatic XP/lapis deduction are excluded.

Ordinary and death backpacks share stable physical sidebar slots, client-local
scrolling, exact hovered sorting and close/recovery lifecycle. Death sorting may
relocate each exact preexisting captured backpack once without allowing new
nested insertion. Backpack components, hidden storage and overflow are retained.

Final owner-capture three-JVM validation and independent review passed. The
verified implementation is ready for the requested mainline integration. No
publication is requested.

## Bundle-style direct item interaction (verified)

Ordinary backpacks support native Bundle gestures in either cursor/slot
orientation: left-click with an item deposits it; right-click with an empty
cursor/target withdraws the selected stack. Empty-handed left-click still picks
up the whole backpack. Native wheel/hover/quick-move lifecycle selects/resets
items, with translated vanilla tooltip guidance. Death backpacks use the same sidebar slot operations and sorting; bundle-style
direct item gestures apply to ordinary backpacks.

CONTAINER is the only contents store. A bounded selected-index component is
synchronized and persisted. Server selection binds menu/state/slot/expected
stack and rejects dead players and spectators. Transfers stage contents, retain
inactive/overflow rows and source remainders, enforce destination limits and
portable-container policy, restore components/sources on failure, and refresh
open/panel views in the same tick. Observer input cannot mutate selection or
send packets.

Validation: all 77 server GameTests, native-input client with explicit
VanillaTweaks absence, server-confirmed original item totals, whole-bag
movement, Observer suppression, separate-JVM contents/selection reload, and
current-JAR owner-UI scoped three-JVM ordinary/death regression all passed.
Main JAR SHA256: `1ac755406fdcb5921e62b56ee3a9280f70ee3f2a00e0bfc87317a9865aa006df`.
Screenshots were visually reviewed; independent review cleared production
transactions, authority guards and final test barriers. Native double-click
collection remains vanilla behavior; the placement fixture uses a different
empty slot. Evidence: /tmp/remnant-bundle-evidence-20261015-6b3gvftb.

Bounded local client profile: `-PtotemBackpackBundleOnly=true`. The existing
full safe-actions profile/runner also includes this test. Neither profile
changes production authentication. Full account-v1 admission is still outside
this passing owner-UI scope. No version bump or publication was performed.

## Workstation/death sidebar final validation

Final safe-actions regression: all 81 GameTests passed (2026-10-15), including
captured-backpack death sorting, explicit constructors, native functional slot
indices, deep death contents, hovered sort routing, chest Shift-click boundaries
and close recovery. VanillaTweaks-present and -absent clients and separate-JVM
seed/reload all passed. Evidence: `test-artifacts/safe-actions-20261015223056`.

The native sidebar provider uses `remnant_sidebar@1` with inventory/crafting/
enchanting/chest variants. It snapshots native slots plus one explicit selected
bag (including offhand selection), reconstructs stable physical panel indices,
and uses detached target identity, XP, creative state and effects. Unsent recipe
search text is represented only by a boolean. The provider also implements
`Predicate<Screen>` as optional ownership contract v1: Observer must suppress
native adapters for a claimed screen even if capture fails or the peer lacks
owner transport. Module-present and fault-injected client checks passed in v12 and the final
artifact client rerun. Final JAR SHA256: `895f5a76e4a31ec28c0cf103e2ec43f49c1fd52d77808fdd61e8aeb8ff10f12f`.
Real-target-capture three-JVM validation passed for all eight ordinary/death
sidebar variants: initial/later contents, remote cursor, input suppression, target
close, Stop and dedicated-server cleanup. Evidence:
`/tmp/remnant-sidebar-owner-ui-e2e-final.log` and
`/tmp/observer-remnant-sidebar-integration/build/e2e/results`.

Final source identity matches all 255 entries recorded by the safe-actions runner.
Independent review cleared the final implementation, compatibility prerequisites
and actual validation evidence. Final crafting and death-chest E2E screenshots
were visually reviewed. Logs, profiles, source hashes, JARs and screenshots are
preserved locally at: /tmp/remnant-sidebar-integration-evidence-20261015-6v5eylbk
