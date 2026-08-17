package dev.totem.remnant.client.manual;

import dev.totem.remnant.network.RemnantManualRecipesPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Immutable client snapshot used by the written-book recipe renderer. */
public final class RemnantManualRecipeCache {
    private static volatile Map<String, RemnantManualRecipesPayload.Entry> recipes = Map.of();
    private static volatile boolean synchronizedFromServer;

    private RemnantManualRecipeCache() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                RemnantManualRecipesPayload.TYPE,
                (payload, context) -> {
                    recipes = payload.recipes().stream().collect(Collectors.toUnmodifiableMap(
                            RemnantManualRecipesPayload.Entry::id,
                            Function.identity()
                    ));
                    synchronizedFromServer = true;
                }
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    public static RemnantManualRecipesPayload.Entry get(String id) {
        return recipes.get(id);
    }

    public static boolean isSynchronizedFromServer() {
        return synchronizedFromServer;
    }

    private static void clear() {
        recipes = Map.of();
        synchronizedFromServer = false;
    }
}
