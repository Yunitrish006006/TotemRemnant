package com.adaptor.deadrecall;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathLocationManager {
    private static final Map<UUID, DeathLocation> deathLocations = new HashMap<>();

    public static void setDeathLocation(ServerPlayer player, BlockPos pos, Level world) {
        deathLocations.put(player.getUUID(), new DeathLocation(pos, world.dimension().toString()));
    }

    public static DeathLocation getDeathLocation(ServerPlayer player) {
        return deathLocations.get(player.getUUID());
    }

    public static void clearDeathLocation(ServerPlayer player) {
        deathLocations.remove(player.getUUID());
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
