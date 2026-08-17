package dev.totem.remnant.death;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/** Package-local access used only by the two-JVM Remnant restart probe. */
public final class SoulboundDeathItemRestartProbeAccess {
    private SoulboundDeathItemRestartProbeAccess() {
    }

    public static void seed(MinecraftServer server, UUID playerId) {
        SoulboundDeathItemSavedData data = server.overworld().getDataStorage()
                .computeIfAbsent(SoulboundDeathItemSavedData.TYPE);
        if (!data.putIfAbsent(playerId, new ItemStack(Items.RECOVERY_COMPASS), 7)) {
            throw new IllegalStateException("Seed phase found a stale pending soulbound item");
        }
    }

    public static void verifyAndRemove(MinecraftServer server, UUID playerId) {
        SoulboundDeathItemSavedData data = server.overworld().getDataStorage()
                .computeIfAbsent(SoulboundDeathItemSavedData.TYPE);
        SoulboundDeathItemSavedData.PendingItem pending = data.pending(playerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Restart did not reload the pending soulbound item"));
        if (!pending.stack().is(Items.RECOVERY_COMPASS)
                || pending.stack().getCount() != 1
                || pending.preferredSlot() != 7) {
            throw new IllegalStateException("Restart changed the pending soulbound item");
        }
        data.remove(playerId);
    }
}
