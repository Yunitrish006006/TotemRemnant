package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.DiscordBridge;
import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.item.ModItems;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Captures a player's authoritative inventory directly before vanilla death drops are emitted.
 *
 * <p>The service only commits inventory removal after it has built and spawned the death backpack.
 * Any runtime failure discards the incomplete entity, rolls back the temporary death node and
 * restores the captured slots so vanilla {@link Inventory#dropAll()} can remain the fallback.</p>
 */
public final class DeathBackpackCaptureService {
    private static final String TAG_DEATH_BACKPACK_ID = "deadrecall_death_backpack_id";
    private static final int PICKUP_DELAY_TICKS = 40;
    private static final Set<UUID> COMPLETED_CAPTURES = new HashSet<>();
    private static final Map<UUID, CaptureFailurePoint> FORCED_TEST_FAILURES = new HashMap<>();

    private DeathBackpackCaptureService() {
    }

    /**
     * Runs at the invocation point immediately before vanilla {@code Inventory.dropAll()}.
     * Backpacks remain in the inventory and are therefore emitted by vanilla instead of being
     * nested inside the new death backpack.
     */
    public static boolean captureBeforeVanillaDrop(ServerPlayer player, ServerLevel level) {
        Inventory inventory = player.getInventory();
        List<CapturedSlot> capturedSlots = collectCapturableSlots(inventory);
        if (capturedSlots.isEmpty()) {
            return false;
        }

        List<ItemStack> contents = capturedSlots.stream()
                .map(CapturedSlot::stack)
                .map(ItemStack::copy)
                .toList();
        ItemStack deathBackpack = createDeathBackpack(contents);
        BlockPos deathPos = player.blockPosition().immutable();
        ItemEntity backpackEntity = null;
        UUID deathNodeId = null;

        removeCapturedSlots(inventory, capturedSlots);
        try {
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
            clearForcedFailureForTesting(player.getUUID());
            Deadrecall.LOGGER.error(
                    "Direct death backpack capture failed for {}; vanilla death drops will be used",
                    player.getName().getString(),
                    exception
            );
            return false;
        }

        clearForcedFailureForTesting(player.getUUID());
        COMPLETED_CAPTURES.add(player.getUUID());
        notifyCaptureCompleted(player, contents.size(), deathPos);
        return true;
    }

    /**
     * Consumed by the legacy AFTER_DEATH bridge so the old nearby-ItemEntity collector does not
     * run after a successful direct capture.
     */
    public static boolean consumeCompletedCapture(UUID playerId) {
        return COMPLETED_CAPTURES.remove(playerId);
    }

    static boolean isCapturable(ItemStack stack) {
        return !stack.isEmpty() && !BackpackItemHelper.isBackpackItem(stack);
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
            if (isCapturable(stack)) {
                captured.add(new CapturedSlot(slot, stack.copy()));
            }
        }
        return List.copyOf(captured);
    }

    private static ItemStack createDeathBackpack(List<ItemStack> contents) {
        ItemStack deathBackpack = new ItemStack(ModItems.DEATH_BACKPACK);
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_DEATH_BACKPACK_ID, UUID.randomUUID().toString());
        deathBackpack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        deathBackpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return deathBackpack;
    }

    private static void removeCapturedSlots(Inventory inventory, List<CapturedSlot> capturedSlots) {
        for (CapturedSlot captured : capturedSlots) {
            inventory.removeItemNoUpdate(captured.slot());
        }
    }

    private static void restoreCapturedSlots(Inventory inventory, List<CapturedSlot> capturedSlots) {
        for (CapturedSlot captured : capturedSlots) {
            ItemStack restored = captured.stack().copy();
            if (inventory.getItem(captured.slot()).isEmpty()) {
                inventory.setItem(captured.slot(), restored);
            } else {
                inventory.placeItemBackInInventory(restored, false);
            }
        }
    }

    enum CaptureFailurePoint {
        AFTER_SLOT_REMOVAL,
        AFTER_ENTITY_ADD,
        AFTER_DEATH_NODE_CREATE
    }

    private record CapturedSlot(int slot, ItemStack stack) {
    }
}
