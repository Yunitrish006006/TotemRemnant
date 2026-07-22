package dev.totem.remnant.death;

import dev.totem.core.api.v1.death.DeathBackpackNodeLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/** Commits a prepared Remnant death-backpack capture without owning inventory-slot extraction. */
public final class DeathBackpackCaptureLifecycle {
    private static final int PICKUP_DELAY_TICKS = 40;
    private DeathBackpackCaptureLifecycle() { }

    public static boolean commit(ServerPlayer player, ServerLevel level, BlockPos position, List<ItemStack> contents) {
        DeathBackpackFactory factory = DeathBackpackFactory.current();
        if (factory == null || contents.isEmpty()) return false;
        ItemStack backpack = factory.create(contents);
        if (backpack.isEmpty()) return false;
        ItemEntity entity = null;
        UUID nodeId = null;
        try {
            entity = new ItemEntity(level, position.getX() + .5, position.getY() + .5, position.getZ() + .5, backpack);
            entity.setPickUpDelay(PICKUP_DELAY_TICKS);
            entity.setUnlimitedLifetime();
            if (!level.addFreshEntity(entity)) throw new IllegalStateException("Minecraft rejected the death backpack ItemEntity");
            nodeId = DeathBackpackNodeLifecycle.current().map(adapter -> adapter.create(player, level, position)).orElse(null);
            DeathBackpackNodeBinding.write(backpack, nodeId);
            entity.setItem(backpack);
        } catch (RuntimeException exception) {
            if (nodeId != null) {
                UUID nodeToRollback = nodeId;
                DeathBackpackNodeLifecycle.current().ifPresent(adapter -> adapter.rollback(player, level, nodeToRollback));
            }
            if (entity != null && entity.isAlive()) entity.discard();
            return false;
        }
        DeathBackpackNotifications notifications = DeathBackpackNotifications.current();
        if (notifications != null) try { notifications.created(player, contents.size(), position); } catch (RuntimeException ignored) { }
        return true;
    }
}
