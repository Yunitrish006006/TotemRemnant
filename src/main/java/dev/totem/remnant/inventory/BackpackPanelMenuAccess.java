package dev.totem.remnant.inventory;

/** Mixin bridge shared by server selection validation and the client layout. */
public interface BackpackPanelMenuAccess {
    static boolean supports(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        return isNativeClass(menu) && menu instanceof BackpackPanelMenuAccess access
                && access.totem$getBackpackPanel() != null;
    }

    static boolean isNativeClass(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        return BackpackSidebarAdapters.find(menu) != null;
    }

    BackpackPanelAttachment totem$getBackpackPanelAttachment();

    default BackpackPanelContainer totem$getBackpackPanel() {
        var attachment = totem$getBackpackPanelAttachment();
        return attachment == null ? null : attachment.panel();
    }

    default int totem$getBackpackPanelSlotStart() {
        var attachment = totem$getBackpackPanelAttachment();
        return attachment == null ? -1 : attachment.slotStart();
    }

    default boolean totem$selectBackpackSlot(int inventorySlot) {
        var panel = totem$getBackpackPanel();
        return panel != null && panel.select(inventorySlot);
    }

    default boolean totem$layoutBackpackSlots(int relativeLeft, int relativeTop, int columns) {
        return totem$layoutBackpackWindow(relativeLeft, relativeTop, columns, 0, Integer.MAX_VALUE);
    }

    default boolean totem$layoutBackpackWindow(int relativeLeft, int relativeTop, int columns, int firstRow, int rows) {
        var attachment = totem$getBackpackPanelAttachment();
        return attachment != null && attachment.layoutWindow(relativeLeft, relativeTop, columns, firstRow, rows);
    }
}
