# Death Backpack Delta Specification

## ADDED Requirements

### Requirement: Pre-drop authoritative capture

When a non-spectator ServerPlayer dies with `keepInventory=false`, the system SHALL capture eligible ItemStacks from the player's authoritative Inventory before vanilla emits those stacks as ItemEntity instances.

#### Scenario: Normal inventory capture

- **GIVEN** a player has eligible items in main inventory, hotbar, armor or offhand slots
- **WHEN** vanilla death processing reaches the `Inventory.dropAll()` invocation
- **THEN** the system SHALL copy those stacks directly into one death backpack
- **AND** the captured slots SHALL not subsequently be emitted by vanilla
- **AND** the resulting ItemStacks SHALL preserve count and Data Components

#### Scenario: Keep inventory

- **GIVEN** `keepInventory=true`
- **WHEN** the player dies
- **THEN** no death backpack capture SHALL run
- **AND** the player's inventory SHALL remain governed by vanilla behavior

### Requirement: Backpack nesting exclusion

The system SHALL NOT place any DeadRecall tiered backpack or death backpack inside a newly created death backpack.

#### Scenario: Player carries a backpack

- **GIVEN** the player inventory contains normal items and one or more DeadRecall backpacks
- **WHEN** direct death capture succeeds
- **THEN** normal items SHALL be stored in the new death backpack
- **AND** backpack items SHALL remain in the player Inventory until vanilla `dropAll()` emits them normally

### Requirement: Transactional fallback

Direct capture SHALL be transactional and SHALL fail back to vanilla death drops without deleting player items.

#### Scenario: Capture failure before completion

- **GIVEN** the system has created a capture snapshot
- **WHEN** death-backpack entity creation, death-node creation or binding throws or fails
- **THEN** any incomplete death-backpack entity SHALL be discarded
- **AND** all captured stacks SHALL be restored to the player Inventory
- **AND** vanilla `Inventory.dropAll()` SHALL continue
- **AND** the legacy post-drop collector MAY run as a compatibility fallback

### Requirement: Legacy collector isolation

The old nearby-ItemEntity collector SHALL NOT run after a successful direct capture.

#### Scenario: Direct capture completed

- **GIVEN** direct capture created and bound a death backpack successfully
- **WHEN** the existing AFTER_DEATH handler executes
- **THEN** it SHALL consume the direct-capture completion marker
- **AND** clear the player's pending nearby-drop snapshot
- **AND** cancel the legacy radius scan

### Requirement: Server authority

All death inventory capture, mutation, rollback, death-node creation and ItemEntity creation SHALL execute on the logical Server thread. The Client SHALL NOT provide captured contents or decide which slots are eligible.

## MODIFIED Requirements

### Requirement: Death backpack source data

The primary source for death-backpack contents SHALL be the player's authoritative slot data rather than ItemEntity instances found near the death location. Nearby ItemEntity scanning SHALL only remain as a temporary fallback until compatibility verification permits its removal.
