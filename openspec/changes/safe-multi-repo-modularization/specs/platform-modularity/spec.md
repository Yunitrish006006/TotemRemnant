# Delta Spec: Platform Modularity

## ADDED Requirements

### Requirement: Major features have independent repositories

Each major Totem feature MUST have an independent repository and build artifact. DeadRecall MUST remain available as a compatibility bundle until the migration and observation requirements are complete.

#### Scenario: A feature is extracted

- **WHEN** a DeadRecall feature moves to a new repository
- **THEN** the new repository owns its implementation, tests, resources and release workflow
- **AND** DeadRecall pins an exact tested module version
- **AND** users may still install the DeadRecall compatibility bundle

### Requirement: Repository dependencies remain acyclic

TotemCore MUST NOT depend on a feature repository. Feature repositories MUST depend only on TotemCore by default, and cross-feature integrations MUST use versioned public APIs, events or optional adapters.

#### Scenario: Remnant reports a death-backpack event

- **WHEN** Remnant creates or recovers a death backpack
- **THEN** it publishes a versioned lifecycle event without importing Nexus or Discord implementation classes
- **AND** optional Nexus or Discord adapters may subscribe when installed
- **AND** Remnant works when those modules are absent

### Requirement: Repository movement preserves compatibility identifiers

Moving code between repositories MUST NOT silently remove or rename existing `deadrecall:*` registry, Payload, SavedData or resource identifiers.

#### Scenario: A compatibility-surface entry changes

- **WHEN** CI detects a missing or renamed baseline entry
- **THEN** the build fails
- **AND** the baseline remains unchanged until an owning-module migration and rollback path are approved

#### Scenario: A module is assembled into the compatibility bundle

- **WHEN** the implementation no longer resides in the DeadRecall source tree
- **THEN** the assembled bundle is checked against the same compatibility surface
- **AND** exactly one implementation owns each registration

### Requirement: Extraction is staged and reversible

Each feature MUST pass inventory, seam, copy, dual-validation, cutover, deletion and observation stages. Persistent schema changes MUST NOT be combined with repository movement.

#### Scenario: A new external module is consumed for the first time

- **WHEN** DeadRecall first loads the external module artifact
- **THEN** the old implementation remains recoverable
- **AND** the same commit does not permanently delete both rollback code and old compatible artifact selection
- **AND** CI proves that no identifier, Payload receiver, event or Mixin is registered twice

### Requirement: Standalone and bundle installations are validated

An extracted module MUST work with Core without unrelated features, and the DeadRecall bundle MUST preserve the current integrated behavior.

#### Scenario: An optional feature is absent

- **WHEN** a player installs Core and one feature without other Totem features
- **THEN** the game starts without class-loading or Mixin failure
- **AND** the installed feature retains its supported behavior

#### Scenario: A legacy world is opened through the bundle

- **WHEN** an existing DeadRecall world is opened after module extraction
- **THEN** existing registries and SavedData load without loss
- **AND** restart and migration tests pass before release
