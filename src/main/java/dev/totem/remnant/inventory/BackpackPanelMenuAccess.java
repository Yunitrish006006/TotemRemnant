package dev.totem.remnant.inventory;

/** Mixin bridge shared by server selection validation and the client layout. */
public interface BackpackPanelMenuAccess {
    static boolean supports(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        return isNativeClass(menu) && menu instanceof BackpackPanelMenuAccess access
                && access.totem$getBackpackPanel() != null;
    }

    static boolean isNativeClass(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        Class<?> type = menu.getClass();
        return type == net.minecraft.world.inventory.InventoryMenu.class
                || type == net.minecraft.world.inventory.CraftingMenu.class
                || type == net.minecraft.world.inventory.EnchantmentMenu.class
                || type == net.minecraft.world.inventory.ChestMenu.class;
    }

    BackpackPanelContainer totem$getBackpackPanel();

    int totem$getBackpackPanelSlotStart();

    boolean totem$selectBackpackSlot(int inventorySlot);

    boolean totem$layoutBackpackSlots(int relativeLeft, int relativeTop, int columns);
    boolean totem$layoutBackpackWindow(int relativeLeft, int relativeTop, int columns, int firstRow, int rows);
}
