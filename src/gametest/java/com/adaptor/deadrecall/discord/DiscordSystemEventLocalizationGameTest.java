package com.adaptor.deadrecall.discord;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class DiscordSystemEventLocalizationGameTest {
    @GameTest(maxTicks = 40)
    public void dedicatedServerFormatsSystemEventsExactlyOnce(GameTestHelper helper) {
        List<DiscordEventPayload> captured = new ArrayList<>();
        try (AutoCloseable ignored = DiscordEventDispatcher.observeForTesting(captured::add)) {
            DiscordEventNotifications.death(Component.translatable(
                    "death.attack.mob.item",
                    Component.literal("ServerPlayer"),
                    Component.translatable("entity.minecraft.zombie"),
                    Component.literal("Custom Blade")
            ));
            DiscordEventNotifications.bossDefeated(
                    Component.translatable("entity.minecraft.ender_dragon"),
                    "ServerPlayer"
            );
            DiscordEventNotifications.raidEnded("victory");
            DiscordEventNotifications.difficultyChanged("ServerAdmin", "hard");

            List<DiscordEventPayload> expected = List.of(
                    new DiscordEventPayload(
                            "player_death",
                            "死亡訊息",
                            "ServerPlayer 被 殭屍 用 Custom Blade 殺死"
                    ),
                    new DiscordEventPayload(
                            "boss_defeated",
                            "ServerPlayer",
                            "ServerPlayer 擊敗了 終界龍"
                    ),
                    new DiscordEventPayload("raid_ended", "系統", "襲擊已結束：勝利"),
                    new DiscordEventPayload(
                            "difficulty_changed",
                            "ServerAdmin",
                            "ServerAdmin 將難度改為 困難"
                    )
            );
            if (!captured.equals(expected)) {
                throw helper.assertionException("Unexpected localized system payloads: " + captured);
            }
            helper.succeed();
        } catch (Exception exception) {
            throw helper.assertionException("Failed to capture system payloads: " + exception.getMessage());
        }
    }
}
