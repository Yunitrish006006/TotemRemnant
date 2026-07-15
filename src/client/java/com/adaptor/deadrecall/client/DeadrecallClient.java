package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.item.copper.CopperGolemMenu;
import com.adaptor.deadrecall.menu.ModMenus;
import com.adaptor.deadrecall.mixin.client.MenuScreensAccessor;
import com.adaptor.deadrecall.network.DiscordConfigSyncPayload;
import com.adaptor.deadrecall.network.CopperWrenchBindingsPayload;
import com.adaptor.deadrecall.network.RequestDiscordConfigPayload;
import com.adaptor.deadrecall.network.SortBackpackPayload;
import com.adaptor.deadrecall.network.SpaceUnitFriendsPayload;
import com.adaptor.deadrecall.network.SpaceUnitMapPayload;
import com.adaptor.deadrecall.network.SpaceUnitRegistrationPreviewPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Proxy;

public class DeadrecallClient implements ClientModInitializer {
    public static KeyMapping openDiscordConfigKey;
    public static KeyMapping sortBackpackKey;
    private static final int DISCORD_CONFIG_OPEN_TIMEOUT_TICKS = 100;
    private static int pendingDiscordConfigOpenTicks = 0;

    @Override
    public void onInitializeClient() {
        registerCopperGolemScreen();
        CopperGolemVisualizationClient.initialize();

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
            if (pendingDiscordConfigOpenTicks > 0) {
                pendingDiscordConfigOpenTicks--;
            }
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
                        if (screen == null && pendingDiscordConfigOpenTicks > 0) {
                            screen = new DiscordConfigScreen();
                            mc.setScreenAndShow(screen);
                        }
                        pendingDiscordConfigOpenTicks = 0;
                        if (screen != null) {
                            screen.applyServerConfig(payload.enabled(), payload.workerUrl(), payload.apiKey());
                            screen.applyChannels(payload.channels());
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(CopperWrenchBindingsPayload.TYPE,
                (payload, context) -> {
                    net.minecraft.client.Minecraft mc = context.client();
                    mc.execute(() -> CopperWrenchBindingsScreen.receivePayload(payload));
                });

        ClientPlayNetworking.registerGlobalReceiver(SpaceUnitMapPayload.TYPE,
                (payload, context) -> {
                    net.minecraft.client.Minecraft mc = context.client();
                    mc.execute(() -> {
                        SpaceUnitMapScreen screen = SpaceUnitMapScreen.CURRENT;
                        if (screen != null && screen.isFor(payload.sourceType(), payload.sourceUnitId())) {
                            screen.applyPayload(payload);
                        } else {
                            mc.setScreenAndShow(new SpaceUnitMapScreen(payload));
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(SpaceUnitFriendsPayload.TYPE,
                (payload, context) -> {
                    net.minecraft.client.Minecraft mc = context.client();
                    mc.execute(() -> {
                        SpaceUnitFriendsScreen screen = SpaceUnitFriendsScreen.CURRENT;
                        if (screen != null) {
                            screen.applyPayload(payload);
                        } else {
                            mc.setScreenAndShow(new SpaceUnitFriendsScreen(null, payload));
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(SpaceUnitRegistrationPreviewPayload.TYPE,
                (payload, context) -> {
                    net.minecraft.client.Minecraft mc = context.client();
                    mc.execute(() -> mc.setScreenAndShow(new SpaceUnitRegistrationPreviewScreen(payload)));
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

    private void registerCopperGolemScreen() {
        try {
            Class<?> constructorClass = Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            Object factory = Proxy.newProxyInstance(
                    constructorClass.getClassLoader(),
                    new Class<?>[]{constructorClass},
                    (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "create" -> new CopperWrenchBindingsScreen(
                                    (CopperGolemMenu) args[0],
                                    (Inventory) args[1],
                                    (Component) args[2]
                            );
                            case "fromPacket" -> null;
                            case "toString" -> "DeadRecall copper golem screen factory";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException("Unsupported MenuScreens factory method: " + method);
                        };
                    }
            );
            MenuScreensAccessor.deadrecall$getScreens().put(ModMenus.COPPER_GOLEM, factory);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register copper golem screen", e);
        }
    }

    private void openDiscordConfigUi(net.minecraft.client.Minecraft mc) {
        if (ClientPlayNetworking.canSend(RequestDiscordConfigPayload.TYPE)) {
            pendingDiscordConfigOpenTicks = DISCORD_CONFIG_OPEN_TIMEOUT_TICKS;
            ClientPlayNetworking.send(new RequestDiscordConfigPayload());
        } else if (mc.player != null) {
            mc.player.sendSystemMessage(Component.translatable("message.deadrecall.discord_config.request_failed").withStyle(ChatFormatting.RED));
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
