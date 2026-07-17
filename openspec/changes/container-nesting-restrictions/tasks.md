# Tasks: Container Nesting Restrictions

## Policy

- [x] Add a central portable-container classification service.
- [x] Add predicates for Bundle, Shulker Box and DeadRecall backpack containers.
- [x] Implement a bidirectional denied insertion matrix.
- [x] Add data-driven tags for addon portable containers.

## Integration

- [x] Enforce the policy in DeadRecall backpack slots and quick-move logic.
- [x] Enforce the policy in Bundle insertion paths through the vanilla container-item hook.
- [x] Enforce the policy in Shulker Box menu, sided insertion and mod-owned direct Container transfer paths.
- [x] Cover hopper, hopper minecart, dropper and dispenser transfers through the Shulker Box sided-insertion contract.
- [x] Apply the same rule to death capture and transient-stack fallback.
- [x] Verify restricted transient-container rollback remains exactly once.
- [x] Add localized rejection messages and rate-limited automation diagnostics.

## Legacy data

- [x] Preserve existing nested contents on load.
- [x] Allow extracting invalid nested items from DeadRecall backpacks.
- [x] Reject reinsertion without deleting or rewriting the stack in DeadRecall backpack menus.
- [x] Add a read-only administrator report for invalid nesting in online player inventories, cursors, crafting inputs and currently open menus.
- [x] Bound diagnostic traversal by depth, scanned-stack count and retained findings without mutating Components.

## Tests

- [x] Backpack into Bundle and Bundle into backpack policy/runtime-hook coverage.
- [x] Backpack into every Shulker Box color and reverse-direction policy coverage.
- [x] Drag, shift-click, number-key, double-click and cursor interaction matrix.
- [ ] Hopper, hopper minecart, dropper and dispenser fixture matrix.
  - [x] Real Hopper-to-Shulker rejection and ordinary-item control GameTests.
  - [x] Real Dropper-to-Shulker rejection and ordinary-item control GameTests.
  - [x] All six Shulker sided-insertion faces reject normal and death backpacks.
  - [x] Shared Shulker sided-insertion guard used by hopper minecart and other container-targeting automation paths.
  - [ ] Dedicated Hopper Minecart and Dispenser fixtures.
- [x] Death capture and rollback exactly-once tests for Bundle and Shulker Box stacks.
- [x] Existing invalid nesting load/extract/reinsert fixture test.
- [x] Custom Data Components and named container preservation test.
- [x] Read-only diagnostics cover both nesting directions, clean contents, mutation safety and depth truncation.
- [ ] Multiplayer race and Dedicated Server restart tests.
  - [x] Existing three-JVM Dedicated Server restart probe remains required by CI.
