package com.adaptor.deadrecall.discord;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DiscordEventFormatterTest {
    @Test
    void rendersVanillaAdvancementTitleInTraditionalChinese() {
        assertEquals(
                "Alex 完成了進度「石器時代」",
                DiscordEventFormatter.advancementMessage(
                        "Alex",
                        Component.translatable("advancements.story.mine_stone.title"),
                        "task"
                )
        );
    }

    @Test
    void mapsEveryAdvancementFrameTypeToChinese() {
        Component title = Component.translatable("advancements.story.mine_diamond.title");
        assertEquals("Alex 完成了進度「鑽石！」", DiscordEventFormatter.advancementMessage("Alex", title, "task"));
        assertEquals("Alex 完成了目標「鑽石！」", DiscordEventFormatter.advancementMessage("Alex", title, "goal"));
        assertEquals("Alex 完成了挑戰「鑽石！」", DiscordEventFormatter.advancementMessage("Alex", title, "challenge"));
    }

    @Test
    void rendersNestedComponentArgumentsAndPreservesLiteralNames() {
        Component nested = Component.translatable(
                "discord.deadrecall.test.nested",
                Component.literal("PlayerOne"),
                Component.literal("Excalibur-E")
        );
        assertEquals("PlayerOne 使用 Excalibur-E", DiscordLocalizationService.render(nested));
    }

    @Test
    void unknownTranslationKeyDoesNotLeakRawKey() {
        String rendered = DiscordLocalizationService.render(
                Component.translatable("advancements.example.missing.title")
        );
        assertEquals("未知進度", rendered);
        assertFalse(rendered.contains("advancements.example"));
    }

    @Test
    void formatsUnnamedLibrarianLevelUpWithChineseCareerNames() {
        assertEquals(
                "村民（圖書管理員）升級：新手 → 學徒",
                DiscordEventFormatter.villagerLevelUpMessage("", "librarian", 1, 2)
        );
    }

    @Test
    void preservesCustomVillagerNameWhileLocalizingProfessionAndLevels() {
        assertEquals(
                "Archivist E（圖書管理員）升級：學徒 → 老手",
                DiscordEventFormatter.villagerLevelUpMessage("Archivist E", "librarian", 2, 3)
        );
    }

    @Test
    void localizesNestedDeathTemplateAndPreservesLiteralNames() {
        Component deathMessage = Component.translatable(
                "death.attack.mob.item",
                Component.literal("Alex"),
                Component.translatable("entity.minecraft.zombie"),
                Component.literal("Excalibur-E")
        );
        assertEquals(
                "Alex 被 殭屍 用 Excalibur-E 殺死",
                DiscordEventFormatter.deathMessage(deathMessage)
        );
    }

    @Test
    void localizesDefaultBossNamesAndPreservesCustomBossNames() {
        assertEquals(
                "Alex 擊敗了 終界龍",
                DiscordEventFormatter.bossDefeatedMessage(
                        Component.translatable("entity.minecraft.ender_dragon"),
                        "Alex"
                )
        );
        assertEquals(
                "The Archivist 被擊敗了",
                DiscordEventFormatter.bossDefeatedMessage(Component.literal("The Archivist"), "")
        );
    }

    @Test
    void localizesRaidResultsAndDifficultyLabels() {
        assertEquals("襲擊已結束：勝利", DiscordEventFormatter.raidEndedMessage("victory"));
        assertEquals("襲擊已結束：失敗", DiscordEventFormatter.raidEndedMessage("defeat"));
        assertEquals("襲擊已結束：停止", DiscordEventFormatter.raidEndedMessage("stopped"));
        assertEquals("Admin 將難度改為 困難", DiscordEventFormatter.difficultyChangedMessage("Admin", "hard"));
    }

    @Test
    void advancementNotificationCreatesExactlyOneLocalizedPayload() throws Exception {
        List<DiscordEventPayload> captured = new ArrayList<>();
        try (AutoCloseable ignored = DiscordEventDispatcher.observeForTesting(captured::add)) {
            DiscordEventNotifications.advancement(
                    "Alex",
                    Component.translatable("advancements.story.mine_stone.title"),
                    "task"
            );
        }
        assertEquals(List.of(new DiscordEventPayload(
                "advancement",
                "Alex",
                "Alex 完成了進度「石器時代」"
        )), captured);
    }

    @Test
    void villagerNotificationCreatesExactlyOneLocalizedPayload() throws Exception {
        List<DiscordEventPayload> captured = new ArrayList<>();
        try (AutoCloseable ignored = DiscordEventDispatcher.observeForTesting(captured::add)) {
            DiscordEventNotifications.villagerLevelUp("", "librarian", 1, 2);
        }
        assertEquals(List.of(new DiscordEventPayload(
                "villager_level_up",
                "系統",
                "村民（圖書管理員）升級：新手 → 學徒"
        )), captured);
    }

    @Test
    void phaseTwoNotificationsEachCreateOneLocalizedPayload() throws Exception {
        List<DiscordEventPayload> captured = new ArrayList<>();
        try (AutoCloseable ignored = DiscordEventDispatcher.observeForTesting(captured::add)) {
            DiscordEventNotifications.death(Component.translatable(
                    "death.attack.generic",
                    Component.literal("Alex")
            ));
            DiscordEventNotifications.bossDefeated(
                    Component.translatable("entity.minecraft.wither"),
                    "Alex"
            );
            DiscordEventNotifications.raidEnded("victory");
            DiscordEventNotifications.difficultyChanged("Admin", "normal");
        }
        assertEquals(List.of(
                new DiscordEventPayload("player_death", "死亡訊息", "Alex 死亡"),
                new DiscordEventPayload("boss_defeated", "Alex", "Alex 擊敗了 凋零怪"),
                new DiscordEventPayload("raid_ended", "系統", "襲擊已結束：勝利"),
                new DiscordEventPayload("difficulty_changed", "Admin", "Admin 將難度改為 普通")
        ), captured);
    }

    @Test
    void componentRenderingUsesOneImmutableSnapshotDuringReload() throws Exception {
        Map<String, String> previous = DiscordLocalizationService.snapshotForTesting();
        Map<String, String> first = Map.of(
                "discord.deadrecall.test.atomic.root", "A-%s",
                "discord.deadrecall.test.atomic.value", "1"
        );
        Map<String, String> second = Map.of(
                "discord.deadrecall.test.atomic.root", "B-%s",
                "discord.deadrecall.test.atomic.value", "2"
        );
        Component nested = Component.translatable(
                "discord.deadrecall.test.atomic.root",
                Component.translatable("discord.deadrecall.test.atomic.value")
        );
        Set<String> completeResults = Set.of("A-1", "B-2");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            DiscordLocalizationService.replaceSnapshotForTesting(first);
            Future<?> writer = executor.submit(() -> {
                await(start);
                for (int index = 0; index < 25_000; index++) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    DiscordLocalizationService.replaceSnapshotForTesting((index & 1) == 0 ? second : first);
                }
            });
            Future<?> reader = executor.submit(() -> {
                await(start);
                for (int index = 0; index < 25_000; index++) {
                    String rendered = DiscordLocalizationService.render(nested);
                    if (!completeResults.contains(rendered)) {
                        throw new AssertionError("Observed a mixed translation snapshot: " + rendered);
                    }
                }
            });

            start.countDown();
            writer.get(10, TimeUnit.SECONDS);
            reader.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
            DiscordLocalizationService.replaceSnapshotForTesting(previous);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for atomic snapshot test", exception);
        }
    }
}
