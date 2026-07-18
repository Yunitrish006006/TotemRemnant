package com.adaptor.deadrecall.space;

import com.adaptor.deadrecall.network.SpaceUnitMapPayload;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public final class TeleportInterfacePhaseDGameTest {
    private static final BlockPos PLAYER_POS = new BlockPos(3, 20, 3);
    private static final BlockPos TARGET_POS = new BlockPos(18, 20, 3);
    private static final List<BlockPos> SUPPORT_OFFSETS = List.of(
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 1),
            new BlockPos(-1, 0, -1),
            new BlockPos(1, 0, -1),
            new BlockPos(-1, 0, 1)
    );

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void bookPayloadCarriesBaseAndFinalQuoteDetails(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = createPlayer(helper);
        BlockPos targetPos = helper.absolutePos(TARGET_POS);
        placeLodestoneStructure(level, targetPos);
        SpaceUnitRecord target = units(level.getServer()).getOrCreateLodestone(level, targetPos, player);
        discovery(level.getServer()).markDiscovered(player.getUUID(), target.id());

        try {
            player.setItemSlot(EquipmentSlot.MAINHAND, Items.BOOK.getDefaultInstance());
            establishContext(player);
            SpaceUnitMapPayload.Entry entry = entryFor(player, target);

            require(helper, entry.baseFoodCost() == entry.finalFoodCost(),
                    "Book quote changed food cost in Phase D details");
            require(helper, entry.prepareTicks() == Math.max(
                            30,
                            (int) Math.ceil(entry.basePrepareTicks() * 0.80D)),
                    "Book final preparation time does not match its base quote");
            require(helper, entry.baseMaxHorizontalDeviation() == entry.maxHorizontalDeviation(),
                    "Book quote changed deviation in Phase D details");
            require(helper, entry.structureWearChancePercent()
                            == (int) Math.floor(entry.baseStructureWearChancePercent() * 0.75D),
                    "Book final wear chance does not match its base quote");
            helper.succeed();
        } finally {
            discovery(level.getServer()).removeDiscovered(player.getUUID(), target.id());
            units(level.getServer()).disableLodestone(target.dimension(), target.pos(), level.getGameTime());
            cleanup(player);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void recoveryCompassDeathPayloadCarriesBaseAndFinalQuoteDetails(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = createPlayer(helper);
        BlockPos deathPos = helper.absolutePos(TARGET_POS);
        UUID deathNodeId = SpaceUnitHandler.createDeathNode(player, level, deathPos);
        SpaceUnitRecord target = units(level.getServer()).get(deathNodeId)
                .orElseThrow(() -> helper.assertionException("Created death node was absent"));

        try {
            player.setItemSlot(EquipmentSlot.MAINHAND, Items.RECOVERY_COMPASS.getDefaultInstance());
            establishContext(player);
            SpaceUnitMapPayload.Entry entry = entryFor(player, target);

            require(helper, entry.baseFoodCost() == entry.finalFoodCost(),
                    "Recovery compass changed death-node food cost");
            require(helper, entry.basePrepareTicks() == entry.prepareTicks(),
                    "Recovery compass changed death-node preparation time");
            require(helper, entry.maxHorizontalDeviation()
                            == (int) Math.floor(entry.baseMaxHorizontalDeviation() * 0.50D),
                    "Recovery compass final deviation does not match its base quote");
            require(helper,
                    entry.baseStructureWearChancePercent() == entry.structureWearChancePercent(),
                    "Recovery compass changed death-node structure wear");
            helper.succeed();
        } finally {
            discovery(level.getServer()).removeDiscovered(player.getUUID(), deathNodeId);
            units(level.getServer()).disableDeathUnit(player.getUUID(), deathNodeId, level.getGameTime());
            cleanup(player);
        }
    }

    private static SpaceUnitMapPayload.Entry entryFor(ServerPlayer player, SpaceUnitRecord target) {
        SpaceUnitMapPayload payload = buildPlayerSourcePayload(player, List.of(target));
        return payload.entries().stream()
                .filter(entry -> entry.id().equals(target.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Target quote was absent from Phase D payload"));
    }

    private static SpaceUnitMapPayload buildPlayerSourcePayload(
            ServerPlayer player,
            List<SpaceUnitRecord> visibleUnits) {
        try {
            Class<?> mapSourceClass = Class.forName(
                    "com.adaptor.deadrecall.space.SpaceUnitHandler$MapSource");
            Constructor<?> constructor = mapSourceClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Object source = constructor.newInstance(
                    player.getUUID(),
                    SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                    player.getName().getString(),
                    player.level().dimension(),
                    player.blockPosition().immutable(),
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
            return (SpaceUnitMapPayload) method.invoke(null, player, source, visibleUnits);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not inspect Phase D payload", failure);
        }
    }

    private static void establishContext(ServerPlayer player) {
        SpaceUnitHandler.establishInterfaceContext(
                player,
                InteractionHand.MAIN_HAND,
                SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                player.getUUID()
        ).orElseThrow(() -> new IllegalStateException("Could not establish Phase D context"));
    }

    private static ServerPlayer createPlayer(GameTestHelper helper) {
        BlockPos feetPos = helper.absolutePos(PLAYER_POS);
        helper.getLevel().setBlockAndUpdate(feetPos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(feetPos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(feetPos.above(), Blocks.AIR.defaultBlockState());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(feetPos.getX() + 0.5D, feetPos.getY(), feetPos.getZ() + 0.5D, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        return player;
    }

    private static void placeLodestoneStructure(ServerLevel level, BlockPos lodestonePos) {
        level.setBlockAndUpdate(lodestonePos, Blocks.LODESTONE.defaultBlockState());
        level.setBlockAndUpdate(lodestonePos.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(lodestonePos.above(2), Blocks.AIR.defaultBlockState());
        for (BlockPos offset : SUPPORT_OFFSETS) {
            level.setBlockAndUpdate(
                    lodestonePos.offset(offset),
                    Blocks.POLISHED_DEEPSLATE.defaultBlockState()
            );
        }
    }

    private static DeadRecallSpaceUnitSavedData units(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);
    }

    private static DeadRecallSpaceDiscoverySavedData discovery(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeadRecallSpaceDiscoverySavedData.TYPE);
    }

    private static void cleanup(ServerPlayer player) {
        SpaceUnitHandler.clearInterfaceContext(player.getUUID());
        if (!player.isRemoved()) {
            player.discard();
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
