# Death Backpack Delta Specification

## ADDED Requirements

### Requirement: Pre-drop authoritative capture

When a non-spectator `ServerPlayer` dies with `keepInventory=false`, the system SHALL capture eligible `ItemStack` instances from authoritative player-owned sources before vanilla emits them as `ItemEntity` instances.

Authoritative sources SHALL include vanilla Inventory／Equipment, active carried stack, player crafting inputs, explicitly whitelisted vanilla workstation inputs and registered addon-provider slots. Persistent block/entity storage and result previews SHALL remain excluded.

#### Scenario: Normal capture

- **GIVEN** eligible stacks exist in authoritative player-owned sources
- **WHEN** death processing enters `Player.dropEquipment(ServerLevel)` before vanilla or addon drops
- **THEN** those stacks SHALL be copied into one death backpack
- **AND** count and Data Components SHALL be preserved
- **AND** committed sources SHALL not emit duplicate world drops

#### Scenario: Keep inventory

- **GIVEN** `keepInventory=true`
- **WHEN** the player dies
- **THEN** DeadRecall capture SHALL not run
- **AND** vanilla and addon keep rules SHALL remain authoritative

### Requirement: Portable-container exclusion

Restricted portable containers SHALL NOT be placed inside a death backpack, regardless of whether they originate from vanilla, transient or addon slots.

#### Scenario: Restricted addon stack

- **GIVEN** an addon slot contains a restricted portable container
- **WHEN** addon snapshots are collected
- **THEN** the stack SHALL be excluded
- **AND** DeadRecall SHALL not clear that addon slot

### Requirement: Addon inventory transaction API

The system SHALL expose a Server-side provider／slot SPI for addon-owned player inventory.

Providers SHALL use unique stable identifiers and return only slots that their addon has authoritatively classified as death drops. Slots SHALL provide a stable source key, immutable snapshot, compare-and-clear commit and restore-if-empty rollback operation.

#### Scenario: Addon commit

- **GIVEN** an addon slot still matches its snapshotted Item, count and Data Components
- **WHEN** the transaction commits
- **THEN** the source SHALL be cleared exactly once
- **AND** its copied stack SHALL be stored in the death backpack

#### Scenario: Addon source changed

- **GIVEN** an addon source differs from its snapshot before commit
- **WHEN** compare-and-clear executes
- **THEN** the operation SHALL reject
- **AND** the complete death-backpack transaction SHALL roll back

#### Scenario: Provider snapshot failure

- **GIVEN** one provider throws or returns invalid snapshot data
- **WHEN** sources are collected
- **THEN** that provider SHALL be isolated and logged
- **AND** other providers and vanilla capture SHALL remain eligible to continue

#### Scenario: Optional addon absent

- **GIVEN** an optional addon is not installed
- **WHEN** DeadRecall initializes
- **THEN** its adapter class SHALL not be linked or initialized
- **AND** DeadRecall SHALL remain able to start

### Requirement: Trinkets drop-rule ownership

When Trinkets Updated is installed, DeadRecall SHALL use `forEachDroppable` and SHALL NOT reinterpret Trinkets death rules.

#### Scenario: Trinkets DROP

- **GIVEN** Trinkets returns a player slot as droppable
- **WHEN** DeadRecall captures it
- **THEN** count and Data Components SHALL be preserved
- **AND** the slot SHALL be cleared exactly once

#### Scenario: Trinkets KEEP or DESTROY

- **GIVEN** Trinkets resolves a slot to `KEEP` or `DESTROY`
- **WHEN** DeadRecall requests droppable slots
- **THEN** that slot SHALL not enter the DeadRecall transaction
- **AND** Trinkets SHALL retain authority over its outcome

### Requirement: Transactional fallback

Direct capture SHALL fail back to native death-drop paths without deleting or duplicating player items.

#### Scenario: Failure after source removal

- **GIVEN** one or more sources were removed
- **WHEN** addon commit, backpack entity creation, death-node creation or binding fails
- **THEN** incomplete backpack entities SHALL be discarded
- **AND** incomplete death nodes SHALL be disabled and discovery references removed
- **AND** vanilla slots SHALL be restored
- **AND** transient stacks SHALL be returned to Inventory
- **AND** cleared addon slots SHALL be restored in reverse order when possible
- **AND** addon stacks that cannot be restored in place SHALL be returned to Inventory
- **AND** native death processing SHALL continue

#### Scenario: Trinkets native fallback

- **GIVEN** a Trinkets `DROP` slot was cleared and restored by rollback
- **WHEN** Trinkets completes its native death handler
- **THEN** exactly one native loose drop SHALL be emitted
- **AND** the Trinkets source SHALL be empty
- **AND** no incomplete death backpack SHALL remain

### Requirement: Legacy collector removal

No nearby-`ItemEntity` collector, completion marker, radius scan, UUID-difference scan or replacement death-backpack path SHALL run after successful or failed direct capture.

#### Scenario: Later death callbacks

- **GIVEN** direct capture has either committed or rolled back
- **WHEN** later death callbacks execute
- **THEN** they SHALL not collect or discard nearby world drops
- **AND** they SHALL not create a second death backpack

### Requirement: Post-commit notification isolation

Discord, player-notification and logging failures SHALL NOT roll back a completed death-backpack transaction.

### Requirement: Server authority

All source selection, mutation, rollback, death-node creation and `ItemEntity` creation SHALL execute on the logical Server thread. The Client SHALL NOT provide captured contents or determine eligible slots.

## MODIFIED Requirements

### Requirement: Death backpack source data

The sole runtime sources for death-backpack contents SHALL be authoritative player-owned vanilla slots, explicitly supported transient inputs and registered addon-provider slots. Nearby `ItemEntity` scanning SHALL NOT participate in normal capture or failure fallback.
