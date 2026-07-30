package dev.totem.remnant.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side menu that enforces the Remnant nesting policy for every click path.
 *
 * <p>Legacy invalid contents may move out, but cannot be reinserted. The ItemStack that owns the
 * open inventory is identity-locked so number-key and direct clicks cannot move it into itself.</p>
 */
public final class BackpackMenu extends ChestMenu {
    private static final long REJECTION_MESSAGE_COOLDOWN_TICKS = 20L;

    private final ItemStack trackedBackpackStack;
    private final int backpackSlotCount;
    private long nextRejectionMessageGameTime;

    public BackpackMenu(
            MenuType<?> menuType,
            int containerId,
            Inventory playerInventory,
            DeathBackpackInventory backpackInventory,
            int rows
    ) {
        super(menuType, containerId, playerInventory, backpackInventory, rows);
        trackedBackpackStack = backpackInventory.getBackpackStack();
        backpackSlotCount = rows * 9;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot sourceSlot = slots.get(slotIndex);
        if (slotIndex >= backpackSlotCount
                && !PortableContainerPolicy.mayInsertIntoBackpack(sourceSlot.getItem())) {
            notifyRestrictedInsertion(player);
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, slotIndex);
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (targetsTrackedBackpack(slotIndex)
                || swapsTrackedBackpackFromInventory(player.getInventory(), input, buttonNum)) {
            return;
        }

        if (collectsRestrictedContainersWithPickupAll(input)
                || insertsRestrictedCarriedStack(slotIndex, input)
                || swapsRestrictedInventoryStackIntoBackpack(
                        slotIndex,
                        player.getInventory(),
                        input,
                        buttonNum
                )) {
            notifyRestrictedInsertion(player);
            return;
        }
        super.clicked(slotIndex, buttonNum, input, player);
    }

    private boolean targetsTrackedBackpack(int slotIndex) {
        return slotIndex >= 0
                && slotIndex < slots.size()
                && slots.get(slotIndex).getItem() == trackedBackpackStack;
    }

    private boolean swapsTrackedBackpackFromInventory(
            Inventory inventory,
            ContainerInput input,
            int inventorySlot
    ) {
        return input == ContainerInput.SWAP
                && inventorySlot >= 0
                && inventorySlot < inventory.getContainerSize()
                && inventory.getItem(inventorySlot) == trackedBackpackStack;
    }

    private boolean collectsRestrictedContainersWithPickupAll(ContainerInput input) {
        return input == ContainerInput.PICKUP_ALL
                && PortableContainerPolicy.isRestrictedPortableContainer(getCarried());
    }

    private boolean insertsRestrictedCarriedStack(int slotIndex, ContainerInput input) {
        if (slotIndex < 0 || slotIndex >= backpackSlotCount) {
            return false;
        }
        if (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_CRAFT) {
            return false;
        }
        return !PortableContainerPolicy.mayInsertIntoBackpack(getCarried());
    }

    private boolean swapsRestrictedInventoryStackIntoBackpack(
            int slotIndex,
            Inventory inventory,
            ContainerInput input,
            int inventorySlot
    ) {
        return slotIndex >= 0
                && slotIndex < backpackSlotCount
                && input == ContainerInput.SWAP
                && inventorySlot >= 0
                && inventorySlot < inventory.getContainerSize()
                && !PortableContainerPolicy.mayInsertIntoBackpack(inventory.getItem(inventorySlot));
    }

    private void notifyRestrictedInsertion(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        long gameTime = player.level().getGameTime();
        if (gameTime < nextRejectionMessageGameTime) {
            return;
        }
        nextRejectionMessageGameTime = gameTime + REJECTION_MESSAGE_COOLDOWN_TICKS;
        player.sendSystemMessage(Component.translatable(
                "item.deadrecall.backpack.tooltip.no_nesting"
        ));
    }
}
