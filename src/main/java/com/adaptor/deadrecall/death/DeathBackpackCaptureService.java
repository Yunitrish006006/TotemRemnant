package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.DiscordBridge;
import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryProvider;
import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryRegistry;
import com.adaptor.deadrecall.api.death.DeathBackpackAddonSlot;
import com.adaptor.deadrecall.inventory.PortableContainerPolicy;
import com.adaptor.deadrecall.registry.TotemRemnantItemRegistration;
import com.adaptor.deadrecall.space.DeadRecallSpaceDiscoverySavedData;
import com.adaptor.deadrecall.space.DeadRecallSpaceUnitSavedData;
import com.adaptor.deadrecall.space.SpaceUnitHandler;
import com.adaptor.deadrecall.space.SpaceUnitRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Captures authoritative player-owned stacks directly before vanilla death drops are emitted.
 * Vanilla, transient menu and registered addon slots participate in one transaction.
 */
public final class DeathBackpackCaptureService {
    private static final String TAG_DEATH_BACKPACK_ID = "deadrecall_death_backpack_id";
    private static final int PICKUP_DELAY_TICKS = 40;
    private static final Map<UUID, CaptureFailurePoint> FORCED_TEST_FAILURES = new HashMap<>();

    private DeathBackpackCaptureService() {
    }

    /** Runs at {@code Player.dropEquipment} entry before vanilla and addon death drops. */
    public static boolean captureBeforeVanillaDrop(ServerPlayer player, ServerLevel level) {
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return false;
        }

        Inventory inventory = player.getInventory();
        List<TransientStack> transientStacks = collectTransientStacks(player);
        processUncapturedTransientStacks(player, inventory, transientStacks);

        List<CapturedSlot> capturedSlots = collectCapturableSlots(inventory);
        List<TransientStack> capturedTransientStacks = transientStacks.stream()
                .filter(transientStack -> isTransientCapturable(transientStack.stack()))
                .toList();
        List<AddonCapturedSlot> capturedAddonSlots = collectCapturableAddonSlots(player);
        if (capturedSlots.isEmpty() && capturedTransientStacks.isEmpty() && capturedAddonSlots.isEmpty()) {
            return false;
        }

        List<ItemStack> contents = new ArrayList<>(
                capturedSlots.size() + capturedTransientStacks.size() + capturedAddonSlots.size());
        capturedSlots.stream().map(CapturedSlot::stack).map(ItemStack::copy).forEach(contents::add);
        capturedTransientStacks.stream().map(TransientStack::stack).map(ItemStack::copy).forEach(contents::add);
        capturedAddonSlots.stream().map(AddonCapturedSlot::stack).map(ItemStack::copy).forEach(contents::add);

        ItemStack deathBackpack = createDeathBackpack(contents);
        BlockPos deathPos = player.blockPosition().immutable();
        ItemEntity backpackEntity = null;
        UUID deathNodeId = null;
        List<AddonCapturedSlot> removedAddonSlots = new ArrayList<>();

        try {
            removeCapturedSlots(inventory, capturedSlots);
            removeCapturedTransientStacks(capturedTransientStacks);
            removeCapturedAddonSlots(capturedAddonSlots, removedAddonSlots);
            failIfRequested(player, CaptureFailurePoint.AFTER_SLOT_REMOVAL);

            backpackEntity = new ItemEntity(
                    level,
                    deathPos.getX() + 0.5,
                    deathPos.getY() + 0.5,
                    deathPos.getZ() + 0.5,
                    deathBackpack
            );
            backpackEntity.setPickUpDelay(PICKUP_DELAY_TICKS);
            backpackEntity.setUnlimitedLifetime();

            if (!level.addFreshEntity(backpackEntity)) {
                throw new IllegalStateException("Minecraft rejected the death backpack ItemEntity");
            }
            failIfRequested(player, CaptureFailurePoint.AFTER_ENTITY_ADD);

            deathNodeId = createDeathNodeTransactional(player, level, deathPos);
            failIfRequested(player, CaptureFailurePoint.AFTER_DEATH_NODE_CREATE);

            SpaceUnitHandler.writeDeathNodeBinding(deathBackpack, deathNodeId);
            backpackEntity.setItem(deathBackpack);
        } catch (RuntimeException exception) {
            if (deathNodeId != null) {
                rollbackDeathNode(player, level, deathNodeId);
            }
            if (backpackEntity != null && backpackEntity.isAlive()) {
                backpackEntity.discard();
            }
            restoreCapturedSlots(inventory, capturedSlots);
            restoreTransientStacksForVanillaDrop(inventory, capturedTransientStacks);
            restoreAddonSlotsForVanillaDrop(inventory, removedAddonSlots);
            clearForcedFailureForTesting(player.getUUID());
            Deadrecall.LOGGER.error(
                    "Direct death backpack capture failed for {}; vanilla death drops will be used",
                    player.getName().getString(),
                    exception
            );
            return false;
        }

