package dev.totem.remnant.client.integration;

import dev.totem.core.api.v1.client.observer.ObserverReadOnlyScreen;
import dev.totem.remnant.inventory.BackpackMenu;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import dev.totem.remnant.network.BackpackSortPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.function.BiFunction;

/** Shared dispatch for native middle-click and VanillaTweaks' configurable v1 binding. */
public final class RemnantSortClientProvider implements BiFunction<AbstractContainerScreen<?>, Slot, Boolean> {
    @Override public Boolean apply(AbstractContainerScreen<?> screen, Slot hovered) {
        var menu = screen.getMenu();
        if (!(menu instanceof BackpackMenu) && !BackpackPanelMenuAccess.supports(menu)
                && !dev.totem.remnant.inventory.MatchingBackpackSort.isVanillaChest(menu)) return false;
        if (screen instanceof ObserverReadOnlyScreen observer && observer.totem$isObserverReadOnly()) return true;
        if (hovered == null || !hovered.isActive()) return true;
        if (dev.totem.remnant.inventory.BackpackPanelContainer.isBackpack(hovered.getItem())) {
            if (ClientPlayNetworking.canSend(dev.totem.remnant.network.SortHoveredBackpackPayload.TYPE))
                ClientPlayNetworking.send(dev.totem.remnant.network.SortHoveredBackpackPayload.forSlot(menu, hovered));
            return true;
        }
        int selected = -1;
        ItemStack expected = ItemStack.EMPTY;
        if (BackpackPanelMenuAccess.supports(menu) && menu instanceof BackpackPanelMenuAccess access) {
            selected = access.totem$getBackpackPanel().selectedInventorySlot();
            expected = access.totem$getBackpackPanel().selectedBackpack().copy();
        }
        if (ClientPlayNetworking.canSend(BackpackSortPayload.TYPE)) {
            ClientPlayNetworking.send(new BackpackSortPayload(menu.containerId, menu.getStateId(), hovered.index, selected, expected));
        }
        return true;
    }
}
