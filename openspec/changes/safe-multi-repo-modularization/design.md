# Design: Safe Multi-Repository Modularization

## 1. Target repository topology

```text
TotemCore
├── TotemDiscordBridge
├── TotemRemnant
├── TotemAutomata
└── TotemNexus

DeadRecall compatibility bundle
├── pins one tested version of every required module
├── preserves the existing single-install experience
├── owns cross-repository integration and migration tests
└── temporarily retains gameplay that has no approved repository owner
```

Feature repositories may depend on `TotemCore`. They must not directly depend on another feature repository. Optional cross-feature behavior is implemented by versioned Core events or by a small optional integration adapter owned by the consumer.

The machine-readable initial ownership map is [`repository-map.json`](repository-map.json).

## 2. Compatibility invariants

Repository extraction must not by itself change:

- the `deadrecall` registry namespace;
- item, block, effect, recipe, criterion, tag or creative-tab IDs;
- Payload IDs or their codecs;
- SavedData keys, `data_version` fields or codec meaning;
- translation keys, config paths or command behavior;
- existing addon API behavior;
- the ability to upgrade an existing world through the DeadRecall bundle.

[`compatibility-surface.txt`](compatibility-surface.txt) is the Phase 0 snapshot of visible `deadrecall:*` identifiers and packaged `assets/deadrecall` / `data/deadrecall` paths. CI compares the current source tree with this snapshot. Once files live in other repositories, the same snapshot must be checked against the assembled compatibility bundle instead of only the DeadRecall source tree.

Removing or renaming an entry requires a separate OpenSpec change with owner, migration, rollback and assembled-bundle evidence. Updating the baseline merely to make CI green is forbidden.

## 3. Extraction protocol

Every feature repository uses the same sequence:

1. **Inventory** — identify implementation, API, Payload, Mixin, client, resource, GameTest and persistent identifiers.
2. **Seam** — replace direct feature-to-feature calls with a Core API/event or an optional adapter while code still lives in DeadRecall.
3. **Copy** — create the new repository from an isolated temporary history export. Never rewrite the active DeadRecall repository history.
4. **Dual validation** — build the new module independently and as part of the DeadRecall bundle while the old implementation remains available behind one bootstrap choice.
5. **Cutover** — make the bundle load the new module artifact and prove that only one implementation registers each identifier, receiver, event and Mixin.
6. **Deletion** — remove the old implementation only after standalone, bundle, migration, restart and Dedicated Server gates pass.
7. **Observation** — keep versions lockstep and retain rollback compatibility for at least two DeadRecall releases before independent versioning.

At no point may two implementations register the same registry or Payload ID in one runtime.

## 4. Initial ordering

### TotemCore

Core is created first but contains no gameplay. Its first release provides only:

- versioned public API package conventions;
- event registration and lifecycle contracts;
- migration/version helpers;
- common identifier and permission primitives that are proven to have at least two consumers.

Existing death addon interfaces remain compatible. Moving or replacing their package requires forwarding types and a separate addon migration window.

### TotemDiscordBridge pilot

Discord Bridge is the first physical extraction because it has a relatively clear service boundary and no world registry content. It validates cross-repository checkout, Core dependency, bundle assembly, secrets, config migration and release pinning before persistent gameplay moves.

### TotemRemnant

Before extraction, death backpack creation/recovery must stop calling Nexus implementation directly. Remnant publishes versioned death/backpack lifecycle events; Nexus and Discord adapters subscribe without mutating Remnant private state.

### TotemAutomata

Copper Golem implementation, payloads, client screens, Mixin and restart probes move together. Shared `ModItems`, `ModMenus`, language files and client bootstrap must first be split into owner-specific registration classes.

### TotemNexus

Nexus moves last among current gameplay modules because it owns the largest connected group of SavedData, Payload, client UI, teleport sessions and Mixin accessors. All existing SavedData keys and `deadrecall:*` identifiers remain readable.

## 5. Unassigned gameplay

Alchemy, enchanting changes, recipe overrides, concrete-powder behavior, portable-container policy and other legacy gameplay remain in DeadRecall until each has an approved bounded-context repository. They must not be placed in TotemCore merely to empty DeadRecall.

## 6. Build and release contract

Initial module releases use the same Minecraft and Java compatibility line and are pinned by an immutable manifest in DeadRecall. A compatibility build must resolve exact module versions; floating ranges such as `latest` are forbidden.

Each module repository eventually runs:

- Java 25 compile and unit tests;
- relevant Fabric Server GameTests;
- Dedicated Server client-class isolation checks;
- its own restart/migration probes when it persists world state;
- a compatibility contract check against the Core API version.

DeadRecall additionally runs the assembled bundle, legacy-world fixtures and cross-module combinations. Independent Modrinth publishing is enabled only after the bundle has consumed two successful lockstep releases.

## 7. Installation matrix

For each extracted feature, CI must cover:

| Installation | Expected result |
|---|---|
| Core only | Starts without gameplay registration |
| Core + feature | Feature starts and passes its own tests |
| DeadRecall bundle | Preserves current all-in-one behavior |
| Core + feature without unrelated feature | Starts without class-loading or Mixin failure |
| Legacy world + DeadRecall bundle | Loads without missing registry or SavedData loss |
| Dedicated Server | Loads no client-only classes |

Pairwise feature combinations are required only where an explicit integration exists. The full bundle remains the authoritative end-to-end combination.

## 8. Rollback

- Every cutover is one feature and one reversible PR.
- The old DeadRecall implementation is not deleted in the same commit that first consumes a new external module.
- Persistent schema changes are prohibited during repository movement.
- The compatibility manifest can pin the previous known-good module artifact without changing world data.
- Repository history extraction occurs only in a temporary clone or mirror; the source repository is never force-rewritten.