        clearForcedFailureForTesting(player.getUUID());
        notifyCaptureCompleted(player, contents.size(), deathPos);
        return true;
    }

    static boolean isCapturable(ItemStack stack) {
        return !stack.isEmpty() && PortableContainerPolicy.mayInsertIntoBackpack(stack);
    }

    static void forceFailureForTesting(UUID playerId, CaptureFailurePoint failurePoint) {
        FORCED_TEST_FAILURES.put(playerId, failurePoint);
    }

    static void clearForcedFailureForTesting(UUID playerId) {
        FORCED_TEST_FAILURES.remove(playerId);
    }

    private static void failIfRequested(ServerPlayer player, CaptureFailurePoint failurePoint) {
        if (FORCED_TEST_FAILURES.get(player.getUUID()) != failurePoint) {
            return;
        }
        FORCED_TEST_FAILURES.remove(player.getUUID());
        throw new IllegalStateException("Forced death-backpack capture failure at " + failurePoint);
    }

    private static UUID createDeathNodeTransactional(ServerPlayer player, ServerLevel level, BlockPos deathPos) {
        DeadRecallSpaceUnitSavedData units = units(level);
        DeadRecallSpaceDiscoverySavedData discovery = discovery(level);
        SpaceUnitRecord unit = units.createDeathUnit(level, deathPos, player);
        try {
            discovery.markDiscovered(player.getUUID(), unit.id());
            return unit.id();
        } catch (RuntimeException exception) {
            units.disableDeathUnit(player.getUUID(), unit.id(), level.getGameTime());
            discovery.removeDiscovered(player.getUUID(), unit.id());
            throw exception;
        }
    }

    private static void rollbackDeathNode(ServerPlayer player, ServerLevel level, UUID deathNodeId) {
        units(level).disableDeathUnit(player.getUUID(), deathNodeId, level.getGameTime());
        discovery(level).removeDiscovered(player.getUUID(), deathNodeId);
    }

    private static DeadRecallSpaceUnitSavedData units(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);
    }

    private static DeadRecallSpaceDiscoverySavedData discovery(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(DeadRecallSpaceDiscoverySavedData.TYPE);
    }

    private static void notifyCaptureCompleted(ServerPlayer player, int stackCount, BlockPos deathPos) {
        try {
            DiscordBridge.sendDeathBackpackCreated(player.getName().getString());
        } catch (RuntimeException exception) {
            Deadrecall.LOGGER.warn(
                    "Death backpack was created, but the Discord notification failed for {}",
                    player.getName().getString(),
                    exception
            );
        }

        try {
            player.sendSystemMessage(Component.translatable("message.deadrecall.death_backpack.collected")
                    .withStyle(ChatFormatting.YELLOW));
        } catch (RuntimeException exception) {
            Deadrecall.LOGGER.warn(
                    "Death backpack was created, but the player notification failed for {}",
                    player.getName().getString(),
                    exception
            );
        }

        Deadrecall.LOGGER.info(
                "Created death backpack directly from player {} with {} stacks at {}",
                player.getName().getString(),
                stackCount,
                deathPos
        );
    }

    private static List<CapturedSlot> collectCapturableSlots(Inventory inventory) {
        List<CapturedSlot> captured = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isCapturable(stack) && !isPreventedFromDeathDrop(stack)) {
                captured.add(new CapturedSlot(slot, stack.copy()));
            }
        }
        return List.copyOf(captured);
    }

    private static List<AddonCapturedSlot> collectCapturableAddonSlots(ServerPlayer player) {
        List<AddonCapturedSlot> captured = new ArrayList<>();
        Set<String> sourceKeys = new HashSet<>();
        for (DeathBackpackAddonInventoryProvider provider : DeathBackpackAddonInventoryRegistry.providers()) {
            try {
                List<? extends DeathBackpackAddonSlot> slots = provider.collectDroppableSlots(player);
                if (slots == null) {
                    throw new IllegalStateException("Provider returned null slot list");
                }
                for (DeathBackpackAddonSlot slot : slots) {
                    if (slot == null) {
                        throw new IllegalStateException("Provider returned a null slot");
                    }
                    String sourceKey = provider.id() + ":" + slot.sourceKey();
                    if (!sourceKeys.add(sourceKey)) {
                        throw new IllegalStateException("Provider returned duplicate source " + sourceKey);
                    }
                    ItemStack snapshot = slot.snapshot();
                    if (isCapturable(snapshot)) {
                        captured.add(new AddonCapturedSlot(provider.id().toString(), slot, snapshot.copy()));
                    }
                }
            } catch (RuntimeException exception) {
                Deadrecall.LOGGER.warn(
                        "Skipping death-backpack addon inventory provider {} after snapshot failure",
                        provider.id(),
                        exception
                );
            }
        }
        return List.copyOf(captured);
    }

    private static List<TransientStack> collectTransientStacks(ServerPlayer player) {
        List<TransientStack> transientStacks = new ArrayList<>();
        AbstractContainerMenu activeMenu = player.containerMenu;
        ItemStack carried = activeMenu.getCarried();
        if (!carried.isEmpty()) {
            transientStacks.add(TransientStack.carried(activeMenu, carried.copy()));
        }

        Container craftSlots = player.inventoryMenu.getCraftSlots();
        addContainerStacks(transientStacks, craftSlots);
        for (Slot slot : workstationInputSlots(activeMenu)) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                transientStacks.add(TransientStack.container(
                        slot.container,
                        slot.getContainerSlot(),
                        stack.copy()
                ));
            }
        }
        return List.copyOf(transientStacks);
    }

    private static void addContainerStacks(List<TransientStack> transientStacks, Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                transientStacks.add(TransientStack.container(container, slot, stack.copy()));
            }
        }
    }

    private static List<Slot> workstationInputSlots(AbstractContainerMenu menu) {
        if (menu instanceof CraftingMenu craftingMenu) {
            return List.copyOf(craftingMenu.getInputGridSlots());
        }
        if (menu instanceof ItemCombinerMenu itemCombinerMenu) {
            return inputSlotRange(menu, 0, itemCombinerMenu.getResultSlot());
        }
        if (menu instanceof GrindstoneMenu) {
            return inputSlotRange(menu, GrindstoneMenu.INPUT_SLOT, GrindstoneMenu.RESULT_SLOT);
        }
        if (menu instanceof StonecutterMenu) {
            return inputSlotRange(menu, StonecutterMenu.INPUT_SLOT, StonecutterMenu.RESULT_SLOT);
        }
        if (menu instanceof LoomMenu) {
            return inputSlotRange(menu, 0, 3);
        }
        if (menu instanceof CartographyTableMenu) {
            return inputSlotRange(menu, CartographyTableMenu.MAP_SLOT, CartographyTableMenu.RESULT_SLOT);
        }
        if (menu instanceof EnchantmentMenu) {
            return inputSlotRange(menu, 0, 2);
        }
        return List.of();
    }

    private static List<Slot> inputSlotRange(AbstractContainerMenu menu, int startInclusive, int endExclusive) {
        if (startInclusive < 0 || endExclusive < startInclusive || endExclusive > menu.slots.size()) {
            throw new IllegalStateException(
                    "Invalid workstation input range " + startInclusive + ".." + endExclusive
                            + " for " + menu.getClass().getName()
                            + " with " + menu.slots.size() + " slots"
            );
        }
        return List.copyOf(menu.slots.subList(startInclusive, endExclusive));
    }

    private static boolean isTransientCapturable(ItemStack stack) {
        return isCapturable(stack) && !isPreventedFromDeathDrop(stack);
    }

    private static boolean isPreventedFromDeathDrop(ItemStack stack) {
        return EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP);
    }

    private static void processUncapturedTransientStacks(
            ServerPlayer player,
            Inventory inventory,
            List<TransientStack> transientStacks
    ) {
        for (TransientStack transientStack : transientStacks) {
            ItemStack stack = transientStack.stack();
            if (isPreventedFromDeathDrop(stack)) {
                transientStack.clear();
                continue;
            }
            if (!PortableContainerPolicy.isRestrictedPortableContainer(stack)) {
                continue;
            }

            transientStack.clear();
            ItemStack looseContainer = stack.copy();
            ItemEntity dropped = player.drop(looseContainer, false);
            if (dropped == null) {
                inventory.placeItemBackInInventory(looseContainer, false);
            }
        }
    }

    private static ItemStack createDeathBackpack(List<ItemStack> contents) {
        ItemStack deathBackpack = new ItemStack(TotemRemnantItemRegistration.DEATH_BACKPACK);
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_DEATH_BACKPACK_ID, UUID.randomUUID().toString());
        deathBackpack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        deathBackpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return deathBackpack;
    }

    private static void removeCapturedSlots(Inventory inventory, List<CapturedSlot> capturedSlots) {
        for (CapturedSlot capturedSlot : capturedSlots) {
            inventory.setItem(capturedSlot.slot(), ItemStack.EMPTY);
        }
    }

    private static void restoreCapturedSlots(Inventory inventory, List<CapturedSlot> capturedSlots) {
        for (CapturedSlot capturedSlot : capturedSlots) {
            inventory.setItem(capturedSlot.slot(), capturedSlot.stack().copy());
        }
    }

    private static void removeCapturedTransientStacks(List<TransientStack> transientStacks) {
        transientStacks.forEach(TransientStack::clear);
    }

    private static void removeCapturedAddonSlots(
            List<AddonCapturedSlot> capturedSlots,
            List<AddonCapturedSlot> removedSlots
    ) {
        for (AddonCapturedSlot capturedSlot : capturedSlots) {
            if (!capturedSlot.slot().clearIfUnchanged(capturedSlot.stack())) {
                throw new IllegalStateException(
                        "Addon inventory slot changed before commit: "
                                + capturedSlot.providerId() + ":" + capturedSlot.slot().sourceKey()
                );
            }
            removedSlots.add(capturedSlot);
        }
    }

    private static void restoreTransientStacksForVanillaDrop(
            Inventory inventory,
            List<TransientStack> transientStacks
    ) {
        for (TransientStack transientStack : transientStacks) {
            ItemStack restored = transientStack.stack().copy();
            inventory.placeItemBackInInventory(restored, false);
        }
    }

    private static void restoreAddonSlotsForVanillaDrop(
            Inventory inventory,
            List<AddonCapturedSlot> capturedSlots
    ) {
        for (int index = capturedSlots.size() - 1; index >= 0; index--) {
            AddonCapturedSlot capturedSlot = capturedSlots.get(index);
            ItemStack restored = capturedSlot.stack().copy();
            try {
                if (capturedSlot.slot().restoreIfEmpty(restored)) {
                    continue;
                }
            } catch (RuntimeException exception) {
                Deadrecall.LOGGER.warn(
                        "Could not restore addon inventory source {}:{}; using vanilla inventory fallback",
                        capturedSlot.providerId(),
                        capturedSlot.slot().sourceKey(),
                        exception
                );
            }
            inventory.placeItemBackInInventory(restored, false);
        }
    }

    private record CapturedSlot(int slot, ItemStack stack) {
    }

    private record AddonCapturedSlot(String providerId, DeathBackpackAddonSlot slot, ItemStack stack) {
    }

    private record TransientStack(AbstractContainerMenu menu, Container container, int slot, ItemStack stack) {
        private static TransientStack carried(AbstractContainerMenu menu, ItemStack stack) {
            return new TransientStack(menu, null, -1, stack);
        }

        private static TransientStack container(Container container, int slot, ItemStack stack) {
            return new TransientStack(null, container, slot, stack);
        }

        private void clear() {
            if (this.menu != null) {
                this.menu.setCarried(ItemStack.EMPTY);
            } else if (this.container != null && this.slot >= 0) {
                this.container.setItem(this.slot, ItemStack.EMPTY);
            }
        }
    }

    enum CaptureFailurePoint {
        AFTER_SLOT_REMOVAL,
        AFTER_ENTITY_ADD,
        AFTER_DEATH_NODE_CREATE
    }
}
