package com.adaptor.deadrecall.discord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordTransientNotificationPolicyTest {
    @Test
    void allowlistsOperationalMessagesOnly() {
        assertTrue(DiscordTransientNotificationPolicy.isTemporaryEvent("player_join"));
        assertTrue(DiscordTransientNotificationPolicy.isTemporaryEvent("player_first_join"));
        assertTrue(DiscordTransientNotificationPolicy.isTemporaryEvent("death_backpack_created"));
        assertTrue(DiscordTransientNotificationPolicy.isTemporaryEvent("death_backpack_recovered"));

        assertFalse(DiscordTransientNotificationPolicy.isTemporaryEvent("player_leave"));
        assertFalse(DiscordTransientNotificationPolicy.isTemporaryEvent("chat"));
        assertFalse(DiscordTransientNotificationPolicy.isTemporaryEvent("player_death"));
        assertFalse(DiscordTransientNotificationPolicy.isTemporaryEvent(null));
    }

    @Test
    void usesFixedTenMinuteLifetime() {
        assertEquals(600, DiscordTransientNotificationPolicy.DELETE_AFTER_SECONDS);
        assertEquals(600, DiscordTransientNotificationPolicy.deleteAfterSeconds("player_join"));
        assertEquals(0, DiscordTransientNotificationPolicy.deleteAfterSeconds("chat"));
    }
}
