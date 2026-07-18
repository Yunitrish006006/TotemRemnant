package com.adaptor.deadrecall.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side chest menu used by both normal and death backpacks.
 *
 * <p>The ItemStack that owns this inventory must remain in the player's inventory while the
 * menu is open. Vanilla ChestMenu allows every player inventory slot to participate in
 * quick-move and swap operations, so the owning backpack needs an explicit identity lock.</p>
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
            BackpackInventory backpackInventory,
            int rows
    ) {
        super(menuType, containerId, playerInventory, backpackInventory, rows);
        this.trackedBackpackStack = backpackInventory.getBackpackStack();
        this.backpackSlotCount = rows * 9;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot sourceSlot = this.slots.get(slotIndex);
        if (slotIndex >= this.backpackSlotCount
                && !PortableContainerPolicy.mayInsertIntoBackpack(sourceSlot.getItem())) {
            // Restricted items already present in legacy backpack contents can still move out.
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
                && slotIndex < this.slots.size()
                && this.slots.get(slotIndex).getItem() == this.trackedBackpackStack;
    }

    private boolean swapsTrackedBackpackFromInventory(Inventory inventory, ContainerInput input, int inventorySlot) {
        return input == ContainerInput.SWAP
                && inventorySlot >= 0
                && inventorySlot < inventory.getContainerSize()
                && inventory.getItem(inventorySlot) == this.trackedBackpackStack;
    }

    private boolean collectsRestrictedContainersWithPickupAll(ContainerInput input) {
        return input == ContainerInput.PICKUP_ALL
                && PortableContainerPolicy.isRestrictedPortableContainer(this.getCarried());
    }

    private boolean insertsRestrictedCarriedStack(int slotIndex, ContainerInput input) {
        if (slotIndex < 0 || slotIndex >= this.backpackSlotCount) {
            return false;
        }
        if (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_CRAFT) {
            return false;
        }
        return !PortableContainerPolicy.mayInsertIntoBackpack(this.getCarried());
    }

    private boolean swapsRestrictedInventoryStackIntoBackpack(
            int slotIndex,
            Inventory inventory,
            ContainerInput input,
            int inventorySlot
    ) {
        return slotIndex >= 0
                && slotIndex < this.backpackSlotCount
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
        if (gameTime < this.nextRejectionMessageGameTime) {
            return;
        }
        this.nextRejectionMessageGameTime = gameTime + REJECTION_MESSAGE_COOLDOWN_TICKS;
        player.sendSystemMessage(Component.translatable("item.deadrecall.backpack.tooltip.no_nesting"));
    }
}
