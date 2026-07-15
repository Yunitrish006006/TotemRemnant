package com.adaptor.deadrecall.inventory;

import com.adaptor.deadrecall.item.BackpackItemHelper;
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
    private final ItemStack trackedBackpackStack;
    private final int backpackSlotCount;

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
        if (slotIndex >= this.backpackSlotCount && BackpackItemHelper.isBackpackItem(sourceSlot.getItem())) {
            // Do not let any backpack in the player's inventory quick-move into the open backpack.
            // Backpacks already present in corrupted legacy contents can still be quick-moved out.
            return ItemStack.EMPTY;
        }

        return super.quickMoveStack(player, slotIndex);
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (targetsTrackedBackpack(slotIndex)
                || swapsTrackedBackpackFromInventory(player.getInventory(), input, buttonNum)
                || collectsBackpacksWithPickupAll(input)) {
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

    private boolean collectsBackpacksWithPickupAll(ContainerInput input) {
        return input == ContainerInput.PICKUP_ALL && BackpackItemHelper.isBackpackItem(this.getCarried());
    }
}
