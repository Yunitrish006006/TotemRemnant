# Copper Golem persistence testing

## Remote chunk unload and reload

`CopperGolemChunkPersistenceGameTest` creates a Copper Golem, Copper Chest Home and gathering target in a remote chunk that is initially unloaded.

The test force-loads the chunk, writes gathering state, then removes the force ticket and waits until `ServerLevel.isLoaded` confirms that the chunk is actually unloaded. It force-loads the same chunk again and resolves the Copper Golem by its original UUID.

The reload assertion covers:

- Entity UUID and gathering mode.
- stopped/running state and revision.
- stored activity.
- Home binding and target block.
- fuel stack and remaining fuel ticks.
- tool durability and custom components.
- gathering storage count and custom components.
- manual target rules and scanner cursor.

The test does not substitute an NBT copy for chunk unload. It requires the server entity manager to unload and reload the real chunk.

## Dedicated Server JVM probe

`CopperGolemRestartProbe` is a gametest-source-set `ModInitializer`; it is not included in the production JAR. It is enabled only when `DEADRECALL_COPPER_RESTART_PROBE_PHASE` is set.

The `runCopperRestartProbe` Loom configuration uses `run/copperRestartProbe/world`. CI starts three independent Dedicated Server JVMs:

1. `seed` creates a persistent Copper Golem, Copper Chest Home, target block and initial gathering state, then records the entity UUID outside the world directory.
2. `recover` reloads the same world and UUID, verifies the first state, changes the revision, activity, tool durability, storage components, scanner cursor, target block and Home inventory, then saves again.
3. `verify` reloads the world a second time and verifies the recovered state exactly.

Each phase writes an `.ok` or `.failure` marker to `copper-restart-probe/`. CI keeps `copper-restart-*.log`, the marker directory and the Dedicated Server run directory when a failure occurs.

The probe verifies actual world/entity/BlockEntity persistence across processes. It does not simulate a real network client or GUI interaction.
