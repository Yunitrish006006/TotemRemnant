package dev.totem.remnant.registry;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import dev.totem.remnant.network.RemnantRulesPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/** Persistent per-world switches for Remnant gameplay. */
public final class RemnantGameRules {
    public static final GameRule<Boolean> GENERATE_DEATH_BACKPACKS =
            GameRuleBuilder.forBoolean(true)
                    .category(GameRuleCategory.DROPS)
                    .buildAndRegister(Identifier.fromNamespaceAndPath(
                            "totem", "remnant_generate_death_backpacks"));

    public static final GameRule<Boolean> DEATH_BACKPACK_OWNER_PICKUP_ONLY =
            GameRuleBuilder.forBoolean(true)
                    .category(GameRuleCategory.DROPS)
                    .buildAndRegister(Identifier.fromNamespaceAndPath(
                            "totem", "remnant_death_backpack_owner_pickup_only"));

    public static final GameRule<Boolean> PREVENT_PORTABLE_CONTAINER_NESTING =
            GameRuleBuilder.forBoolean(true)
                    .category(GameRuleCategory.MISC)
                    .buildAndRegister(Identifier.fromNamespaceAndPath(
                            "totem", "remnant_prevent_portable_container_nesting"));

    private static volatile MinecraftServer activeServer;
    private static volatile boolean clientGeneratesDeathBackpacks = true;
    private static volatile boolean clientDeathBackpackOwnerPickupOnly = true;
    private static volatile boolean clientPreventsPortableContainerNesting = true;
    private static volatile boolean clientRulesSynchronized;

    private RemnantGameRules() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(
                RemnantRulesPayload.TYPE,
                RemnantRulesPayload.CODEC
        );
        ServerLifecycleEvents.SERVER_STARTED.register(server -> activeServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (activeServer == server) {
                activeServer = null;
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sendRules(handler.getPlayer()));
        registerRuleSync(GENERATE_DEATH_BACKPACKS);
        registerRuleSync(DEATH_BACKPACK_OWNER_PICKUP_ONLY);
        registerRuleSync(PREVENT_PORTABLE_CONTAINER_NESTING);
    }

    public static boolean generateDeathBackpacks(ServerLevel level) {
        return level.getGameRules().get(GENERATE_DEATH_BACKPACKS);
    }

    public static boolean deathBackpackOwnerPickupOnly(ServerLevel level) {
        return level.getGameRules().get(DEATH_BACKPACK_OWNER_PICKUP_ONLY);
    }

    public static boolean preventPortableContainerNesting(ServerLevel level) {
        return level.getGameRules().get(PREVENT_PORTABLE_CONTAINER_NESTING);
    }

    /** Used by item-level vanilla checks that do not receive a world argument. */
    public static boolean preventPortableContainerNesting() {
        MinecraftServer server = activeServer;
        return server != null
                ? server.getGameRules().get(PREVENT_PORTABLE_CONTAINER_NESTING)
                : clientPreventsPortableContainerNesting;
    }

    public static boolean clientGeneratesDeathBackpacks() {
        return clientGeneratesDeathBackpacks;
    }

    public static boolean clientDeathBackpackOwnerPickupOnly() {
        return clientDeathBackpackOwnerPickupOnly;
    }

    public static boolean clientPreventsPortableContainerNesting() {
        return clientPreventsPortableContainerNesting;
    }

    public static boolean clientRulesSynchronized() {
        return clientRulesSynchronized;
    }

    public static void updateClientRules(
            boolean generateDeathBackpacks,
            boolean ownerPickupOnly,
            boolean preventNesting) {
        clientGeneratesDeathBackpacks = generateDeathBackpacks;
        clientDeathBackpackOwnerPickupOnly = ownerPickupOnly;
        clientPreventsPortableContainerNesting = preventNesting;
        clientRulesSynchronized = true;
    }

    public static void resetClientRules() {
        clientGeneratesDeathBackpacks = true;
        clientDeathBackpackOwnerPickupOnly = true;
        clientPreventsPortableContainerNesting = true;
        clientRulesSynchronized = false;
    }

    private static void registerRuleSync(GameRule<Boolean> rule) {
        GameRuleEvents.changeCallback(rule)
                .register((value, server) -> server.getPlayerList().getPlayers()
                        .forEach(RemnantGameRules::sendRules));
    }

    private static void sendRules(net.minecraft.server.level.ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, RemnantRulesPayload.TYPE)) {
            ServerPlayNetworking.send(player, new RemnantRulesPayload(
                    generateDeathBackpacks(player.level()),
                    deathBackpackOwnerPickupOnly(player.level()),
                    preventPortableContainerNesting(player.level())
            ));
        }
    }
}
