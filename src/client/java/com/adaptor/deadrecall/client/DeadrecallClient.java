package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.network.DiscordConfigSyncPayload;
import com.adaptor.deadrecall.network.RequestDiscordConfigPayload;
import com.adaptor.deadrecall.network.SortBackpackPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class DeadrecallClient implements ClientModInitializer {

    public static KeyMapping openDiscordConfigKey;
    public static KeyMapping sortBackpackKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("deadrecall", "category")
        );

        // 預設無綁定鍵（GLFW_KEY_UNKNOWN = -1）
        openDiscordConfigKey = new KeyMapping(
                "key.deadrecall.discord_config",
                GLFW.GLFW_KEY_UNKNOWN,
                category
        );

        // 預設中鍵整理容器
        sortBackpackKey = new KeyMapping(
                "key.deadrecall.sort_backpack",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                category
        );

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openDiscordConfigKey.consumeClick()) {
                openDiscordConfigUi(mc);
            }
        });

        // 收到伺服器回傳的設定後，填入已開啟的畫面
        ClientPlayNetworking.registerGlobalReceiver(DiscordConfigSyncPayload.TYPE,
                (payload, context) -> {
                    net.minecraft.client.Minecraft mc = context.client();
                    mc.execute(() -> {
                        DiscordConfigScreen screen = DiscordConfigScreen.CURRENT;
                        if (screen != null) {
                            screen.applyServerConfig(payload.enabled(), payload.workerUrl(), payload.apiKey());
                            screen.applyChannels(payload.channels());
                        }
                    });
                });

        // 保留指令方式開啟
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("discordbridgeui")
                        .executes(context -> {
                            net.minecraft.client.Minecraft mc = context.getSource().getClient();
                            // 延到下一 tick 才開畫面，避免聊天欄關閉時把剛開的 UI 蓋掉
                            mc.execute(() -> openDiscordConfigUi(mc));
                            return 1;
                        })));
    }

    private void openDiscordConfigUi(net.minecraft.client.Minecraft mc) {
        DiscordConfigScreen screen = new DiscordConfigScreen();
        mc.setScreenAndShow(screen);
        // 向伺服器請求目前設定
        if (ClientPlayNetworking.canSend(RequestDiscordConfigPayload.TYPE)) {
            ClientPlayNetworking.send(new RequestDiscordConfigPayload());
        }
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
