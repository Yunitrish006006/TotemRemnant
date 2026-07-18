package com.adaptor.deadrecall.space;

import com.adaptor.deadrecall.network.SpaceUnitMapPayload;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TeleportInterfacePhaseCGameTest {
    private static final BlockPos REQUESTER_POS = new BlockPos(2, 20, 2);
    private static final BlockPos TARGET_POS = new BlockPos(14, 20, 2);

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void filledMapPayloadUsesServerCoverageWithoutExactFriendCoordinates(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, REQUESTER_POS);
        ServerPlayer target = createPlayer(helper, TARGET_POS);
        try {
            prepareFriends(helper, requester, target);
            requester.getAbilities().instabuild = false;
            requester.getFoodData().setFoodLevel(20);
            requester.getFoodData().setSaturation(20.0F);
            requester.setItemSlot(EquipmentSlot.MAINHAND, coveringMap(helper.getLevel(), target.blockPosition()));
            establishMapContext(requester);

            SpaceUnitMapPayload payload = buildPlayerSourcePayload(requester);
            SpaceUnitMapPayload.Entry entry = payload.entries().stream()
                    .filter(candidate -> candidate.id().equals(target.getUUID()))
                    .findFirst()
                    .orElseThrow(() -> helper.assertionException(
                            "Covered friend target was absent from the filled-map payload"));

            require(helper, payload.interfaceType() == TeleportInterfaceType.FILLED_MAP,
                    "Payload did not retain its authoritative filled-map interface type");
            require(helper, entry.interfaceBonusActive(),
                    "Server map coverage did not activate the friend-target specialization");
            require(helper, entry.baseFoodCost() == 5 && entry.finalFoodCost() == 4,
                    "Covered friend payload did not retain its five-to-four base/final food quote");
            require(helper, entry.saturationCost() == 4,
                    "Covered five-point friend route did not receive the ceil(80%) food cost");
            require(helper, entry.interfaceBonusMessageKey().endsWith("filled_map.active"),
                    "Covered target did not receive the active map explanation key");
            require(helper,
                    entry.x() != target.blockPosition().getX()
                            || entry.y() != target.blockPosition().getY()
                            || entry.z() != target.blockPosition().getZ(),
                    "Filled-map coverage leaked the friend's exact Server position");
            require(helper, entry.distanceBlocks() >= 64 && entry.distanceBlocks() % 64 == 0,
                    "Filled-map coverage leaked an unrounded friend distance");
            helper.succeed();
        } finally {
            cleanup(helper, requester, target);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void movingCoveredFriendOutsideMapCancelsSessionBeforePayment(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, REQUESTER_POS);
        ServerPlayer target = createPlayer(helper, TARGET_POS);
        try {
            prepareFriends(helper, requester, target);
            requester.getAbilities().instabuild = false;
            requester.getFoodData().setFoodLevel(20);
            requester.getFoodData().setSaturation(20.0F);
            requester.setItemSlot(EquipmentSlot.MAINHAND, coveringMap(helper.getLevel(), target.blockPosition()));
            establishMapContext(requester);
            startTeleport(requester, target);
            require(helper, sessions().containsKey(requester.getUUID()),
                    "Covered friend route did not create a filled-map session");

            BlockPos outsideCoverage = target.blockPosition().offset(200, 0, 0);
            placeSafeLanding(helper.getLevel(), outsideCoverage);
            snapTo(target, outsideCoverage);
            SpaceUnitHandler.tickTeleportSessions(helper.getLevel().getServer());

            require(helper, !sessions().containsKey(requester.getUUID()),
                    "Filled-map session survived its dynamic target leaving coverage");
            require(helper, requester.getFoodData().getFoodLevel() == 20,
                    "Coverage-change cancellation deducted hunger");
            require(helper, requester.getFoodData().getSaturationLevel() == 20.0F,
                    "Coverage-change cancellation deducted saturation");
            helper.succeed();
        } finally {
            cleanup(helper, requester, target);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void removingMapIdCancelsSessionBeforePayment(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, REQUESTER_POS);
        ServerPlayer target = createPlayer(helper, TARGET_POS);
        try {
            prepareFriends(helper, requester, target);
            requester.getAbilities().instabuild = false;
            requester.getFoodData().setFoodLevel(20);
            requester.getFoodData().setSaturation(20.0F);
            requester.setItemSlot(EquipmentSlot.MAINHAND, coveringMap(helper.getLevel(), target.blockPosition()));
            establishMapContext(requester);
            startTeleport(requester, target);
            require(helper, sessions().containsKey(requester.getUUID()),
                    "Filled map did not create a session before map-ID removal");

            requester.setItemSlot(EquipmentSlot.MAINHAND, Items.FILLED_MAP.getDefaultInstance());
            SpaceUnitHandler.tickTeleportSessions(helper.getLevel().getServer());

            require(helper, !sessions().containsKey(requester.getUUID()),
                    "Session survived removal of the initiating filled-map ID");
            require(helper, requester.getFoodData().getFoodLevel() == 20,
                    "Map-ID cancellation deducted hunger");
            require(helper, requester.getFoodData().getSaturationLevel() == 20.0F,
                    "Map-ID cancellation deducted saturation");
            helper.succeed();
        } finally {
            cleanup(helper, requester, target);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void coveredCrossDimensionTargetKeepsAmethystCostUnchanged(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, REQUESTER_POS);
        ServerPlayer target = createPlayer(helper, TARGET_POS);
        try {
            prepareFriends(helper, requester, target);
            requester.getAbilities().instabuild = false;
            requester.getFoodData().setFoodLevel(20);
            requester.getFoodData().setSaturation(20.0F);
            requester.getInventory().add(new ItemStack(Items.AMETHYST_SHARD, 16));

            ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
            require(helper, nether != null, "Nether was unavailable for filled-map quote test");
            BlockPos netherTarget = new BlockPos(80, 80, 80);
            placeSafeLanding(nether, netherTarget);
            target.teleportTo(
                    nether,
                    netherTarget.getX() + 0.5D,
                    netherTarget.getY(),
                    netherTarget.getZ() + 0.5D,
                    Relative.DELTA,
                    target.getYRot(),
                    target.getXRot(),
                    false
            );

            requester.setItemSlot(EquipmentSlot.MAINHAND, Items.COMPASS.getDefaultInstance());
            establishMapContext(requester);
            SpaceUnitMapPayload.Entry baseline = friendEntry(buildPlayerSourcePayload(requester), target, helper);
            SpaceUnitHandler.clearInterfaceContext(requester.getUUID());

            requester.setItemSlot(EquipmentSlot.MAINHAND, coveringMap(nether, target.blockPosition()));
            establishMapContext(requester);
            SpaceUnitMapPayload.Entry covered = friendEntry(buildPlayerSourcePayload(requester), target, helper);

            require(helper, covered.interfaceBonusActive(),
                    "Map of the target Dimension did not cover its cross-Dimension friend target");
            require(helper, covered.baseFoodCost() == baseline.baseFoodCost(),
                    "Filled-map specialization changed the cross-Dimension base food quote");
            require(helper, covered.finalFoodCost() < covered.baseFoodCost(),
                    "Filled-map payload did not retain a reduced final food quote");
            require(helper, covered.saturationCost() < baseline.saturationCost(),
                    "Covered cross-Dimension route did not reduce food-equivalent cost");
            require(helper, baseline.amethystCost() > 0,
                    "Cross-Dimension baseline unexpectedly had no amethyst cost");
            require(helper, covered.amethystCost() == baseline.amethystCost(),
                    "Filled-map specialization changed the cross-Dimension amethyst cost");
            helper.succeed();
        } finally {
            cleanup(helper, requester, target);
        }
    }

    private static ItemStack coveringMap(ServerLevel level, BlockPos targetPos) {
        return MapItem.create(level, targetPos.getX(), targetPos.getZ(), (byte) 0, true, false);
    }

    private static void establishMapContext(ServerPlayer requester) {
        SpaceUnitHandler.establishInterfaceContext(
                requester,
                InteractionHand.MAIN_HAND,
                SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                requester.getUUID()
        ).orElseThrow(() -> new IllegalStateException("Could not establish filled-map context"));
    }

    private static void startTeleport(ServerPlayer requester, ServerPlayer target) {
        SpaceUnitHandler.startTeleport(
                requester,
                SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                requester.getUUID(),
                target.getUUID()
        );
    }

    private static SpaceUnitMapPayload buildPlayerSourcePayload(ServerPlayer requester) {
        try {
            Class<?> mapSourceClass = Class.forName(
                    "com.adaptor.deadrecall.space.SpaceUnitHandler$MapSource");
            Constructor<?> constructor = mapSourceClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Object source = constructor.newInstance(
                    requester.getUUID(),
                    SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                    requester.getName().getString(),
                    requester.level().dimension(),
                    requester.blockPosition().immutable(),
                    0.6D,
                    0,
                    SpaceUnitType.PLAYER
            );
            Method method = SpaceUnitHandler.class.getDeclaredMethod(
                    "buildMapPayload",
                    ServerPlayer.class,
                    mapSourceClass,
                    List.class
            );
            method.setAccessible(true);
            return (SpaceUnitMapPayload) method.invoke(null, requester, source, List.of());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not inspect filled-map payload", failure);
        }
    }

    private static SpaceUnitMapPayload.Entry friendEntry(
            SpaceUnitMapPayload payload,
            ServerPlayer target,
            GameTestHelper helper) {
        return payload.entries().stream()
                .filter(candidate -> candidate.id().equals(target.getUUID()))
                .findFirst()
                .orElseThrow(() -> helper.assertionException(
                        "Friend target was absent from the authoritative map payload"));
    }

    private static ServerPlayer createPlayer(GameTestHelper helper, BlockPos relativePos) {
        BlockPos feetPos = helper.absolutePos(relativePos);
        placeSafeLanding(helper.getLevel(), feetPos);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        snapTo(player, feetPos);
        return player;
    }

    private static void snapTo(ServerPlayer player, BlockPos pos) {
        player.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
    }

    private static void placeSafeLanding(ServerLevel level, BlockPos feetPos) {
        level.setBlockAndUpdate(feetPos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(feetPos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(feetPos.above(), Blocks.AIR.defaultBlockState());
    }

    private static void prepareFriends(
            GameTestHelper helper,
            ServerPlayer requester,
            ServerPlayer target) {
        DeadRecallFriendSavedData data = friendData(helper);
        require(helper,
                data.inviteOrAccept(requester.getUUID(), target.getUUID())
                        == DeadRecallFriendSavedData.FriendActionResult.INVITED,
                "Could not create friend invitation for Phase C test");
        require(helper,
                data.inviteOrAccept(target.getUUID(), requester.getUUID())
                        == DeadRecallFriendSavedData.FriendActionResult.ACCEPTED,
                "Could not accept friend invitation for Phase C test");
    }

    private static DeadRecallFriendSavedData friendData(GameTestHelper helper) {
        return helper.getLevel().getServer().overworld().getDataStorage()
                .computeIfAbsent(DeadRecallFriendSavedData.TYPE);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Object> sessions() {
        try {
            Field field = SpaceUnitHandler.class.getDeclaredField("teleportSessions");
            field.setAccessible(true);
            return (Map<UUID, Object>) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not inspect Phase C teleport sessions", failure);
        }
    }

    private static void cleanup(GameTestHelper helper, ServerPlayer requester, ServerPlayer target) {
        sessions().remove(requester.getUUID());
        SpaceUnitHandler.clearInterfaceContext(requester.getUUID());
        SpaceUnitHandler.clearInterfaceContext(target.getUUID());
        friendData(helper).removeRelationship(requester.getUUID(), target.getUUID());
        if (!requester.isRemoved()) {
            requester.discard();
        }
        if (!target.isRemoved()) {
            target.discard();
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
