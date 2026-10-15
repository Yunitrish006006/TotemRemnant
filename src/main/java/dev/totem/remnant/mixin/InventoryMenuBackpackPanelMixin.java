package dev.totem.remnant.mixin;

import dev.totem.remnant.inventory.BackpackPanelContainer;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import dev.totem.remnant.inventory.BackpackPanelSlot;
import dev.totem.remnant.inventory.MutableSlotPosition;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Appends bounded real slots after each supported native menu has constructed its slots. */
@Mixin(InventoryMenu.class)
abstract class InventoryMenuBackpackPanelMixin implements BackpackPanelMenuAccess {
    @Unique private BackpackPanelContainer totem$backpackPanel;
    @Unique private int totem$backpackPanelSlotStart;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void totem$addBackpackPanelSlots(
            CallbackInfo callback
    ) {
        if (totem$backpackPanel != null) return; // Delegating constructors also reach TAIL.
        var nativeMenu = (net.minecraft.world.inventory.AbstractContainerMenu) (Object) this;
        if (!BackpackPanelMenuAccess.isNativeClass(nativeMenu)) return;
        Inventory inventory = nativeMenu.slots.stream().map(slot -> slot.container)
                .filter(Inventory.class::isInstance).map(Inventory.class::cast).findFirst().orElseThrow();
        totem$backpackPanel = new BackpackPanelContainer(inventory);
        totem$backpackPanel.selectFirstBackpack();
        var menu = (net.minecraft.world.inventory.AbstractContainerMenu) (Object) this;
        totem$backpackPanelSlotStart = menu.slots.size();
        for (int slot = 0; slot < BackpackPanelContainer.MAX_PANEL_SLOTS; slot++) {
            ((AbstractContainerMenuInvoker) this).totem$addSlot(
                    new BackpackPanelSlot(totem$backpackPanel, slot, -10_000, -10_000)
            );
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void totem$quickMoveBackpackPanel(
            Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> callback
    ) {
        var menu = (net.minecraft.world.inventory.AbstractContainerMenu) (Object) this;
        if (totem$backpackPanel != null && slotIndex >= totem$backpackPanelSlotStart
                && slotIndex < totem$backpackPanelSlotStart + BackpackPanelContainer.MAX_PANEL_SLOTS) {
            callback.setReturnValue(totem$moveFromPanel(player, menu, slotIndex));
        }
        // Deliberately do not intercept ordinary InventoryMenu slots here. Shift-clicking the
        // player's inventory/hotbar must retain vanilla quick-move behavior even while the E-screen
        // backpack panel is visible. The panel remains interactive, and Shift-clicking a panel slot
        // still moves that stack back into the normal player inventory.
    }

    @Override
    public BackpackPanelContainer totem$getBackpackPanel() {
        return totem$backpackPanel;
    }

    @Override
    public int totem$getBackpackPanelSlotStart() {
        return totem$backpackPanelSlotStart;
    }

    @Override
    public boolean totem$selectBackpackSlot(int inventorySlot) {
        return totem$backpackPanel != null && totem$backpackPanel.select(inventorySlot);
    }

    @Override
    public boolean totem$layoutBackpackSlots(int relativeLeft, int relativeTop, int columns) {
        return totem$layoutBackpackWindow(relativeLeft, relativeTop, columns, 0, Integer.MAX_VALUE);
    }

    @Override
    public boolean totem$layoutBackpackWindow(int relativeLeft, int relativeTop, int columns, int firstRow, int rows) {
        if (totem$backpackPanel == null) return false;
        if (columns <= 0 || firstRow < 0 || rows < 0) {
            throw new IllegalArgumentException("Backpack panel columns must be positive");
        }
        var menu = (net.minecraft.world.inventory.AbstractContainerMenu) (Object) this;
        boolean changed = false;
        int activeSlots = totem$backpackPanel.activeSlotCount();
        for (int panelSlot = 0; panelSlot < BackpackPanelContainer.MAX_PANEL_SLOTS; panelSlot++) {
            int menuSlot = totem$backpackPanelSlotStart + panelSlot;
            int offset = panelSlot - firstRow * columns;
            boolean visible = panelSlot < activeSlots && offset >= 0 && offset / columns < rows;
            int x = visible
                    ? relativeLeft + offset % columns * 18
                    : -10_000;
            int y = visible
                    ? relativeTop + offset / columns * 18
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

    @Unique
    private ItemStack totem$moveFromPanel(Player player, net.minecraft.world.inventory.AbstractContainerMenu menu, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
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
        for (int i = totem$backpackPanelSlotStart - 1; i >= 0 && !stack.isEmpty(); i--) {
            Slot target = menu.getSlot(i);
            if (target.container == player.getInventory() && target.getContainerSlot() < 36
                    && target.getContainerSlot() >= 0) {
                moved |= ((AbstractContainerMenuInvoker) this).totem$moveItemStackTo(stack, i, i + 1, false);
            }
        }
        if (!moved) return ItemStack.EMPTY;
        totem$finishQuickMove(player, source, stack, original);
        return original;
    }

    @Unique
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
