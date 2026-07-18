package com.adaptor.deadrecall.discord;

import net.minecraft.network.chat.Component;

public final class DiscordEventFormatter {
    private DiscordEventFormatter() {
    }

    public static String advancementMessage(String playerName, Component title, String frameType) {
        String name = normalize(playerName);
        String localizedTitle = DiscordLocalizationService.render(title);
        if (localizedTitle.isEmpty()) {
            localizedTitle = DiscordLocalizationService.translate("discord.deadrecall.advancement.unknown");
        }
        String type = DiscordLocalizationService.translate(switch (normalize(frameType)) {
            case "goal" -> "discord.deadrecall.advancement.goal";
            case "challenge" -> "discord.deadrecall.advancement.challenge";
            default -> "discord.deadrecall.advancement.task";
        });
        return name + " 完成了" + type + "「" + localizedTitle + "」";
    }

    public static String villagerLevelUpMessage(
            String customName,
            String professionPath,
            int previousLevel,
            int currentLevel
    ) {
        String normalizedCustomName = normalize(customName);
        String villagerName = normalizedCustomName.isEmpty()
                ? DiscordLocalizationService.translate("entity.minecraft.villager")
                : normalizedCustomName;
        String profession = DiscordLocalizationService.translate(
                "entity.minecraft.villager." + normalizeProfessionPath(professionPath)
        );
        String previous = DiscordLocalizationService.translate("merchant.level." + clampLevel(previousLevel));
        String current = DiscordLocalizationService.translate("merchant.level." + clampLevel(currentLevel));

        if ("未知訊息".equals(profession) || "未知實體".equals(profession)) {
            return villagerName + " 升級：" + previous + " → " + current;
        }
        return villagerName + "（" + profession + "）升級：" + previous + " → " + current;
    }

    public static String deathMessage(Component deathMessage) {
        String localized = DiscordLocalizationService.render(deathMessage);
        return localized.isEmpty()
                ? DiscordLocalizationService.translate("discord.deadrecall.death.unknown")
                : localized;
    }

    public static String bossDefeatedMessage(Component bossName, String killerName) {
        String boss = DiscordLocalizationService.render(bossName);
        if (boss.isEmpty()) {
            boss = DiscordLocalizationService.translate("discord.deadrecall.entity.unknown");
        }

        String killer = normalize(killerName);
        return killer.isEmpty() ? boss + " 被擊敗了" : killer + " 擊敗了 " + boss;
    }

    public static String raidEndedMessage(String result) {
        String resultKey = switch (normalize(result)) {
            case "victory" -> "event.minecraft.raid.victory";
            case "defeat", "loss" -> "event.minecraft.raid.defeat";
            case "stopped" -> "discord.deadrecall.raid.stopped";
            default -> "discord.deadrecall.raid.ended";
        };
        return "襲擊已結束：" + DiscordLocalizationService.translate(resultKey);
    }

    public static String difficultyChangedMessage(String actor, String difficultyPath) {
        String source = normalize(actor);
        if (source.isEmpty()) {
            source = "server";
        }
        String difficulty = DiscordLocalizationService.translate(
                "options.difficulty." + normalize(difficultyPath).toLowerCase(java.util.Locale.ROOT)
        );
        return source + " 將難度改為 " + difficulty;
    }

    private static int clampLevel(int level) {
        return Math.max(1, Math.min(5, level));
    }

    private static String normalizeProfessionPath(String professionPath) {
        String normalized = normalize(professionPath);
        return normalized.isEmpty() ? "none" : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
