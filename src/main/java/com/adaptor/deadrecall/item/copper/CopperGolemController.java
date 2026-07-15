package com.adaptor.deadrecall.item.copper;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class CopperGolemController {
    private static final int PRUNE_BINDINGS_INTERVAL_TICKS = 20;
    private static final int COPPER_GOLEM_DISCOVERY_INTERVAL_TICKS = 20;
    private static final Map<UUID, ResourceKey<Level>> TRACKED_COPPER_GOLEMS = new ConcurrentHashMap<>();

    private static int pruneBindingsTicker = 0;
    private static int copperGolemDiscoveryTicker = COPPER_GOLEM_DISCOVERY_INTERVAL_TICKS - 1;

    private CopperGolemController() {
    }

    static void tick(MinecraftServer server) {
        pruneBindingsTicker++;
        boolean shouldPruneBindings = pruneBindingsTicker >= PRUNE_BINDINGS_INTERVAL_TICKS;
        if (shouldPruneBindings) {
            pruneBindingsTicker = 0;
        }

        copperGolemDiscoveryTicker++;
        if (copperGolemDiscoveryTicker >= COPPER_GOLEM_DISCOVERY_INTERVAL_TICKS) {
            copperGolemDiscoveryTicker = 0;
            discoverManagedCopperGolems(server);
        }

        tickTrackedCopperGolems(server, shouldPruneBindings);
    }

    static void track(CopperGolem golem) {
        if (golem.level().isClientSide() || golem.isRemoved()) {
            return;
        }
        TRACKED_COPPER_GOLEMS.put(golem.getUUID(), golem.level().dimension());
    }

    static void untrack(CopperGolem golem) {
        TRACKED_COPPER_GOLEMS.remove(golem.getUUID());
    }

    private static void discoverManagedCopperGolems(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CopperGolem golem && CopperGolemWrenchHandler.shouldTrackCopperGolem(golem)) {
                    track(golem);
                }
            }
        }
    }

    private static void tickTrackedCopperGolems(MinecraftServer server, boolean shouldPruneBindings) {
        for (Map.Entry<UUID, ResourceKey<Level>> entry : new ArrayList<>(TRACKED_COPPER_GOLEMS.entrySet())) {
            ServerLevel level = server.getLevel(entry.getValue());
            if (level == null) {
                TRACKED_COPPER_GOLEMS.remove(entry.getKey());
                continue;
            }

            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof CopperGolem golem) || golem.isRemoved() || !golem.isAlive()) {
                TRACKED_COPPER_GOLEMS.remove(entry.getKey());
                continue;
            }

            CopperGolemWrenchHandler.tickManagedCopperGolem(server, level, golem, shouldPruneBindings);
            if (!CopperGolemWrenchHandler.shouldTrackCopperGolem(golem)) {
                TRACKED_COPPER_GOLEMS.remove(entry.getKey());
            }
        }
    }
}
