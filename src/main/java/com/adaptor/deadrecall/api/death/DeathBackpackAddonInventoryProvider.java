package com.adaptor.deadrecall.api.death;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Supplies player-owned addon slots that should follow the addon's own death-drop policy.
 * Providers must only return slots whose items are meant to drop for the current death.
 */
public interface DeathBackpackAddonInventoryProvider {
    Identifier id();

    List<? extends DeathBackpackAddonSlot> collectDroppableSlots(ServerPlayer player);
}
