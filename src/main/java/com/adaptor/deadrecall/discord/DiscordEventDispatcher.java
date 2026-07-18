package com.adaptor.deadrecall.discord;

import com.adaptor.deadrecall.DiscordBridge;

import java.util.Objects;
import java.util.function.Consumer;

public final class DiscordEventDispatcher {
    private static volatile Consumer<DiscordEventPayload> observerForTesting;

    private DiscordEventDispatcher() {
    }

    public static void send(String event, String username, String message) {
        DiscordEventPayload payload = new DiscordEventPayload(event, username, message);
        if (payload.event().isEmpty() || payload.username().isEmpty() || payload.message().isEmpty()) {
            return;
        }

        Consumer<DiscordEventPayload> observer = observerForTesting;
        if (observer != null) {
            observer.accept(payload);
        }
        DiscordBridge.sendMinecraftEvent(payload.event(), payload.username(), payload.message());
    }

    public static AutoCloseable observeForTesting(Consumer<DiscordEventPayload> observer) {
        Objects.requireNonNull(observer, "observer");
        if (observerForTesting != null) {
            throw new IllegalStateException("Discord event observer already installed");
        }
        observerForTesting = observer;
        return () -> observerForTesting = null;
    }
}
