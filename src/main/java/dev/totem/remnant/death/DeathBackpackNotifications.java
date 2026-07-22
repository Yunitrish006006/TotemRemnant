package dev.totem.remnant.death;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Optional bundle-owned notification seam for Remnant capture and recovery events. */
public interface DeathBackpackNotifications {
    void created(ServerPlayer player, int stackCount, BlockPos position);
    void recovered(ServerPlayer player);

    static void register(DeathBackpackNotifications adapter) { Holder.adapter = adapter; }
    static DeathBackpackNotifications current() { return Holder.adapter; }

    final class Holder {
        private static volatile DeathBackpackNotifications adapter;
        private Holder() { }
    }
}
