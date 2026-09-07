package com.adaptor.totem.api.death;

import net.minecraft.world.item.ItemStack;

/** Transactional view of an addon-owned player slot. */
public interface DeathBackpackAddonSlot {
    String sourceKey();

    ItemStack snapshot();

    boolean clearIfUnchanged(ItemStack expected);

    boolean restoreIfEmpty(ItemStack stack);
}
