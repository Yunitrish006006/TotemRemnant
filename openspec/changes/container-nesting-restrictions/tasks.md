# Tasks: Container Nesting Restrictions

## Policy

- [ ] Add a central portable-container classification service.
- [ ] Add tags or predicates for Bundle, Shulker Box, DeadRecall backpack and addon containers.
- [ ] Implement a bidirectional denied insertion matrix.

## Integration

- [ ] Enforce the policy in DeadRecall backpack slots and quick-move logic.
- [ ] Enforce the policy in Bundle insertion paths.
- [ ] Enforce the policy in Shulker Box and generic container transfer paths.
- [ ] Cover hopper, hopper minecart, dropper and dispenser transfers.
- [ ] Apply the same rule to death capture, recovery and rollback.
- [ ] Add localized rejection messages and rate-limited automation diagnostics.

## Legacy data

- [ ] Preserve existing nested contents on load.
- [ ] Allow extracting invalid nested items.
- [ ] Reject reinsertion without deleting or rewriting the stack.
- [ ] Add an admin diagnostic command or report for invalid nesting.

## Tests

- [ ] Backpack into Bundle and Bundle into backpack.
- [ ] Backpack into every Shulker Box color and reverse direction.
- [ ] Drag, shift-click, number-key, double-click and cursor tests.
- [ ] Hopper, hopper minecart, dropper and dispenser tests.
- [ ] Death capture and rollback exactly-once tests.
- [ ] Existing invalid nesting load/extract/reinsert tests.
- [ ] Custom Data Components and named container preservation tests.
- [ ] Multiplayer race and Dedicated Server restart tests.