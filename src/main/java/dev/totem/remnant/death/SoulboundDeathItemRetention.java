package dev.totem.remnant.death;

import dev.totem.core.api.v1.death.DeathRetainedItemPolicy;
import dev.totem.remnant.TotemRemnant;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Optional;

/** Stages and restores one policy-authorized item without exposing Nexus classes to Remnant. */
public final class SoulboundDeathItemRetention {
    private SoulboundDeathItemRetention() {
    }

    public static boolean stageForDeath(ServerPlayer player) {
        Optional<DeathRetainedItemPolicy> policy = DeathRetainedItemPolicy.current();
        if (player == null || policy.isEmpty()) {
            return false;
        }

        SoulboundDeathItemSavedData data = data(player.level().getServer());
        if (data.pending(player.getUUID()).isPresent()) {
            TotemRemnant.LOGGER.warn(
                    "Refusing to replace an unrestored soulbound item for {}",
                    player.getName().getString()
            );
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!authorized(policy.get(), player, stack) || vanishesOnDeath(stack)) {
                continue;
            }

            ItemStack retained = stack.copyWithCount(1);
            if (!data.putIfAbsent(player.getUUID(), retained, slot)) {
                return false;
            }
            if (stack.getCount() == 1) {
                inventory.setItem(slot, ItemStack.EMPTY);
            } else {
                stack.shrink(1);
            }
            inventory.setChanged();
            return true;
        }
        return false;
    }

    public static boolean restoreAfterRespawn(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        SoulboundDeathItemSavedData data = data(player.level().getServer());
        Optional<SoulboundDeathItemSavedData.PendingItem> pending = data.pending(player.getUUID());
        if (pending.isEmpty()) {
            return false;
        }

        SoulboundDeathItemSavedData.PendingItem retained = pending.get();
        Inventory inventory = player.getInventory();
        int targetSlot = availableSlot(inventory, retained.preferredSlot());
        if (targetSlot < 0) {
            TotemRemnant.LOGGER.warn(
                    "Could not restore soulbound teleport item for {}; inventory has no empty slot",
                    player.getName().getString()
            );
            return false;
        }

        inventory.setItem(targetSlot, retained.stack().copy());
        inventory.setChanged();
        data.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable(
                "message.deadrecall.remnant.soulbound_restored",
                retained.stack().getHoverName()
        ));
        return true;
    }

    static boolean hasPending(ServerPlayer player) {
        return data(player.level().getServer()).pending(player.getUUID()).isPresent();
    }

    private static boolean authorized(
            DeathRetainedItemPolicy policy,
            ServerPlayer player,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return false;
        }
        try {
            return policy.shouldRetain(player, stack);
        } catch (RuntimeException exception) {
            TotemRemnant.LOGGER.warn(
                    "Death-retained item policy failed for {}",
                    player.getName().getString(),
                    exception
            );
            return false;
        }
    }

    private static boolean vanishesOnDeath(ItemStack stack) {
        return EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP);
    }

    private static int availableSlot(Inventory inventory, int preferredSlot) {
        if (preferredSlot >= 0
                && preferredSlot < inventory.getContainerSize()
                && inventory.getItem(preferredSlot).isEmpty()) {
            return preferredSlot;
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static SoulboundDeathItemSavedData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SoulboundDeathItemSavedData.TYPE);
    }
}
