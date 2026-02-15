package com.adaptor.deadrecall;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathLocationManager {
    private static final Map<UUID, DeathLocation> deathLocations = new HashMap<>();

    public static void setDeathLocation(ServerPlayerEntity player, BlockPos pos, World world) {
        deathLocations.put(player.getUuid(), new DeathLocation(pos, world.getRegistryKey().getValue().toString()));
    }

    public static DeathLocation getDeathLocation(ServerPlayerEntity player) {
        return deathLocations.get(player.getUuid());
    }

    public static void clearDeathLocation(ServerPlayerEntity player) {
        deathLocations.remove(player.getUuid());
    }

    public static class DeathLocation {
        public final BlockPos pos;
        public final String worldRegistryKey;
        public DeathLocation(BlockPos pos, String worldRegistryKey) {
            this.pos = pos;
            this.worldRegistryKey = worldRegistryKey;
        }
    }
}

