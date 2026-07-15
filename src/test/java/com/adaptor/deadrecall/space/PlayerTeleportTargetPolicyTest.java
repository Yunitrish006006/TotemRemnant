package com.adaptor.deadrecall.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerTeleportTargetPolicyTest {
    @Test
    void classifiesOfflineBeforeOtherState() {
        assertEquals(
                PlayerTeleportTargetPolicy.State.OFFLINE,
                PlayerTeleportTargetPolicy.classify(false, false, true, false)
        );
    }

    @Test
    void classifiesDeadOrRemovedPlayersAsUnavailable() {
        assertEquals(
                PlayerTeleportTargetPolicy.State.UNAVAILABLE,
                PlayerTeleportTargetPolicy.classify(true, false, false, true)
        );
        assertEquals(
                PlayerTeleportTargetPolicy.State.UNAVAILABLE,
                PlayerTeleportTargetPolicy.classify(true, true, true, true)
        );
    }

    @Test
    void classifiesLostFriendshipAfterAvailabilityChecks() {
        assertEquals(
                PlayerTeleportTargetPolicy.State.NOT_FRIENDS,
                PlayerTeleportTargetPolicy.classify(true, true, false, false)
        );
    }

    @Test
    void classifiesValidOnlineFriendAsAvailable() {
        assertEquals(
                PlayerTeleportTargetPolicy.State.AVAILABLE,
                PlayerTeleportTargetPolicy.classify(true, true, false, true)
        );
    }

    @Test
    void exposesStableMessageKeys() {
        assertEquals(
                "message.deadrecall.space_unit.teleport_cancelled.target_offline",
                PlayerTeleportTargetPolicy.cancellationMessageKey(PlayerTeleportTargetPolicy.State.OFFLINE)
        );
        assertEquals(
                "message.deadrecall.space_unit.teleport_cancelled.target_unavailable",
                PlayerTeleportTargetPolicy.cancellationMessageKey(PlayerTeleportTargetPolicy.State.UNAVAILABLE)
        );
        assertEquals(
                "message.deadrecall.space_unit.teleport_cancelled.target_friendship",
                PlayerTeleportTargetPolicy.cancellationMessageKey(PlayerTeleportTargetPolicy.State.NOT_FRIENDS)
        );
    }
}
