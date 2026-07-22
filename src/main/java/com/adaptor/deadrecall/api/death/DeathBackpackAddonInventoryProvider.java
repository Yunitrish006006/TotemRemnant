package com.adaptor.deadrecall.api.death;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;

/** Compatibility API for addon-owned death-drop slots. */
public interface DeathBackpackAddonInventoryProvider {
    Identifier id();
    List<? extends DeathBackpackAddonSlot> collectDroppableSlots(ServerPlayer player);
}
