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
                    if (access.totem$selectBackpackSlot(payload.inventorySlot())) {
                        player.inventoryMenu.broadcastFullState();
                    }
                })
        );
    }
}
