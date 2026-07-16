# Container Safety Specification

## Requirement: Bidirectional portable-container restriction

The system SHALL reject insertion of a DeadRecall backpack into a Bundle, Shulker Box or configured restricted portable container, and SHALL reject insertion of those containers into a DeadRecall backpack.

### Scenario: Backpack into Bundle

- GIVEN a player holds a DeadRecall backpack
- WHEN the player attempts to insert it into a Bundle
- THEN the Server rejects the insertion
- AND the backpack remains unchanged and available to the player

### Scenario: Shulker Box into backpack

- GIVEN a Shulker Box ItemStack
- WHEN any transfer path attempts to insert it into a DeadRecall backpack
- THEN the Server rejects the transfer without consuming or duplicating the stack

## Requirement: Server-authoritative enforcement

The system SHALL enforce the same restriction for manual, automated and programmatic transfer paths.

### Scenario: Hopper transfer

- GIVEN a restricted portable container is available to a hopper
- AND the destination is a container that forbids that item
- WHEN the hopper attempts transfer
- THEN the transfer fails atomically
- AND source and destination counts remain unchanged

## Requirement: Legacy nested data preservation

The system SHALL preserve existing invalid nested data during load and synchronization.

### Scenario: Extract legacy nested item

- GIVEN an old-world backpack already contains a Shulker Box
- WHEN the player removes the Shulker Box
- THEN removal succeeds
- AND reinsertion into the backpack is rejected
- AND neither ItemStack is rewritten or deleted

## Requirement: Death transaction safety

The system SHALL exclude restricted portable containers from being recursively captured inside a DeadRecall backpack.

### Scenario: Player dies carrying a Bundle

- GIVEN a player dies while carrying a Bundle
- WHEN the death backpack transaction runs
- THEN the Bundle is not stored inside the death backpack
- AND it follows the configured fallback path exactly once
- AND transaction rollback cannot create a duplicate

## Requirement: Extensible classification

The system SHALL support data-driven tags or registered predicates for addon portable containers without requiring direct dependency on addon implementation classes.

## Requirement: Localized feedback

The system SHALL use translation keys for all player-visible insertion rejection messages.