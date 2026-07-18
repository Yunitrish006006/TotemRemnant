package com.adaptor.deadrecall.space;

import com.adaptor.deadrecall.mixin.SpaceUnitTeleportSessionAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DirectFriendPlayerTeleportGameTest {
    private static final BlockPos REQUESTER_POS = new BlockPos(2, 20, 2);
    private static final BlockPos INITIAL_TARGET_POS = new BlockPos(14, 20, 2);
    private static final BlockPos LATEST_TARGET_POS = new BlockPos(90, 20, 2);
    private static final BlockPos THIRD_PLAYER_POS = new BlockPos(2, 20, 14);
    private static final BlockPos NETHER_TARGET_POS = new BlockPos(32, 200, 32);

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 80)
    public void bilateralFriendsStartDirectPlayerSession(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, REQUESTER_POS);
        ServerPlayer target = createPlayer(helper, INITIAL_TARGET_POS);
        try {
            preparePlayer(requester, true);
            preparePlayer(target, true);
            makeFriends(helper, requester, target);
            requireOnline(helper, requester, target);

            startPlayerTeleport(requester, target);

            Object session = sessions().get(requester.getUUID());
            require(helper, session instanceof SpaceUnitTeleportSessionAccessor,
                    "Bilateral friends did not create a direct PLAYER teleport session");
            require(helper, target.getUUID().equals(
                            ((SpaceUnitTeleportSessionAccessor) session).deadrecall$getTargetUnitId()),
                    "Direct friend session targeted the wrong player UUID");
            helper.succeed();
        } finally {
            cleanup(helper, requester, target);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 80)
    public void nonFriendPendingInviteAndSelfTargetsAreRejected(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, REQUESTER_POS);
        ServerPlayer target = createPlayer(helper, INITIAL_TARGET_POS);
        try {
            preparePlayer(requester, true);
            preparePlayer(target, true);
            requireOnline(helper, requester, target);

            startPlayerTeleport(requester, target);
            require(helper, !sessions().containsKey(requester.getUUID()),
                    "A non-friend PLAYER target created a teleport session");

            friendData(helper).inviteOrAccept(requester.getUUID(), target.getUUID());
            startPlayerTeleport(requester, target);
            require(helper, !sessions().containsKey(requester.getUUID()),
                    "A one-way pending friend invite created a teleport session");

            startPlayerTeleport(requester, requester);
            require(helper, !sessions().containsKey(requester.getUUID()),
                    "A player created a PLAYER teleport session targeting themselves");
            helper.succeed();
        } finally {
            cleanup(helper, requester, target);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 400)
    public void completionUsesLatestTargetPositionSafeLandingAndChargesOnce(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, REQUESTER_POS);
        ServerPlayer target = createPlayer(helper, INITIAL_TARGET_POS);
        preparePlayer(requester, false);
        preparePlayer(target, true);
        requester.getFoodData().setFoodLevel(20);
        requester.getFoodData().setSaturation(0.0F);
        makeFriends(helper, requester, target);
        requireOnline(helper, requester, target);

        startPlayerTeleport(requester, target);
        require(helper, sessions().containsKey(requester.getUUID()),
                "Direct friend session did not start before latest-position regression");

        helper.runAtTickTime(5, () -> {
            removeFloor(helper.getLevel(), helper.absolutePos(INITIAL_TARGET_POS));
            placeSafeLanding(helper.getLevel(), helper.absolutePos(LATEST_TARGET_POS));
            snapTo(target, helper.absolutePos(LATEST_TARGET_POS));
        });

        helper.runAtTickTime(340, () -> {
            BlockPos expected = helper.absolutePos(LATEST_TARGET_POS);
            require(helper, requester.level() == helper.getLevel(),
                    "Same-dimension friend teleport changed to an unexpected level");
            require(helper, requester.blockPosition().equals(expected),
                    "Teleport completed at stale target coordinates instead of the latest safe landing: "
                            + requester.blockPosition() + " != " + expected);
            require(helper, requester.getFoodData().getFoodLevel() == 15,
                    "Successful same-dimension friend teleport did not deduct the expected five hunger points exactly once");
            require(helper, !sessions().containsKey(requester.getUUID()),
                    "Completed PLAYER teleport session remained tracked");
        });

        helper.runAtTickTime(365, () -> {
            try {
                require(helper, requester.getFoodData().getFoodLevel() == 15,
                        "Completed PLAYER teleport charged its cost more than once on later ticks");
                helper.succeed();
            } finally {
                cleanup(helper, requester, target);
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void removingFriendImmediatelyCancelsBothDirectionsAndPreservesUnrelatedSession(GameTestHelper helper) {
        ServerPlayer first = createPlayer(helper, REQUESTER_POS);
        ServerPlayer second = createPlayer(helper, INITIAL_TARGET_POS);
        ServerPlayer third = createPlayer(helper, THIRD_PLAYER_POS);
        try {
            preparePlayer(first, true);
            preparePlayer(second, true);
            preparePlayer(third, true);
            makeFriends(helper, first, second);
            makeFriends(helper, first, third);
            requireOnline(helper, first, second, third);

            startPlayerTeleport(first, second);
            startPlayerTeleport(second, first);
            startPlayerTeleport(third, first);
            require(helper, sessions().keySet().containsAll(Set.of(
                            first.getUUID(), second.getUUID(), third.getUUID())),
                    "Expected three active friend sessions before relationship removal");

            SpaceUnitHandler.removeFriend(first, second.getUUID());

            require(helper, !sessions().containsKey(first.getUUID()),
                    "Removing friendship did not immediately cancel first → second session");
            require(helper, !sessions().containsKey(second.getUUID()),
                    "Removing friendship did not immediately cancel second → first session");
            Object unrelated = sessions().get(third.getUUID());
            require(helper, unrelated instanceof SpaceUnitTeleportSessionAccessor
                            && first.getUUID().equals(
                            ((SpaceUnitTeleportSessionAccessor) unrelated).deadrecall$getTargetUnitId()),
                    "Removing one relationship incorrectly removed an unrelated friend teleport session");
            require(helper, !friendData(helper).areFriends(first.getUUID(), second.getUUID()),
                    "Friend relationship remained after removeFriend");
            require(helper, friendData(helper).areFriends(first.getUUID(), third.getUUID()),
                    "Removing one relationship modified another friendship");
            helper.succeed();
        } finally {
            cleanup(helper, first, second, third);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void targetDeathCancelsSessionBeforeAnyCostIsCharged(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, REQUESTER_POS);
        ServerPlayer target = createPlayer(helper, INITIAL_TARGET_POS);
        try {
            preparePlayer(requester, false);
            preparePlayer(target, true);
            requester.getFoodData().setFoodLevel(20);
            requester.getFoodData().setSaturation(0.0F);
            makeFriends(helper, requester, target);
            requireOnline(helper, requester, target);
            startPlayerTeleport(requester, target);

            target.setHealth(0.0F);
            target.die(helper.getLevel().damageSources().generic());
            SpaceUnitHandler.tickTeleportSessions(helper.getLevel().getServer());

            require(helper, !sessions().containsKey(requester.getUUID()),
                    "A session targeting a dead player remained active");
            require(helper, requester.getFoodData().getFoodLevel() == 20,
                    "Cancelled target-death session charged teleport cost");
            helper.succeed();
        } finally {
            cleanup(helper, requester, target);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void offlineTargetCancelsSessionBeforeAnyCostIsCharged(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, REQUESTER_POS);
        ServerPlayer target = createPlayer(helper, INITIAL_TARGET_POS);
        try {
            preparePlayer(requester, false);
            preparePlayer(target, true);
            requester.getFoodData().setFoodLevel(20);
            requester.getFoodData().setSaturation(0.0F);
            makeFriends(helper, requester, target);
            requireOnline(helper, requester, target);
            startPlayerTeleport(requester, target);

            helper.getLevel().getServer().getPlayerList().remove(target);
            SpaceUnitHandler.tickTeleportSessions(helper.getLevel().getServer());

            require(helper, !sessions().containsKey(requester.getUUID()),
                    "A session targeting an offline player remained active");
            require(helper, requester.getFoodData().getFoodLevel() == 20,
                    "Cancelled offline-target session charged teleport cost");
            helper.succeed();
        } finally {
            cleanup(helper, requester, target);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 420)
    public void targetDimensionChangeUsesLatestDimensionAndSafeLanding(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, REQUESTER_POS);
        ServerPlayer target = createPlayer(helper, INITIAL_TARGET_POS);
        preparePlayer(requester, true);
        preparePlayer(target, true);
        requester.getInventory().add(new ItemStack(Items.AMETHYST_SHARD, 16));
        makeFriends(helper, requester, target);
        requireOnline(helper, requester, target);

        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        require(helper, nether != null, "Nether level was unavailable for target-dimension regression");
        BlockPos netherTarget = helper.absolutePos(NETHER_TARGET_POS);
        placeSafeLanding(nether, netherTarget);

        startPlayerTeleport(requester, target);
        helper.runAtTickTime(5, () -> target.teleportTo(
                nether,
                netherTarget.getX() + 0.5D,
                netherTarget.getY(),
                netherTarget.getZ() + 0.5D,
                Relative.DELTA,
                target.getYRot(),
                target.getXRot(),
                false
        ));

        helper.runAtTickTime(360, () -> {
            try {
                require(helper, requester.level() == nether,
                        "Friend teleport did not follow the target's latest Dimension");
                require(helper, requester.blockPosition().equals(netherTarget),
                        "Cross-dimension friend teleport did not use the target's latest safe landing");
                require(helper, !sessions().containsKey(requester.getUUID()),
                        "Cross-dimension PLAYER session remained tracked after completion");
                helper.succeed();
            } finally {
                cleanup(helper, requester, target);
            }
        });
    }

    private static void startPlayerTeleport(ServerPlayer requester, ServerPlayer target) {
        SpaceUnitHandler.establishInterfaceContext(
                requester,
                InteractionHand.MAIN_HAND,
                SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                requester.getUUID()
        ).orElseThrow(() -> new IllegalStateException("Could not establish compass interface context"));
        SpaceUnitHandler.startTeleport(
                requester,
                SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                requester.getUUID(),
                target.getUUID()
        );
    }

    private static ServerPlayer createPlayer(GameTestHelper helper, BlockPos relativePos) {
        placeSafeLanding(helper.getLevel(), helper.absolutePos(relativePos));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        snapTo(player, helper.absolutePos(relativePos));
        return player;
    }

    private static void preparePlayer(ServerPlayer player, boolean creative) {
        player.getAbilities().instabuild = creative;
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COMPASS));
    }

    private static void snapTo(ServerPlayer player, BlockPos pos) {
        player.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
    }

    private static void placeSafeLanding(ServerLevel level, BlockPos feetPos) {
        level.setBlockAndUpdate(feetPos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(feetPos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(feetPos.above(), Blocks.AIR.defaultBlockState());
    }

    private static void removeFloor(ServerLevel level, BlockPos feetPos) {
        level.setBlockAndUpdate(feetPos.below(), Blocks.AIR.defaultBlockState());
    }

    private static void makeFriends(GameTestHelper helper, ServerPlayer first, ServerPlayer second) {
        DeadRecallFriendSavedData data = friendData(helper);
        require(helper,
                data.inviteOrAccept(first.getUUID(), second.getUUID())
                        == DeadRecallFriendSavedData.FriendActionResult.INVITED,
                "Could not create first direction friend invite");
        require(helper,
                data.inviteOrAccept(second.getUUID(), first.getUUID())
                        == DeadRecallFriendSavedData.FriendActionResult.ACCEPTED,
                "Could not accept reverse friend invite");
    }

    private static DeadRecallFriendSavedData friendData(GameTestHelper helper) {
        return helper.getLevel().getServer().overworld().getDataStorage()
                .computeIfAbsent(DeadRecallFriendSavedData.TYPE);
    }

    private static Map<UUID, Object> sessions() {
        try {
            Field field = SpaceUnitHandler.class.getDeclaredField("teleportSessions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, Object> sessions = (Map<UUID, Object>) field.get(null);
            return sessions;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not inspect active teleport sessions", exception);
        }
    }

    private static void requireOnline(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer player : players) {
            require(helper,
                    helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player,
                    "GameTest mock player was not registered in the server PlayerList: " + player.getUUID());
        }
    }

    private static void cleanup(GameTestHelper helper, ServerPlayer... players) {
        DeadRecallFriendSavedData data = friendData(helper);
        for (ServerPlayer player : players) {
            sessions().remove(player.getUUID());
            SpaceUnitHandler.clearInterfaceContext(player.getUUID());
        }
        for (int first = 0; first < players.length; first++) {
            for (int second = first + 1; second < players.length; second++) {
                data.removeRelationship(players[first].getUUID(), players[second].getUUID());
            }
        }
        for (ServerPlayer player : players) {
            if (!player.isRemoved()) {
                player.discard();
            }
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
