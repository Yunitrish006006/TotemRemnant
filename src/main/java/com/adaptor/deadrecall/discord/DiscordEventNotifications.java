package com.adaptor.deadrecall.discord;

import net.minecraft.network.chat.Component;

public final class DiscordEventNotifications {
    private DiscordEventNotifications() {
    }

    public static void advancement(String playerName, Component title, String frameType) {
        String message = DiscordEventFormatter.advancementMessage(playerName, title, frameType);
        DiscordEventDispatcher.send("advancement", playerName, message);
    }

    public static void villagerLevelUp(
            String customName,
            String professionPath,
            int previousLevel,
            int currentLevel
    ) {
        String message = DiscordEventFormatter.villagerLevelUpMessage(
                customName,
                professionPath,
                previousLevel,
                currentLevel
        );
        DiscordEventDispatcher.send("villager_level_up", "系統", message);
    }

    public static void death(Component deathMessage) {
        DiscordEventDispatcher.send(
                "player_death",
                "死亡訊息",
                DiscordEventFormatter.deathMessage(deathMessage)
        );
    }

    public static void bossDefeated(Component bossName, String killerName) {
        String normalizedKiller = normalize(killerName);
        DiscordEventDispatcher.send(
                "boss_defeated",
                normalizedKiller.isEmpty() ? "系統" : normalizedKiller,
                DiscordEventFormatter.bossDefeatedMessage(bossName, normalizedKiller)
        );
    }

    public static void raidEnded(String result) {
        DiscordEventDispatcher.send("raid_ended", "系統", DiscordEventFormatter.raidEndedMessage(result));
    }

    public static void difficultyChanged(String actor, String difficultyPath) {
        String normalizedActor = normalize(actor);
        String source = normalizedActor.isEmpty() ? "server" : normalizedActor;
        DiscordEventDispatcher.send(
                "difficulty_changed",
                source,
                DiscordEventFormatter.difficultyChangedMessage(source, difficultyPath)
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
