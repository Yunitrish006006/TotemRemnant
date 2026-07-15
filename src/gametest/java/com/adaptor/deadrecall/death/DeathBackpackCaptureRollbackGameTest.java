package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.mixin.DeadRecallSpaceUnitSavedDataAccessor;
import com.adaptor.deadrecall.space.DeadRecallSpaceDiscoverySavedData;
import com.adaptor.deadrecall.space.DeadRecallSpaceUnitSavedData;
import com.adaptor.deadrecall.space.SpaceUnitRecord;
import com.adaptor.deadrecall.space.SpaceUnitStatus;
import com.adaptor.deadrecall.space.SpaceUnitType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DeathBackpackCaptureRollbackGameTest {
    private static final BlockPos CAPTURE_POS = new BlockPos(2, 2, 2);

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void restoresSlotsAndDiscardsEntityWhenSpawnedCaptureFails(GameTestHelper helper) {
        helper.setBlock(CAPTURE_POS.below(), Blocks.STONE);
        BlockPos absolutePos = helper.absolutePos(CAPTURE_POS);
        ServerPlayer player = createPlayerAt(helper, absolutePos);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 11));

        Set<UUID> nodeIdsBefore = deathNodeIds(helper, player.getUUID());
        DeathBackpackCaptureService.forceFailureForTesting(
                player.getUUID(),
                DeathBackpackCaptureService.CaptureFailurePoint.AFTER_ENTITY_ADD
        );

        try {
            boolean captured = DeathBackpackCaptureService.captureBeforeVanillaDrop(player, helper.getLevel());
            require(helper, !captured, "Forced entity-stage failure unexpectedly committed the capture");
            require(helper, player.getInventory().getItem(0).is(Items.DIAMOND)
                            && player.getInventory().getItem(0).getCount() == 11,
                    "Captured slot was not restored after entity-stage failure");
            require(helper, deathBackpacksAround(helper, absolutePos).isEmpty(),
                    "Incomplete death-backpack ItemEntity remained after rollback");
            require(helper, deathNodeIds(helper, player.getUUID()).equals(nodeIdsBefore),
                    "Entity-stage failure unexpectedly created a death node");
            require(helper, !DeathBackpackCaptureService.consumeCompletedCapture(player.getUUID()),
                    "Failed capture was incorrectly marked completed");
            helper.succeed();
        } finally {
            DeathBackpackCaptureService.clearForcedFailureForTesting(player.getUUID());
            player.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void disablesNodeAndRemovesDiscoveryWhenPostNodeCaptureFails(GameTestHelper helper) {
        helper.setBlock(CAPTURE_POS.below(), Blocks.STONE);
        BlockPos absolutePos = helper.absolutePos(CAPTURE_POS);
        ServerPlayer player = createPlayerAt(helper, absolutePos);
        player.getInventory().setItem(0, new ItemStack(Items.EMERALD, 7));

        Set<UUID> nodeIdsBefore = deathNodeIds(helper, player.getUUID());
        DeathBackpackCaptureService.forceFailureForTesting(
                player.getUUID(),
                DeathBackpackCaptureService.CaptureFailurePoint.AFTER_DEATH_NODE_CREATE
        );

        try {
            boolean captured = DeathBackpackCaptureService.captureBeforeVanillaDrop(player, helper.getLevel());
            require(helper, !captured, "Forced post-node failure unexpectedly committed the capture");
            require(helper, player.getInventory().getItem(0).is(Items.EMERALD)
                            && player.getInventory().getItem(0).getCount() == 7,
                    "Captured slot was not restored after post-node failure");
            require(helper, deathBackpacksAround(helper, absolutePos).isEmpty(),
                    "Incomplete death-backpack ItemEntity remained after post-node rollback");

            Map<UUID, SpaceUnitRecord> units = unitRecords(helper);
            Set<UUID> nodeIdsAfter = deathNodeIds(helper, player.getUUID());
            Set<UUID> newNodeIds = new HashSet<>(nodeIdsAfter);
            newNodeIds.removeAll(nodeIdsBefore);
            require(helper, newNodeIds.size() == 1,
                    "Expected one rollback tombstone for the failed death node, found " + newNodeIds.size());

            UUID rolledBackNodeId = newNodeIds.iterator().next();
            SpaceUnitRecord rolledBackNode = units.get(rolledBackNodeId);
            require(helper, rolledBackNode != null && rolledBackNode.status() == SpaceUnitStatus.DISABLED,
                    "Failed death node remained active after rollback");

            DeadRecallSpaceDiscoverySavedData discovery = helper.getLevel().getServer()
                    .overworld()
                    .getDataStorage()
                    .computeIfAbsent(DeadRecallSpaceDiscoverySavedData.TYPE);
            require(helper, !discovery.hasDiscovered(player.getUUID(), rolledBackNodeId),
                    "Failed death node remained in the player's discovery data");
            require(helper, !DeathBackpackCaptureService.consumeCompletedCapture(player.getUUID()),
                    "Failed capture was incorrectly marked completed");
            helper.succeed();
        } finally {
            DeathBackpackCaptureService.clearForcedFailureForTesting(player.getUUID());
            player.discard();
        }
    }

    private static ServerPlayer createPlayerAt(GameTestHelper helper, BlockPos absolutePos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(
                absolutePos.getX() + 0.5,
                absolutePos.getY(),
                absolutePos.getZ() + 0.5,
                0.0F,
                0.0F
        );
        return player;
    }

    private static Set<ItemEntity> deathBackpacksAround(GameTestHelper helper, BlockPos absolutePos) {
        return new HashSet<>(helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(absolutePos).inflate(4.0),
                entity -> entity.isAlive() && BackpackItemHelper.isDeathBackpackItem(entity.getItem())
        ));
    }

    private static Set<UUID> deathNodeIds(GameTestHelper helper, UUID ownerId) {
        Set<UUID> ids = new HashSet<>();
        for (SpaceUnitRecord unit : unitRecords(helper).values()) {
            if (unit.type() == SpaceUnitType.DEATH && unit.owner().equals(ownerId)) {
                ids.add(unit.id());
            }
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, SpaceUnitRecord> unitRecords(GameTestHelper helper) {
        DeadRecallSpaceUnitSavedData data = helper.getLevel().getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);
        return ((DeadRecallSpaceUnitSavedDataAccessor) (Object) data).deadrecall$getUnitsById();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
