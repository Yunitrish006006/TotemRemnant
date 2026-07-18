package com.adaptor.deadrecall.discord;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class DiscordAdvancementLocalizationGameTest {
    @GameTest(maxTicks = 40)
    public void dedicatedServerFormatsAdvancementExactlyOnce(GameTestHelper helper) {
        List<DiscordEventPayload> captured = new ArrayList<>();
        try (AutoCloseable ignored = DiscordEventDispatcher.observeForTesting(captured::add)) {
            DiscordEventNotifications.advancement(
                    "ServerPlayer",
                    Component.translatable("advancements.story.mine_stone.title"),
                    "task"
            );
            List<DiscordEventPayload> expected = List.of(new DiscordEventPayload(
                    "advancement",
                    "ServerPlayer",
                    "ServerPlayer 完成了進度「石器時代」"
            ));
            if (!captured.equals(expected)) {
                throw helper.assertionException("Unexpected localized advancement payloads: " + captured);
            }
            helper.succeed();
        } catch (Exception exception) {
            throw helper.assertionException("Failed to capture advancement payload: " + exception.getMessage());
        }
    }
}
