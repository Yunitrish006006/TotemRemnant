package dev.totem.remnant.network;

import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Server-authoritative selection for the InventoryMenu backpack panel. */
public final class BackpackPanelPayloadRegistration {
    private BackpackPanelPayloadRegistration() {
    }

    public static void register() {
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
}
