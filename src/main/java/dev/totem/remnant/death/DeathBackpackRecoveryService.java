package dev.totem.remnant.death;

import com.adaptor.deadrecall.api.death.DeathBackpackNodeBinding;
import dev.totem.core.api.v1.death.DeathBackpackNodeLifecycle;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Completes an optional external death-node lifecycle when a bound backpack is recovered. */
public final class DeathBackpackRecoveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger("TotemRemnant");
    private static final Set<UUID> FORCED_NOTIFICATION_FAILURES = new HashSet<>();

    private DeathBackpackRecoveryService() { }

    public static boolean recoverBoundNode(ServerPlayer recoveringPlayer, ItemStack deathBackpack) {
        UUID nodeId = DeathBackpackNodeBinding.read(deathBackpack);
        if (nodeId == null) return false;
        boolean disabled = DeathBackpackNodeLifecycle.current()
                .map(adapter -> adapter.recover(recoveringPlayer, nodeId))
                .orElse(false);
        if (!disabled) return false;
        notifyRecoveredSafely(recoveringPlayer);
        DeathBackpackNotifications notifications = DeathBackpackNotifications.current();
        if (notifications != null) {
            try {
                notifications.recovered(recoveringPlayer);
            } catch (RuntimeException exception) {
                LOGGER.warn("Death node was recovered, but bundle notification failed for {}", recoveringPlayer.getName().getString(), exception);
            }
        }
        return true;
    }

    static void forceNotificationFailureForTesting(UUID playerId) { FORCED_NOTIFICATION_FAILURES.add(playerId); }
    static void clearForcedNotificationFailureForTesting(UUID playerId) { FORCED_NOTIFICATION_FAILURES.remove(playerId); }

    private static void notifyRecoveredSafely(ServerPlayer player) {
        try {
            if (FORCED_NOTIFICATION_FAILURES.remove(player.getUUID())) {
                throw new IllegalStateException("Forced death-backpack recovery notification failure");
            }
            player.sendSystemMessage(Component.translatable("message.deadrecall.space_unit.death_node_recovered"));
        } catch (RuntimeException exception) {
            LOGGER.warn("Death node was recovered, but the player notification failed for {}", player.getName().getString(), exception);
        }
    }
}
