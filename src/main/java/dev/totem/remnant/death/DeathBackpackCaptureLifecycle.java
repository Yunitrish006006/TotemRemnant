package dev.totem.remnant.death;

import dev.totem.core.api.v1.death.DeathBackpackNodeLifecycle;
import dev.totem.core.api.v1.event.DeathBackpackCreatedEvent;
import dev.totem.core.api.v1.event.TotemEventBus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/** Commits a prepared Remnant death-backpack capture without owning inventory-slot extraction. */
public final class DeathBackpackCaptureLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger("TotemRemnant");
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
            if (nodeId != null) {
                UUID boundNodeId = nodeId;
                UUID backpackEntityId = entity.getUUID();
                DeathBackpackNodeLifecycle.current()
                        .ifPresent(adapter -> adapter.bind(level, boundNodeId, backpackEntityId));
            }
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
        notifyCreated(player, level, position, contents.size());
        return true;
    }

    private static void notifyCreated(
            ServerPlayer player,
            ServerLevel level,
            BlockPos position,
            int stackCount
    ) {
        try {
            player.sendSystemMessage(Component.translatable("message.deadrecall.death_backpack.collected")
                    .withStyle(ChatFormatting.YELLOW));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Death backpack was created, but the player notification failed for {}",
                    player.getName().getString(),
                    exception
            );
        }

        TotemEventBus.publish(new DeathBackpackCreatedEvent(
                player.getName().getString(),
                stackCount,
                level.dimension().identifier().toString(),
                position.getX(),
                position.getY(),
                position.getZ()
        ));
        LOGGER.info(
                "Created death backpack for {} with {} stacks at {} {}",
                player.getName().getString(),
                stackCount,
                level.dimension().identifier(),
                position
        );
    }
}
