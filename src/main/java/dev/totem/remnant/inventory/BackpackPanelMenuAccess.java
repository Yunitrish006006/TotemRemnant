package dev.totem.remnant.inventory;

/** Mixin bridge shared by server selection validation and the client layout. */
public interface BackpackPanelMenuAccess {
    BackpackPanelContainer totem$getBackpackPanel();

    int totem$getBackpackPanelSlotStart();

    boolean totem$selectBackpackSlot(int inventorySlot);

    boolean totem$layoutBackpackSlots(int relativeLeft, int relativeTop, int columns);
}
