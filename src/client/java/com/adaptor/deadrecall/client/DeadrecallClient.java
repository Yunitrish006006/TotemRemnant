package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.network.SortBackpackPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

public class DeadrecallClient implements ClientModInitializer {
    public static KeyMapping openDiscordConfigKey;
    public static KeyMapping sortBackpackKey;

    @Override
    public void onInitializeClient() {
        DeadRecallClientBootstrap.register();
    }

    public static void requestContainerSort(Minecraft mc, SortBackpackPayload.Target target) {
        if (mc.player == null) {
            return;
        }
        if (ClientPlayNetworking.canSend(SortBackpackPayload.TYPE)) {
            ClientPlayNetworking.send(new SortBackpackPayload(target));
        }
    }

    public static SortBackpackPayload.Target resolveSortTarget(AbstractContainerScreen<?> screen, Slot hoveredSlot, Minecraft mc) {
        if (screen.getMenu() instanceof InventoryMenu) {
            return SortBackpackPayload.Target.PLAYER;
        }

        if (hoveredSlot == null) {
            return null;
        }

        if (mc.player != null && hoveredSlot.container == mc.player.getInventory()) {
            return SortBackpackPayload.Target.PLAYER;
        }

        return SortBackpackPayload.Target.CONTAINER;
    }
}
