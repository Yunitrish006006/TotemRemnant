package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.DiscordBridge;
import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.space.SpaceUnitHandler;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Captures a player's authoritative inventory directly before vanilla death drops are emitted.
 *
 * <p>The service only commits inventory removal after it has built the death-backpack stack.
 * Any runtime failure restores the captured slots and lets vanilla {@link Inventory#dropAll()}
 * continue as the fallback path.</p>
 */
public final class DeathBackpackCaptureService {
    private static final String TAG_DEATH_BACKPACK_ID = "deadrecall_death_backpack_id";
    private static final int PICKUP_DELAY_TICKS = 40;
    private static final Set<UUID> COMPLETED_CAPTURES = new HashSet<>();

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

        removeCapturedSlots(inventory, capturedSlots);
        try {
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

            UUID deathNodeId = SpaceUnitHandler.createDeathNode(player, level, deathPos);
            SpaceUnitHandler.writeDeathNodeBinding(deathBackpack, deathNodeId);
            backpackEntity.setItem(deathBackpack);

            COMPLETED_CAPTURES.add(player.getUUID());
            DiscordBridge.sendDeathBackpackCreated(player.getName().getString());
            player.sendSystemMessage(Component.translatable("message.deadrecall.death_backpack.collected")
                    .withStyle(ChatFormatting.YELLOW));
            Deadrecall.LOGGER.info(
                    "Created death backpack directly from player {} with {} stacks at {}",
                    player.getName().getString(),
                    contents.size(),
                    deathPos
            );
            return true;
        } catch (RuntimeException exception) {
            if (backpackEntity != null && backpackEntity.isAlive()) {
                backpackEntity.discard();
            }
            restoreCapturedSlots(inventory, capturedSlots);
            Deadrecall.LOGGER.error(
                    "Direct death backpack capture failed for {}; vanilla death drops will be used",
                    player.getName().getString(),
                    exception
            );
            return false;
        }
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

    private record CapturedSlot(int slot, ItemStack stack) {
    }
}
