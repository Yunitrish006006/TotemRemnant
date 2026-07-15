package com.adaptor.deadrecall.space;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendTeleportSessionPolicyTest {
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void matchesTeleportFromFirstFriendToSecondFriend() {
        assertTrue(FriendTeleportSessionPolicy.belongsToRelationship(ALICE, BOB, ALICE, BOB));
    }

    @Test
    void matchesTeleportFromSecondFriendToFirstFriend() {
        assertTrue(FriendTeleportSessionPolicy.belongsToRelationship(BOB, ALICE, ALICE, BOB));
    }

    @Test
    void ignoresUnrelatedTeleportSessions() {
        assertFalse(FriendTeleportSessionPolicy.belongsToRelationship(ALICE, CAROL, ALICE, BOB));
        assertFalse(FriendTeleportSessionPolicy.belongsToRelationship(CAROL, BOB, ALICE, BOB));
    }

    @Test
    void rejectsIncompleteRelationshipData() {
        assertFalse(FriendTeleportSessionPolicy.belongsToRelationship(null, BOB, ALICE, BOB));
        assertFalse(FriendTeleportSessionPolicy.belongsToRelationship(ALICE, null, ALICE, BOB));
        assertFalse(FriendTeleportSessionPolicy.belongsToRelationship(ALICE, BOB, null, BOB));
        assertFalse(FriendTeleportSessionPolicy.belongsToRelationship(ALICE, BOB, ALICE, null));
    }
}
