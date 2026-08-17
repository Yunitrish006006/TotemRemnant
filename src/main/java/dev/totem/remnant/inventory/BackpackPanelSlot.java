package dev.totem.remnant.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Real InventoryMenu slot whose availability follows the selected backpack. */
public final class BackpackPanelSlot extends Slot {
    private final BackpackPanelContainer backpack;

    public BackpackPanelSlot(BackpackPanelContainer backpack, int index, int x, int y) {
        super(backpack, index, x, y);
        this.backpack = backpack;
    }

    @Override
    public boolean isActive() {
        return backpack.isSlotActive(getContainerSlot());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return backpack.canPlaceItem(getContainerSlot(), stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return isActive();
    }
}
