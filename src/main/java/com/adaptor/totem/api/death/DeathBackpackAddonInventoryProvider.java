package com.adaptor.totem.api.death;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** API for addon-owned death-drop slots. */
public interface DeathBackpackAddonInventoryProvider {
    Identifier id();

    List<? extends DeathBackpackAddonSlot> collectDroppableSlots(ServerPlayer player);
}
