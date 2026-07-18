package com.adaptor.deadrecall.discord;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.ArrayList;
import java.util.List;

public final class DiscordVillagerLocalizationGameTest {
    @GameTest(maxTicks = 40)
    public void dedicatedServerFormatsVillagerLevelUpExactlyOnce(GameTestHelper helper) {
        List<DiscordEventPayload> captured = new ArrayList<>();
        try (AutoCloseable ignored = DiscordEventDispatcher.observeForTesting(captured::add)) {
            DiscordEventNotifications.villagerLevelUp("", "librarian", 1, 2);
            List<DiscordEventPayload> expected = List.of(new DiscordEventPayload(
                    "villager_level_up",
                    "系統",
                    "村民（圖書管理員）升級：新手 → 學徒"
            ));
            if (!captured.equals(expected)) {
                throw helper.assertionException("Unexpected localized villager payloads: " + captured);
            }
            helper.succeed();
        } catch (Exception exception) {
            throw helper.assertionException("Failed to capture villager payload: " + exception.getMessage());
        }
    }
}
