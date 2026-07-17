# Copper Golem sorting regression

Copper Golem sorting uses real Server GameTests rather than duplicated model-only logic. The fixtures use real Copper Golem entities, chest BlockEntities, remembered source data, destination bindings, Item Components and server chunk loading.

## Covered transactions

`CopperGolemTransportGameTest` verifies the base transaction:

- pickup is capped at 16 items;
- rollback restores the remembered source exactly once;
- destination insertion preserves source quantity after pickup;
- Home deposit preflight is non-mutating;
- gathering tool damage and final break are atomic.

`CopperGolemSortingRegressionGameTest` verifies the sorting lifecycle:

- an unchanged blocked snapshot stays blocked;
- source inventory, destination inventory or binding changes invalidate the blocked snapshot;
- a matching item is inserted into a DeadRecall backpack inside the bound chest before an outer chest slot is used;
- a DeadRecall backpack carried as cargo cannot be nested into the destination backpack;
- removing the final destination returns carried cargo to the remembered source exactly once and clears the source transaction;
- a destination in an unloaded chunk is retained instead of being treated as deleted;
- the retained binding resolves after the chunk and container load;
- a loaded binding is pruned only after its container is actually removed.

## Chunk semantics

Binding pruning is deliberately load-aware. `ServerLevel.isLoaded` is checked before container validation. An unloaded destination is therefore an unknown-but-retained binding, not a missing container. Once loaded, normal container validation resumes.

The GameTest covers the unloaded-to-loaded transition in one server process. Copper Golem persistence across separate Dedicated Server JVM processes remains part of OpenSpec task 13.7.
