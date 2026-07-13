package com.adaptor.deadrecall.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.stream.Collectors;

final class DiscordMixinFormatting {
    private DiscordMixinFormatting() {
    }

    static String actor(CommandSourceStack source) {
        if (source == null) {
            return "server";
        }
        String name = source.getTextName();
        return name == null || name.isBlank() ? "server" : name.trim();
    }

    static String names(Collection<NameAndId> profiles) {
        return profiles.stream()
                .map(NameAndId::name)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(", "));
    }

    static String playerNames(Collection<ServerPlayer> players) {
        return players.stream()
                .map(player -> player.getName().getString())
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(", "));
    }
}
