package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.item.copper.CopperGolemMenu;
import com.adaptor.deadrecall.registry.TotemAutomataMenuRegistration;
import com.adaptor.deadrecall.mixin.client.MenuScreensAccessor;
import com.adaptor.deadrecall.network.CopperWrenchBindingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.lang.reflect.Proxy;

/**
 * Owns client registration for the future TotemAutomata module.
 */
public final class TotemAutomataClientBootstrap {
    private TotemAutomataClientBootstrap() {
    }

    public static void registerScreens() {
        registerCopperGolemScreen();
        CopperGolemVisualizationClient.initialize();
    }

    public static void registerNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(CopperWrenchBindingsPayload.TYPE,
                (payload, context) -> {
                    net.minecraft.client.Minecraft mc = context.client();
                    mc.execute(() -> CopperWrenchBindingsScreen.receivePayload(payload));
                });
    }

    private static void registerCopperGolemScreen() {
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
            MenuScreensAccessor.deadrecall$getScreens().put(TotemAutomataMenuRegistration.COPPER_GOLEM, factory);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register copper golem screen", e);
        }
    }
}
