package com.adaptor.deadrecall.api.death;

import net.minecraft.world.item.ItemStack;

/** Transactional compatibility view of an addon-owned player slot. */
public interface DeathBackpackAddonSlot {
    String sourceKey();
    ItemStack snapshot();
    boolean clearIfUnchanged(ItemStack expected);
    boolean restoreIfEmpty(ItemStack stack);
}
