package com.adaptor.deadrecall.api.death;

import net.minecraft.world.item.ItemStack;

/**
 * Transactional access to one player-owned slot supplied by an addon inventory integration.
 * Implementations are invoked on the authoritative server thread.
 */
public interface DeathBackpackAddonSlot {
    /** Stable diagnostic name, for example {@code chest/cape/0}. */
    String sourceKey();

    /** Returns a copy of the current authoritative stack. */
    ItemStack snapshot();

    /**
     * Clears this slot only when it still contains the expected stack.
     *
     * @return true when the slot was cleared, false when it became invalid or changed
     */
    boolean clearIfUnchanged(ItemStack expected);

    /**
     * Restores a stack only when the source is still valid and empty.
     *
     * @return true when restored to the original addon slot
     */
    boolean restoreIfEmpty(ItemStack stack);
}
