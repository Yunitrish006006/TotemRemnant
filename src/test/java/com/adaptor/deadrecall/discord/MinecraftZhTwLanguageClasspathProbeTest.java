package com.adaptor.deadrecall.discord;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftZhTwLanguageClasspathProbeTest {
    private static final String SYSTEM_TABLE = "/assets/deadrecall/lang/discord_zh_tw/system.json";

    @Test
    void deadRecallProvidesDedicatedServerTranslationSnapshot() throws Exception {
        try (InputStream stream = DiscordLocalizationService.class.getResourceAsStream(SYSTEM_TABLE)) {
            assertNotNull(stream, "Missing bundled Discord zh_tw translation table");
        }
        assertTrue(DiscordLocalizationService.translationCount() >= 140,
                "Discord zh_tw snapshot does not cover the expected Minecraft 26.2 event subset");
    }
}
