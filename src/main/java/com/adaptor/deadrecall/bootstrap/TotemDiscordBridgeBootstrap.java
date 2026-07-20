package com.adaptor.deadrecall.bootstrap;

import com.adaptor.deadrecall.DiscordBridge;
import com.adaptor.deadrecall.discord.DiscordLocalizationService;

import java.nio.file.Path;

/**
 * Owns registration for the future TotemDiscordBridge module.
 */
public final class TotemDiscordBridgeBootstrap {
    private TotemDiscordBridgeBootstrap() {
    }

    public static void register(Path configDir) {
        DiscordLocalizationService.registerReloadListener();
        DiscordBridge.init(configDir);
    }
}
