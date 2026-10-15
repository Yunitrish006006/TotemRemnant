package dev.totem.remnant.integration.sorting;

import dev.totem.remnant.inventory.BackpackActions;
import dev.totem.remnant.inventory.BackpackMenu;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiFunction;

/** Optional v1 generic-request guard, including player sorting with a pinned panel backpack. */
public final class RemnantSortProvider implements BiFunction<ServerPlayer, Boolean, Boolean> {
    @Override public Boolean apply(ServerPlayer player, Boolean playerSide) {
        if (dev.totem.remnant.inventory.MatchingBackpackSort.isVanillaChest(player.containerMenu)) return false;
        if (!(player.containerMenu instanceof BackpackMenu) && player.containerMenu != player.inventoryMenu) return null;
        // An unbound generic request may sort the player surface only. Remnant storage
        // requires the dedicated menu/state/selection-bound payload.
        return playerSide && BackpackActions.sort(player, true);
    }
}
