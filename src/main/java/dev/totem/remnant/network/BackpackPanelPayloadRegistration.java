package dev.totem.remnant.network;

import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Server-authoritative selection for the InventoryMenu backpack panel. */
public final class BackpackPanelPayloadRegistration {
    private BackpackPanelPayloadRegistration() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(BackpackSortPayload.TYPE, BackpackSortPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BackpackSortPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handleSort(context.player(), payload)));
        PayloadTypeRegistry.serverboundPlay().register(
                SelectBackpackPanelPayload.TYPE,
                SelectBackpackPanelPayload.CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                SelectBackpackPanelPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    var player = context.player();
                    if (player.containerMenu != player.inventoryMenu
                            || !(player.inventoryMenu instanceof BackpackPanelMenuAccess access)) {
                        return;
                    }

                    // Selection is deterministic on both sides from the backpack ItemStack already
                    // synchronized in the player's inventory. Do not broadcast menu state here:
                    // changing InventoryMenu state/stateId from a render-driven hover selection can
                    // race a vanilla click packet and restore the pre-click stack, producing an
                    // apparent (and potentially persistent) duplication.
                    access.totem$selectBackpackSlot(payload.inventorySlot());
                })
        );
    }
    /** Same validated path for packet handling and deterministic server tests. */
    public static boolean handleSort(net.minecraft.server.level.ServerPlayer player, BackpackSortPayload payload) {
        var menu = player.containerMenu;
        if (menu.containerId != payload.menuId() || menu.getStateId() != payload.stateId()) return false;
        if (menu == player.inventoryMenu && menu instanceof BackpackPanelMenuAccess access) {
            var panel = access.totem$getBackpackPanel();
            if (panel.selectedInventorySlot() != payload.selectedSlot()
                    || !net.minecraft.world.item.ItemStack.matches(panel.selectedBackpack(), payload.expectedBackpack())) return false;
        } else if (!(menu instanceof dev.totem.remnant.inventory.BackpackMenu)
                && !dev.totem.remnant.inventory.MatchingBackpackSort.isVanillaChest(menu)) return false;
        int hovered = payload.hoveredSlot();
        if (hovered < 0 || hovered >= menu.slots.size()) return false;
        var slot = menu.getSlot(hovered);
        if (!slot.isActive()) return false;
        boolean playerSide = slot.container == player.getInventory();
        if (!playerSide && !(slot instanceof dev.totem.remnant.inventory.BackpackPanelSlot)
                && !dev.totem.remnant.inventory.MatchingBackpackSort.isVanillaChest(menu)
                && (!(menu instanceof dev.totem.remnant.inventory.BackpackMenu b) || hovered >= b.storageSlotCount())) return false;
        return dev.totem.remnant.inventory.BackpackActions.sort(player, playerSide);
    }

}
