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
        var client = net.minecraft.client.Minecraft.getInstance();
        return apply(screen, hovered, client.mouseHandler.getScaledXPos(client.getWindow()),
                client.mouseHandler.getScaledYPos(client.getWindow()));
    }

    public boolean apply(AbstractContainerScreen<?> screen, Slot hovered, double mouseX, double mouseY) {
        var menu = screen.getMenu();
        if (!(menu instanceof BackpackMenu) && !BackpackPanelMenuAccess.supports(menu)
                && !dev.totem.remnant.inventory.MatchingBackpackSort.isVanillaChest(menu)) return false;
        if (screen instanceof ObserverReadOnlyScreen observer && observer.totem$isObserverReadOnly()) return true;
        if (screen instanceof dev.totem.remnant.client.screen.BackpackScreen backpack
                && backpack.containsStorage(mouseX, mouseY)) {
            requestRegion(menu, 0, ((BackpackMenu) menu).storageSlotCount());
            return true;
        }
        if (screen instanceof dev.totem.remnant.client.screen.BackpackSidebarLayout layout) {
            // VanillaTweaks can supply the previous frame's hovered slot. Resolve the current pointer.
            hovered = layout.remnant$hoveredSlot(mouseX, mouseY);
            if (layout.remnant$contains(mouseX, mouseY) && BackpackPanelMenuAccess.supports(menu)
                    && menu instanceof BackpackPanelMenuAccess access) {
                // The whole sidebar sorts its selected backpack, including empty slots and frame.
                requestRegion(menu, access.totem$getBackpackPanelSlotStart(), access.totem$getBackpackPanel().activeSlotCount());
                return true;
            }
        }
        if (hovered == null || !hovered.isActive()) return true;
        if (dev.totem.remnant.inventory.BackpackPanelContainer.isBackpack(hovered.getItem())) {
            if (ClientPlayNetworking.canSend(dev.totem.remnant.network.SortHoveredBackpackPayload.TYPE))
                ClientPlayNetworking.send(dev.totem.remnant.network.SortHoveredBackpackPayload.forSlot(menu, hovered));
            return true;
        }
        requestSurface(menu, hovered);
        return true;
    }

    private static void requestRegion(net.minecraft.world.inventory.AbstractContainerMenu menu, int start, int count) {
        for (int i = start; i >= 0 && i < Math.min(menu.slots.size(), start + count); i++) {
            Slot anchor = menu.getSlot(i);
            if (anchor.isActive()) { requestSurface(menu, anchor); return; }
        }
    }

    private static void requestSurface(net.minecraft.world.inventory.AbstractContainerMenu menu, Slot hovered) {
        int selected = -1;
        ItemStack expected = ItemStack.EMPTY;
        if (BackpackPanelMenuAccess.supports(menu) && menu instanceof BackpackPanelMenuAccess access) {
            selected = access.totem$getBackpackPanel().selectedInventorySlot();
            expected = access.totem$getBackpackPanel().selectedBackpack().copy();
        }
        if (ClientPlayNetworking.canSend(BackpackSortPayload.TYPE)) {
            ClientPlayNetworking.send(new BackpackSortPayload(menu.containerId, menu.getStateId(), hovered.index, selected, expected));
        }
    }
}
