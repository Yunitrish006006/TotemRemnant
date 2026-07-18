package com.adaptor.deadrecall.discord;

public record DiscordEventPayload(String event, String username, String message) {
    public DiscordEventPayload {
        event = normalize(event);
        username = normalize(username);
        message = normalize(message);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
