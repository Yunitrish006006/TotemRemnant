package dev.totem.remnant.inventory;

import dev.totem.remnant.mixin.AbstractContainerMenuInvoker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared state and operations; native menus retain their own functional-slot behavior. */
public final class BackpackPanelAttachment {
    private final AbstractContainerMenu menu;
    private final BackpackPanelContainer panel;
    private final int slotStart;

    private BackpackPanelAttachment(AbstractContainerMenu menu, Inventory inventory) {
        this.menu = menu;
        this.panel = new BackpackPanelContainer(inventory);
        panel.selectFirstBackpack();
        slotStart = menu.slots.size();
        for (int slot = 0; slot < BackpackPanelContainer.MAX_PANEL_SLOTS; slot++) {
            ((AbstractContainerMenuInvoker) menu).totem$addSlot(new BackpackPanelSlot(panel, slot, -10_000, -10_000));
        }
    }

    /** Called only at the completed native constructor tail. Unknown classes fail closed. */
    public static BackpackPanelAttachment attach(AbstractContainerMenu menu) {
        var adapter = BackpackSidebarAdapters.find(menu);
        if (adapter == null || menu.slots.size() != adapter.nativeSlotCount(menu)) return null;
        Inventory inventory = menu.slots.stream().map(slot -> slot.container)
                .filter(Inventory.class::isInstance).map(Inventory.class::cast).findFirst().orElseThrow();
        return new BackpackPanelAttachment(menu, inventory);
    }

    public BackpackPanelContainer panel() { return panel; }
    public int slotStart() { return slotStart; }
    public boolean contains(int index) {
        return index >= slotStart && index < slotStart + BackpackPanelContainer.MAX_PANEL_SLOTS;
    }

    public boolean layoutWindow(int relativeLeft, int relativeTop, int columns, int firstRow, int rows) {
        if (panel == null) return false;
        if (columns <= 0 || firstRow < 0 || rows < 0) {
            throw new IllegalArgumentException("Backpack panel columns must be positive");
        }
        boolean changed = false;
        int activeSlots = panel.activeSlotCount();
        for (int panelSlot = 0; panelSlot < BackpackPanelContainer.MAX_PANEL_SLOTS; panelSlot++) {
            int menuSlot = slotStart + panelSlot;
            long offset = panelSlot - (long) firstRow * columns;
            boolean visible = panelSlot < activeSlots && offset >= 0 && offset / columns < rows;
            int x = visible
                    ? relativeLeft + (int) (offset % columns) * 18
                    : -10_000;
            int y = visible
                    ? relativeTop + (int) (offset / columns) * 18
                    : -10_000;
            Slot current = menu.slots.get(menuSlot);
            if (!(current instanceof BackpackPanelSlot)
                    || !(current instanceof MutableSlotPosition mutable)) {
                throw new IllegalStateException("Backpack panel slot identity was replaced");
            }
            if (current.x == x && current.y == y) {
                continue;
            }
            mutable.totem$setPosition(x, y);
            changed = true;
        }
        return changed;
    }

    public ItemStack quickMove(Player player, int slotIndex) {
        if (!contains(slotIndex) || slotIndex >= menu.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot source = menu.slots.get(slotIndex);
        if (!source.isActive() || !source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        boolean moved = false;
        // Only real main inventory/hotbar destinations, never native functional slots or equipment.
        for (int i = slotStart - 1; i >= 0 && !stack.isEmpty(); i--) {
            Slot target = menu.getSlot(i);
            if (target.container == player.getInventory() && target.getContainerSlot() < 36
                    && target.getContainerSlot() >= 0) {
                moved |= ((AbstractContainerMenuInvoker) menu).totem$moveItemStackTo(stack, i, i + 1, false);
            }
        }
        if (!moved) return ItemStack.EMPTY;
        totem$finishQuickMove(player, source, stack, original);
        return original;
    }

    private static void totem$finishQuickMove(
            Player player,
            Slot source,
            ItemStack remaining,
            ItemStack original
    ) {
        if (remaining.isEmpty()) {
            source.setByPlayer(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        if (remaining.getCount() != original.getCount()) {
            source.onTake(player, remaining);
        }
    }
}
