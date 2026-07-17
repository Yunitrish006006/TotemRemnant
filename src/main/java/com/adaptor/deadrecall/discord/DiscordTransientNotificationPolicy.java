package com.adaptor.deadrecall.discord;

import java.util.Set;

/** Defines which Discord notifications are temporary and their fixed lifetime. */
public final class DiscordTransientNotificationPolicy {
    public static final int DELETE_AFTER_SECONDS = 10 * 60;

    private static final Set<String> TEMPORARY_EVENTS = Set.of(
            "player_join",
            "player_first_join",
            "death_backpack_created",
            "death_backpack_recovered"
    );

    private DiscordTransientNotificationPolicy() {
    }

    public static boolean isTemporaryEvent(String event) {
        return event != null && TEMPORARY_EVENTS.contains(event);
    }

    public static int deleteAfterSeconds(String event) {
        return isTemporaryEvent(event) ? DELETE_AFTER_SECONDS : 0;
    }
}
