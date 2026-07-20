package com.adaptor.deadrecall.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * Composes the client-side feature bootstraps in the legacy registration order.
 */
public final class DeadRecallClientBootstrap {
    private DeadRecallClientBootstrap() {
    }

    public static void register() {
        TotemAutomataClientBootstrap.registerScreens();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("deadrecall", "category")
        );
        DeadrecallClient.openDiscordConfigKey = TotemDiscordBridgeClientBootstrap.createKeyMapping(category);
        DeadrecallClient.sortBackpackKey = LegacyContainerClientBootstrap.createKeyMapping(category);

        TotemDiscordBridgeClientBootstrap.registerRuntime();
        TotemAutomataClientBootstrap.registerNetworking();
        TotemNexusClientBootstrap.registerNetworking();
        TotemDiscordBridgeClientBootstrap.registerCommands();
    }
}
