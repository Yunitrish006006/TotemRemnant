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
import java.util.List;
import java.util.LinkedHashSet;

import dev.totem.remnant.item.TieredBackpackItem;
import dev.totem.remnant.upgrade.BackpackUpgradeData;
import dev.totem.remnant.upgrade.BackpackUpgradeType;

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
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int slot : candidateSlots(inventory)) {
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

    /** Deterministic player-controlled priority without scanning nested containers. */
    static List<Integer> candidateSlots(Inventory inventory) {
        LinkedHashSet<Integer> ordered = new LinkedHashSet<>();
        addIfValid(ordered, inventory, inventory.getSelectedSlot());
        addIfValid(ordered, inventory, Inventory.SLOT_OFFHAND);
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            addIfValid(ordered, inventory, slot);
        }
        for (int slot = Inventory.getSelectionSize(); slot < inventory.getContainerSize(); slot++) {
            addIfValid(ordered, inventory, slot);
        }
        return List.copyOf(ordered);
    }

    /** Consumes every installed one-use charge and stages those backpacks before normal death capture. */
    public static int stageChargedBackpacksForDeath(ServerPlayer player) {
        if (player == null) return 0;
        Inventory inventory = player.getInventory();
        SoulboundDeathItemSavedData data = data(player.level().getServer());
        int staged = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof TieredBackpackItem)
                    || !BackpackUpgradeData.has(stack, BackpackUpgradeType.SOULBOUND_CHARGE)) continue;
            ItemStack retained = stack.copyWithCount(1);
            if (!BackpackUpgradeData.consume(retained, BackpackUpgradeType.SOULBOUND_CHARGE)
                    || !data.add(player.getUUID(), retained, slot)) continue;
            inventory.setItem(slot, ItemStack.EMPTY);
            staged++;
        }
        if (staged > 0) inventory.setChanged();
        return staged;
    }

    public static boolean restoreAfterRespawn(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        SoulboundDeathItemSavedData data = data(player.level().getServer());
        List<SoulboundDeathItemSavedData.PendingItem> pending = data.pendingAll(player.getUUID());
        if (pending.isEmpty()) {
            return false;
        }
        Inventory inventory = player.getInventory();
        if (emptySlotCount(inventory) < pending.size()) {
            TotemRemnant.LOGGER.warn(
                    "Could not restore {} soulbound items for {}; inventory has insufficient space",
                    pending.size(),
                    player.getName().getString()
            );
            return false;
        }
        for (SoulboundDeathItemSavedData.PendingItem retained : pending) {
            int targetSlot = availableSlot(inventory, retained.preferredSlot());
            inventory.setItem(targetSlot, retained.stack().copy());
            player.sendSystemMessage(Component.translatable(
                    "message.deadrecall.remnant.soulbound_restored",
                    retained.stack().getHoverName()
            ));
        }
        inventory.setChanged();
        data.remove(player.getUUID());
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

    private static void addIfValid(
            LinkedHashSet<Integer> ordered,
            Inventory inventory,
            int slot
    ) {
        if (slot >= 0 && slot < inventory.getContainerSize()) {
            ordered.add(slot);
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

    private static int emptySlotCount(Inventory inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).isEmpty()) count++;
        }
        return count;
    }

    private static SoulboundDeathItemSavedData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SoulboundDeathItemSavedData.TYPE);
    }
}
