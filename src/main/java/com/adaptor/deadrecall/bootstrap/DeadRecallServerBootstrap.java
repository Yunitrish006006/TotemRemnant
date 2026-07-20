package com.adaptor.deadrecall.bootstrap;

import com.adaptor.deadrecall.registry.DeadRecallRegistryBootstrap;

import java.nio.file.Path;

/**
 * Composes the server-side feature bootstraps in the legacy registration order.
 */
public final class DeadRecallServerBootstrap {
    private DeadRecallServerBootstrap() {
    }

    public static void register(Path configDir) {
        DeadRecallRegistryBootstrap.registerContent();
        LegacyGameplayBootstrap.registerInteractions();
        TotemAutomataBootstrap.register();
        TotemNexusBootstrap.register();
        LegacyGameplayBootstrap.registerRecipes();
        TotemDiscordBridgeBootstrap.register(configDir);
    }
}
