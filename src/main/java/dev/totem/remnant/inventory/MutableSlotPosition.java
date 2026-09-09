package dev.totem.remnant.inventory;

/**
 * Narrow bridge used by the InventoryMenu backpack panel to reposition its
 * already-registered slots without replacing Slot instances mid-screen.
 */
public interface MutableSlotPosition {
    void totem$setPosition(int x, int y);
}
