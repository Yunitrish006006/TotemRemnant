package com.adaptor.deadrecall.discord;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DiscordLocalizationReloadGameTest {
    private static final String RELOAD_TEST_KEY = "discord.deadrecall.test.reload";
    private static final String DATA_PACK_VALUE = "資料包重新載入完成";

    @GameTest(maxTicks = 400)
    public void dedicatedServerReloadReplacesTranslationSnapshot(GameTestHelper helper) {
        if (!DATA_PACK_VALUE.equals(DiscordLocalizationService.translate(RELOAD_TEST_KEY))) {
            throw helper.assertionException("Discord zh_tw test data resource was not loaded at server startup");
        }

        Map<String, String> stale = new LinkedHashMap<>(DiscordLocalizationService.snapshotForTesting());
        stale.put(RELOAD_TEST_KEY, "過期 snapshot");
        DiscordLocalizationService.replaceSnapshotForTesting(stale);
        if (!"過期 snapshot".equals(DiscordLocalizationService.translate(RELOAD_TEST_KEY))) {
            throw helper.assertionException("Could not install the stale localization test snapshot");
        }

        MinecraftServer server = helper.getLevel().getServer();
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .whenComplete((ignored, failure) -> server.execute(() -> {
                    if (failure != null) {
                        helper.fail("Dedicated Server resource reload failed: " + failure.getMessage());
                        return;
                    }
                    if (!DATA_PACK_VALUE.equals(DiscordLocalizationService.translate(RELOAD_TEST_KEY))) {
                        helper.fail("Runtime reload did not replace the stale Discord zh_tw snapshot");
                        return;
                    }
                    if (!"殭屍".equals(DiscordLocalizationService.translate("entity.minecraft.zombie"))) {
                        helper.fail("Runtime reload lost the bundled zh_tw fallback translations");
                        return;
                    }
                    helper.succeed();
                }));
    }
}
