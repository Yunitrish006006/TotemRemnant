package dev.totem.remnant.mixin;

import dev.totem.remnant.inventory.BackpackPanelContainer;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import dev.totem.remnant.inventory.BackpackPanelSlot;
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

/** Adds bounded real backpack slots to the normal player InventoryMenu. */
@Mixin(InventoryMenu.class)
abstract class InventoryMenuBackpackPanelMixin implements BackpackPanelMenuAccess {
    @Unique private BackpackPanelContainer totem$backpackPanel;
    @Unique private int totem$backpackPanelSlotStart;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void totem$addBackpackPanelSlots(
            Inventory inventory,
            boolean active,
            Player owner,
            CallbackInfo callback
    ) {
        totem$backpackPanel = new BackpackPanelContainer(inventory);
        totem$backpackPanel.selectFirstBackpack();
        InventoryMenu menu = (InventoryMenu) (Object) this;
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
        InventoryMenu menu = (InventoryMenu) (Object) this;
        if (slotIndex >= totem$backpackPanelSlotStart
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
        return totem$backpackPanel.select(inventorySlot);
    }

    @Override
    public boolean totem$layoutBackpackSlots(int relativeLeft, int relativeTop, int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("Backpack panel columns must be positive");
        }
        InventoryMenu menu = (InventoryMenu) (Object) this;
        boolean changed = false;
        int activeSlots = totem$backpackPanel.activeSlotCount();
        for (int panelSlot = 0; panelSlot < BackpackPanelContainer.MAX_PANEL_SLOTS; panelSlot++) {
            int menuSlot = totem$backpackPanelSlotStart + panelSlot;
            int x = panelSlot < activeSlots
                    ? relativeLeft + panelSlot % columns * 18
                    : -10_000;
            int y = panelSlot < activeSlots
                    ? relativeTop + panelSlot / columns * 18
                    : -10_000;
            Slot current = menu.slots.get(menuSlot);
            if (current.x == x && current.y == y
                    && current instanceof BackpackPanelSlot) {
                continue;
            }
            BackpackPanelSlot replacement = new BackpackPanelSlot(
                    totem$backpackPanel,
                    panelSlot,
                    x,
                    y
            );
            replacement.index = menuSlot;
            menu.slots.set(menuSlot, replacement);
            changed = true;
        }
        return changed;
    }

    @Unique
    private ItemStack totem$moveFromPanel(Player player, InventoryMenu menu, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot source = menu.slots.get(slotIndex);
        if (!source.isActive() || !source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        if (!((AbstractContainerMenuInvoker) this).totem$moveItemStackTo(
                stack,
                InventoryMenu.INV_SLOT_START,
                InventoryMenu.USE_ROW_SLOT_END,
                true
        )) {
            return ItemStack.EMPTY;
        }
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
