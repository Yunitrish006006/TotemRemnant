package dev.totem.remnant.client;

import dev.totem.remnant.client.manual.RemnantManualRecipeCache;
import dev.totem.remnant.client.manual.RemnantManualPageOverlay;
import dev.totem.remnant.client.screen.BackpackScreen;
import dev.totem.remnant.registry.BackpackMenuRegistration;
import dev.totem.remnant.network.RemnantRulesPayload;
import dev.totem.remnant.registry.RemnantGameRules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.player.Inventory;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;

/** Registers the custom backpack screen used by the extended menu type. */
public final class TotemRemnantClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RemnantManualRecipeCache.register();
        RemnantManualPageOverlay.register();
        ClientPlayNetworking.registerGlobalReceiver(
                RemnantRulesPayload.TYPE,
                (payload, context) -> RemnantGameRules.updateClientRules(
                        payload.generateDeathBackpacks(),
                        payload.deathBackpackOwnerPickupOnly(),
                        payload.preventPortableContainerNesting())
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                RemnantGameRules.resetClientRules());
        registerScreen();
    }

    /** Minecraft 26.2 keeps the vanilla registration method private; install the same factory map entry. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerScreen() {
        try {
            Class<?> constructorType = Class.forName(
                    "net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            Object constructor = Proxy.newProxyInstance(
                    TotemRemnantClient.class.getClassLoader(),
                    new Class<?>[] {constructorType},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("create") && arguments != null && arguments.length == 3) {
                            return new BackpackScreen(
                                    (dev.totem.remnant.inventory.BackpackMenu) arguments[0],
                                    (Inventory) arguments[1],
                                    (net.minecraft.network.chat.Component) arguments[2]);
                        }
                        return null;
                    });
            Field screensField = MenuScreens.class.getDeclaredField("SCREENS");
            screensField.setAccessible(true);
            ((Map) screensField.get(null)).put(BackpackMenuRegistration.BACKPACK, constructor);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not register Remnant backpack screen", exception);
        }
    }
}
