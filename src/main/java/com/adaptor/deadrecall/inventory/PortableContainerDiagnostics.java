package com.adaptor.deadrecall.inventory;

import com.adaptor.deadrecall.Deadrecall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate-limited server diagnostics for rejected automated nesting attempts.
 */
public final class PortableContainerDiagnostics {
    private static final long LOG_COOLDOWN_TICKS = 20L * 10L;
    private static final int MAX_TRACKED_KEYS = 1024;
    private static final Map<String, Long> NEXT_LOG_TIME = new HashMap<>();

    private PortableContainerDiagnostics() {
    }

    public static void logRejectedAutomation(Level level, BlockPos pos, ItemStack stack, String route) {
        if (level == null || level.isClientSide() || pos == null || stack == null || stack.isEmpty()) {
            return;
        }

        long gameTime = level.getGameTime();
        String key = level.dimension().identifier() + "|" + pos.asLong() + "|" + route;
        synchronized (NEXT_LOG_TIME) {
            long nextLogTime = NEXT_LOG_TIME.getOrDefault(key, Long.MIN_VALUE);
            if (gameTime < nextLogTime) {
                return;
            }
            if (NEXT_LOG_TIME.size() >= MAX_TRACKED_KEYS) {
                NEXT_LOG_TIME.clear();
            }
            NEXT_LOG_TIME.put(key, gameTime + LOG_COOLDOWN_TICKS);
        }

        Deadrecall.LOGGER.warn(
                "Rejected portable-container nesting via {}: item={} target={} {}",
                route,
                BuiltInRegistries.ITEM.getKey(stack.getItem()),
                level.dimension().identifier(),
                pos
        );
    }
}
