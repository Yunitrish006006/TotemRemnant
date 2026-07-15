package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.inventory.BackpackInventory;
import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.mixin.DeadRecallSpaceUnitSavedDataAccessor;
import com.adaptor.deadrecall.space.DeadRecallSpaceDiscoverySavedData;
import com.adaptor.deadrecall.space.DeadRecallSpaceUnitSavedData;
import com.adaptor.deadrecall.space.SpaceUnitRecord;
import com.adaptor.deadrecall.space.SpaceUnitStatus;
import com.adaptor.deadrecall.space.SpaceUnitType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DeathBackpackRecoveryLifecycleGameTest {
    private static final BlockPos FIRST_DEATH_POS = new BlockPos(2, 2, 2);
    private static final BlockPos SECOND_DEATH_POS = new BlockPos(7, 2, 2);
    private static final BlockPos RECOVERY_POS = new BlockPos(4, 2, 6);

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 80)
    public void nonOwnerRecoveryDisablesOnlyTheBoundNodeAfterOwnerDisconnects(GameTestHelper helper) {
        prepareGround(helper, FIRST_DEATH_POS, SECOND_DEATH_POS, RECOVERY_POS);
        BlockPos firstAbsolutePos = helper.absolutePos(FIRST_DEATH_POS);
        BlockPos secondAbsolutePos = helper.absolutePos(SECOND_DEATH_POS);
        ServerPlayer firstOwner = createPlayerAt(helper, firstAbsolutePos);
        ServerPlayer secondOwner = createPlayerAt(helper, secondAbsolutePos);
        firstOwner.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        secondOwner.getInventory().setItem(0, new ItemStack(Items.EMERALD, 5));

        firstOwner.die(helper.getLevel().damageSources().generic());
        secondOwner.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            ServerPlayer recoveringPlayer = createPlayerAt(helper, helper.absolutePos(RECOVERY_POS));
            try {
                require(helper, !recoveringPlayer.getUUID().equals(firstOwner.getUUID()),
                        "Recovery fixture did not create a distinct non-owner player");

                List<ItemEntity> backpacks = deathBackpacksAround(helper, firstAbsolutePos, 10.0);
                require(helper, backpacks.size() == 2,
                        "Expected two independent death backpacks before recovery, found " + backpacks.size());
                ItemEntity firstBackpackEntity = findBackpackContaining(helper, backpacks, Items.DIAMOND, 3);
                ItemEntity secondBackpackEntity = findBackpackContaining(helper, backpacks, Items.EMERALD, 5);

                UUID firstNodeId = activeDeathNodeId(helper, firstOwner.getUUID());
                UUID secondNodeId = activeDeathNodeId(helper, secondOwner.getUUID());
                firstOwner.discard();
                require(helper, firstBackpackEntity.isAlive(),
                        "Disconnecting the owner removed the death backpack entity");

                recoverBackpack(recoveringPlayer, firstBackpackEntity);

                Map<UUID, SpaceUnitRecord> units = unitRecords(helper);
                require(helper, units.get(firstNodeId).status() == SpaceUnitStatus.DISABLED,
                        "Non-owner recovery did not disable the bound death node");
                require(helper, units.get(secondNodeId).status() == SpaceUnitStatus.ACTIVE,
                        "Recovering one backpack disabled an unrelated death node");
                require(helper, recoveringPlayer.getMainHandItem().isEmpty(),
                        "Empty recovered death backpack remained in the recovering player's hand");
                require(helper, secondBackpackEntity.isAlive()
                                && storedItems(secondBackpackEntity.getItem()).stream().anyMatch(stack ->
                                stack.is(Items.EMERALD) && stack.getCount() == 5),
                        "Unrelated death backpack was removed or modified");
                helper.succeed();
            } finally {
                firstOwner.discard();
                secondOwner.discard();
                recoveringPlayer.discard();
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 80)
    public void notificationFailureCannotInterruptNodeRecoveryOrBackpackRemoval(GameTestHelper helper) {
        prepareGround(helper, FIRST_DEATH_POS, RECOVERY_POS);
        BlockPos absoluteDeathPos = helper.absolutePos(FIRST_DEATH_POS);
        ServerPlayer owner = createPlayerAt(helper, absoluteDeathPos);
        owner.getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 6));
        owner.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            ServerPlayer recoveringPlayer = createPlayerAt(helper, helper.absolutePos(RECOVERY_POS));
            try {
                ItemEntity backpackEntity = findBackpackContaining(
                        helper,
                        deathBackpacksAround(helper, absoluteDeathPos, 5.0),
                        Items.GOLD_INGOT,
                        6
                );
                UUID nodeId = activeDeathNodeId(helper, owner.getUUID());
                DeathBackpackRecoveryService.forceNotificationFailureForTesting(recoveringPlayer.getUUID());

                recoverBackpack(recoveringPlayer, backpackEntity);

                require(helper, unitRecords(helper).get(nodeId).status() == SpaceUnitStatus.DISABLED,
                        "Notification failure left the recovered death node active");
                require(helper, recoveringPlayer.getMainHandItem().isEmpty(),
                        "Notification failure interrupted empty death-backpack removal");
                helper.succeed();
            } finally {
                DeathBackpackRecoveryService.clearForcedNotificationFailureForTesting(recoveringPlayer.getUUID());
                owner.discard();
                recoveringPlayer.discard();
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 80)
    public void recoveredNodeAndDiscoverySurviveSavedDataCodecRoundTrip(GameTestHelper helper) {
        prepareGround(helper, FIRST_DEATH_POS);
        BlockPos absoluteDeathPos = helper.absolutePos(FIRST_DEATH_POS);
        ServerPlayer owner = createPlayerAt(helper, absoluteDeathPos);
        owner.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 4));
        owner.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                ItemEntity backpackEntity = findBackpackContaining(
                        helper,
                        deathBackpacksAround(helper, absoluteDeathPos, 5.0),
                        Items.IRON_INGOT,
                        4
                );
                UUID nodeId = activeDeathNodeId(helper, owner.getUUID());
                recoverBackpack(owner, backpackEntity);

                DeadRecallSpaceUnitSavedData units = units(helper);
                DeadRecallSpaceDiscoverySavedData discovery = discovery(helper);
                Tag encodedUnits = DeadRecallSpaceUnitSavedData.CODEC
                        .encodeStart(NbtOps.INSTANCE, units)
                        .getOrThrow();
                Tag encodedDiscovery = DeadRecallSpaceDiscoverySavedData.CODEC
                        .encodeStart(NbtOps.INSTANCE, discovery)
                        .getOrThrow();
                DeadRecallSpaceUnitSavedData decodedUnits = DeadRecallSpaceUnitSavedData.CODEC
                        .parse(NbtOps.INSTANCE, encodedUnits)
                        .getOrThrow();
                DeadRecallSpaceDiscoverySavedData decodedDiscovery = DeadRecallSpaceDiscoverySavedData.CODEC
                        .parse(NbtOps.INSTANCE, encodedDiscovery)
                        .getOrThrow();

                SpaceUnitRecord decodedNode = decodedUnits.get(nodeId)
                        .orElseThrow(() -> helper.assertionException(
                                "Recovered death node disappeared during SavedData codec round-trip"));
                require(helper, decodedNode.type() == SpaceUnitType.DEATH,
                        "Recovered node changed type during SavedData codec round-trip");
                require(helper, decodedNode.status() == SpaceUnitStatus.DISABLED,
                        "Recovered node did not preserve DISABLED status across SavedData codec round-trip");
                require(helper, decodedNode.owner().equals(owner.getUUID()),
                        "Recovered node owner changed during SavedData codec round-trip");
                require(helper, decodedDiscovery.hasDiscovered(owner.getUUID(), nodeId),
                        "Death-node discovery reference did not survive SavedData codec round-trip");
                helper.succeed();
            } finally {
                owner.discard();
            }
        });
    }

    private static void recoverBackpack(ServerPlayer recoveringPlayer, ItemEntity backpackEntity) {
        ItemStack backpackStack = backpackEntity.getItem();
        backpackEntity.discard();
        recoveringPlayer.setItemInHand(InteractionHand.MAIN_HAND, backpackStack);
        BackpackInventory inventory = new BackpackInventory(recoveringPlayer, InteractionHand.MAIN_HAND, 9);
        inventory.clearContent();
        inventory.onClose(recoveringPlayer);
    }

    private static ItemEntity findBackpackContaining(
            GameTestHelper helper,
            List<ItemEntity> backpacks,
            net.minecraft.world.item.Item item,
            int count
    ) {
        return backpacks.stream()
                .filter(entity -> storedItems(entity.getItem()).stream().anyMatch(stack ->
                        stack.is(item) && stack.getCount() == count))
                .findFirst()
                .orElseThrow(() -> helper.assertionException(
                        "Could not find death backpack containing " + item + " x" + count));
    }

    private static List<ItemEntity> deathBackpacksAround(
            GameTestHelper helper,
            BlockPos absolutePos,
            double radius
    ) {
        return helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(absolutePos).inflate(radius),
                entity -> entity.isAlive() && BackpackItemHelper.isDeathBackpackItem(entity.getItem())
        );
    }

    private static List<ItemStack> storedItems(ItemStack deathBackpack) {
        return deathBackpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream()
                .toList();
    }

    private static UUID activeDeathNodeId(GameTestHelper helper, UUID ownerId) {
        Set<UUID> matching = unitRecords(helper).values().stream()
                .filter(unit -> unit.type() == SpaceUnitType.DEATH)
                .filter(unit -> unit.status() == SpaceUnitStatus.ACTIVE)
                .filter(unit -> unit.owner().equals(ownerId))
                .map(SpaceUnitRecord::id)
                .collect(Collectors.toSet());
        if (matching.size() != 1) {
            throw helper.assertionException(
                    "Expected exactly one active death node for owner " + ownerId + ", found " + matching.size());
        }
        return matching.iterator().next();
    }

    private static void prepareGround(GameTestHelper helper, BlockPos... positions) {
        for (BlockPos position : positions) {
            helper.setBlock(position.below(), Blocks.STONE);
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

    private static DeadRecallSpaceUnitSavedData units(GameTestHelper helper) {
        return helper.getLevel().getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);
    }

    private static DeadRecallSpaceDiscoverySavedData discovery(GameTestHelper helper) {
        return helper.getLevel().getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(DeadRecallSpaceDiscoverySavedData.TYPE);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, SpaceUnitRecord> unitRecords(GameTestHelper helper) {
        return ((DeadRecallSpaceUnitSavedDataAccessor) (Object) units(helper)).deadrecall$getUnitsById();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
