# Tasks: Safe Multi-Repository Modularization

## 0. Safety contract and inventory

- [x] 0.1 Define one repository per major feature and retain DeadRecall as the compatibility bundle.
- [x] 0.2 Record the target dependency rule: Core has no feature dependency; features depend only on Core by default.
- [x] 0.3 Record the current `deadrecall:*` identifier and packaged-resource compatibility surface.
- [x] 0.4 Add an automated CI check that rejects an unexplained compatibility-surface change.
- [x] 0.5 Define the copy, dual-validation, cutover, deletion, observation and rollback protocol.
- [x] 0.6 Record current shared seams and unassigned gameplay instead of assigning them to Core.

## 1. Internal boundaries inside DeadRecall

- [x] 1.1 Split the server initializer into owner-specific bootstrap classes without changing registration order or behavior.
- [x] 1.2 Split client initialization by feature ownership.
- [x] 1.3 Split Payload type/receiver registration by feature ownership.
- [x] 1.4 Split Server and Client Mixin configs by future module ownership while keeping one artifact.
- [x] 1.5 Split shared registry holders and resource generation into owner-specific registration classes.
- [ ] 1.6 Add a dependency-boundary check for direct feature-to-feature imports.
- [ ] 1.7 Run full Build, Validate, Server GameTests and both restart probes.

## 2. TotemCore repository

- [ ] 2.1 Specify the minimal Core API and explicitly reject gameplay classes.
- [ ] 2.2 Create the `TotemCore` repository without rewriting DeadRecall history.
- [ ] 2.3 Publish a versioned Core development artifact.
- [ ] 2.4 Add Core compatibility and Dedicated Server tests.
- [ ] 2.5 Document addon API versioning and deprecation policy.

## 3. TotemDiscordBridge pilot repository

- [ ] 3.1 Replace root-initializer Discord registration with a feature bootstrap.
- [ ] 3.2 Isolate Discord Payload, Mixin, client config UI, language and config ownership.
- [ ] 3.3 Create `TotemDiscordBridge` from a temporary filtered history export.
- [ ] 3.4 Validate Core + Discord standalone installation.
- [ ] 3.5 Consume the module from the DeadRecall compatibility bundle without duplicate registration.
- [ ] 3.6 Keep lockstep versions through the observation window before removing the old implementation.

## 4. TotemRemnant repository

- [ ] 4.1 Replace direct death-to-Nexus calls with versioned lifecycle events and optional adapters.
- [ ] 4.2 Assign backpack items, inventory, addon API, Trinkets adapter, Payload and death Mixin ownership.
- [ ] 4.3 Create `TotemRemnant` and preserve existing addon API compatibility.
- [ ] 4.4 Pass death capture/recovery, restart, legacy world, multi-player and Dedicated Server tests.
- [ ] 4.5 Cut the bundle over and remove the old implementation only after dual validation.

## 5. TotemAutomata repository

- [ ] 5.1 Isolate copper item/menu/registry/client/Payload/Mixin registration.
- [ ] 5.2 Create `TotemAutomata` with no required Cognition dependency.
- [ ] 5.3 Pass sorting, gathering, pressure, restart and standalone installation tests.
- [ ] 5.4 Cut the bundle over and remove the old implementation only after dual validation.

## 6. TotemNexus repository

- [ ] 6.1 Isolate Space Unit, teleport, friend, death-node and distributed-spawn ownership.
- [ ] 6.2 Create `TotemNexus` while preserving all SavedData, Payload and resource IDs.
- [ ] 6.3 Pass teleport, privacy, multi-player, dimension, restart and legacy-world tests.
- [ ] 6.4 Cut the bundle over and remove the old implementation only after dual validation.

## 7. Remaining gameplay and compatibility bundle

- [ ] 7.1 Propose explicit repositories for each remaining bounded context.
- [ ] 7.2 Keep unassigned gameplay in DeadRecall and out of Core until approved.
- [ ] 7.3 Convert DeadRecall into an exact-version compatibility bundle and E2E test repository.
- [ ] 7.4 Validate at least two lockstep releases with rollback evidence.
- [ ] 7.5 Enable independent versions and Modrinth publishing for stable repositories.
