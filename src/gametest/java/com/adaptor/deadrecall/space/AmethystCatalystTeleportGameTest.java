package com.adaptor.deadrecall.space;

import com.adaptor.deadrecall.mixin.DeadRecallSpaceUnitSavedDataAccessor;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AmethystCatalystTeleportGameTest {
    private static final BlockPos SOURCE_POS = new BlockPos(4, 20, 4);
    private static final BlockPos PLAYER_SOURCE_POS = new BlockPos(10, 20, 4);
    private static final BlockPos NETHER_TARGET_POS = new BlockPos(20, 100, 20);
    private static final int INITIAL_SHARDS = 16;

    private static final List<BlockPos> CATALYST_OFFSETS = List.of(
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1)
    );
    private static final List<BlockPos> SUPPORT_OFFSETS = List.of(
            new BlockPos(2, 0, 0),
            new BlockPos(-2, 0, 0),
            new BlockPos(0, 0, 2),
            new BlockPos(0, 0, -2),
            new BlockPos(1, 0, 1),
            new BlockPos(-1, 0, -1),
            new BlockPos(1, 0, -1),
            new BlockPos(-1, 0, 1)
    );

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 380)
    public void completionRescansRemovedCatalystsAndChargesFreshCost(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel nether = requireNether(helper);
        BlockPos sourcePos = helper.absolutePos(SOURCE_POS);
        BlockPos targetPos = helper.absolutePos(NETHER_TARGET_POS);
        placeLodestoneStructure(helper.getLevel(), sourcePos, true);
        placeLodestoneStructure(nether, targetPos, true);

        ServerPlayer player = createPlayer(helper, sourcePos.above());
        prepareSurvivalPlayer(player);
        DeadRecallSpaceUnitSavedData units = units(server);
        SpaceUnitRecord source = units.getOrCreateLodestone(helper.getLevel(), sourcePos, player);
        SpaceUnitRecord target = units.getOrCreateLodestone(nether, targetPos, player);
        discover(server, player, source, target);

        require(helper, source.structure().amethystCatalystBlocks() == 4,
                "Source scan did not count four catalyst blocks");
        require(helper, target.structure().amethystCatalystBlocks() == 4,
                "Target scan did not count four catalyst blocks");
        int initialBaseCost = lodestoneBaseCost(source.structure(), target.structure());
        int staleCost = AmethystCatalystDiscount.finalCost(initialBaseCost, 4, 4);

        try {
            startTeleport(
                    player,
                    SpaceUnitHandler.SOURCE_TYPE_LODESTONE,
                    source.id(),
                    target.id()
            );
            require(helper, sessions().containsKey(player.getUUID()),
                    "Cross-dimension lodestone teleport session did not start");
        } catch (Throwable failure) {
            cleanup(server, player, source, target);
            throw failure;
        }

        helper.runAtTickTime(5, () -> removeCatalysts(nether, targetPos));
        helper.runAtTickTime(330, () -> {
            try {
                SpaceUnitRecord refreshedSource = units.get(source.id()).orElseThrow();
                SpaceUnitRecord refreshedTarget = units.get(target.id()).orElseThrow();
                int finalBaseCost = lodestoneBaseCost(
                        refreshedSource.structure(),
                        refreshedTarget.structure()
                );
                int freshCost = AmethystCatalystDiscount.finalCost(finalBaseCost, 4, 0);

                require(helper, refreshedTarget.structure().amethystCatalystBlocks() == 0,
                        "Completion did not rescan catalysts removed after the initial quote");
                require(helper, freshCost > staleCost,
                        "Fixture did not distinguish the stale and refreshed catalyst quote");
                require(helper, player.level() == nether,
                        "Catalyst teleport did not complete in the target Dimension");
                require(helper, countShards(player) == INITIAL_SHARDS - freshCost,
                        "Completion charged a stale catalyst quote instead of the refreshed final cost");
                require(helper, !sessions().containsKey(player.getUUID()),
                        "Completed catalyst teleport session remained active");
                helper.succeed();
            } finally {
                cleanup(server, player, source, target);
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 380)
    public void playerSourceAndDeathTargetCannotContributeCatalysts(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel nether = requireNether(helper);
        BlockPos sourceFeet = helper.absolutePos(PLAYER_SOURCE_POS);
        BlockPos targetPos = helper.absolutePos(NETHER_TARGET_POS.offset(0, 0, 8));
        placeSafeLanding(helper.getLevel(), sourceFeet);
        placeSafeLanding(nether, targetPos);

        ServerPlayer player = createPlayer(helper, sourceFeet);
        prepareSurvivalPlayer(player);
        DeadRecallSpaceUnitSavedData units = units(server);
        SpaceUnitRecord target = units.createDeathUnit(nether, targetPos, player);
        SpaceStructureSnapshot impossibleStoredCatalysts = new SpaceStructureSnapshot(
                1.0D,
                1.0D,
                1.0D,
                0.0D,
                1.0D,
                0.0D,
                2,
                64
        );
        target = target.withStructure(impossibleStoredCatalysts, nether.getGameTime());
        unitRecords(units).put(target.id(), target);
        units.setDirty();
        discovery(server).markDiscovered(player.getUUID(), target.id());

        double routeStability = crossDimensionStability(0.6D, 0.55D, SpaceUnitType.DEATH, true);
        int expectedCost = baseCost(routeStability);
        SpaceUnitRecord finalTarget = target;
        try {
            startTeleport(
                    player,
                    SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                    player.getUUID(),
                    target.id()
            );
            require(helper, sessions().containsKey(player.getUUID()),
                    "Player-to-death cross-dimension session did not start");
        } catch (Throwable failure) {
            cleanup(server, player, finalTarget);
            throw failure;
        }

        helper.runAtTickTime(330, () -> {
            try {
                require(helper, player.level() == nether,
                        "Player-to-death teleport did not reach the target Dimension");
                require(helper, countShards(player) == INITIAL_SHARDS - expectedCost,
                        "Player or death endpoint incorrectly contributed stored catalyst discount");
                helper.succeed();
            } finally {
                cleanup(server, player, finalTarget);
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 380)
    public void bothLodestonesCombineCatalystsForFinalPayment(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel nether = requireNether(helper);
        BlockPos sourcePos = helper.absolutePos(SOURCE_POS.offset(0, 0, 24));
        BlockPos targetPos = helper.absolutePos(NETHER_TARGET_POS.offset(0, 0, 24));
        placeLodestoneStructure(helper.getLevel(), sourcePos, true);
        placeLodestoneStructure(nether, targetPos, true);

        ServerPlayer player = createPlayer(helper, sourcePos.above());
        prepareSurvivalPlayer(player);
        DeadRecallSpaceUnitSavedData units = units(server);
        SpaceUnitRecord source = units.getOrCreateLodestone(helper.getLevel(), sourcePos, player);
        SpaceUnitRecord target = units.getOrCreateLodestone(nether, targetPos, player);
        discover(server, player, source, target);
        int baseCost = lodestoneBaseCost(source.structure(), target.structure());
        int expectedCost = AmethystCatalystDiscount.finalCost(baseCost, 4, 4);

        try {
            startTeleport(
                    player,
                    SpaceUnitHandler.SOURCE_TYPE_LODESTONE,
                    source.id(),
                    target.id()
            );
            require(helper, sessions().containsKey(player.getUUID()),
                    "Two-lodestone catalyst session did not start");
        } catch (Throwable failure) {
            cleanup(server, player, source, target);
            throw failure;
        }

        helper.runAtTickTime(330, () -> {
            try {
                require(helper, player.level() == nether,
                        "Two-lodestone catalyst teleport did not reach the target Dimension");
                require(helper, countShards(player) == INITIAL_SHARDS - expectedCost,
                        "Final payment did not combine catalysts from both lodestone endpoints");
                require(helper, !sessions().containsKey(player.getUUID()),
                        "Two-lodestone catalyst session remained active after completion");
                helper.succeed();
            } finally {
                cleanup(server, player, source, target);
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 380)
    public void playerTargetCannotContributeCatalysts(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel nether = requireNether(helper);
        BlockPos sourcePos = helper.absolutePos(SOURCE_POS.offset(0, 0, 12));
        BlockPos targetFeet = helper.absolutePos(NETHER_TARGET_POS.offset(0, 0, 16));
        placeLodestoneStructure(helper.getLevel(), sourcePos, true);
        placeSafeLanding(nether, targetFeet);

        ServerPlayer requester = createPlayer(helper, sourcePos.above());
        ServerPlayer targetPlayer = createPlayer(helper, helper.absolutePos(PLAYER_SOURCE_POS.offset(0, 0, 12)));
        prepareSurvivalPlayer(requester);
        targetPlayer.getAbilities().instabuild = true;
        targetPlayer.teleportTo(
                nether,
                targetFeet.getX() + 0.5D,
                targetFeet.getY(),
                targetFeet.getZ() + 0.5D,
                Relative.DELTA,
                0.0F,
                0.0F,
                false
        );
        makeFriends(server, requester, targetPlayer);

        DeadRecallSpaceUnitSavedData units = units(server);
        SpaceUnitRecord source = units.getOrCreateLodestone(helper.getLevel(), sourcePos, requester);
        discovery(server).markDiscovered(requester.getUUID(), source.id());
        double routeStability = crossDimensionStability(
                source.structure().resonance(),
                0.6D,
                SpaceUnitType.PLAYER,
                false
        );
        int baseCost = baseCost(routeStability);
        int expectedCost = AmethystCatalystDiscount.finalCost(baseCost, 4, 0);

        try {
            startTeleport(
                    requester,
                    SpaceUnitHandler.SOURCE_TYPE_LODESTONE,
                    source.id(),
                    targetPlayer.getUUID()
            );
            require(helper, sessions().containsKey(requester.getUUID()),
                    "Lodestone-to-player cross-dimension session did not start");
        } catch (Throwable failure) {
            cleanupFriendRoute(server, requester, targetPlayer, source);
            throw failure;
        }

        helper.runAtTickTime(330, () -> {
            try {
                require(helper, requester.level() == nether,
                        "Lodestone-to-player teleport did not reach the target Dimension");
                require(helper, countShards(requester) == INITIAL_SHARDS - expectedCost,
                        "Player target incorrectly contributed catalyst discount");
                helper.succeed();
            } finally {
                cleanupFriendRoute(server, requester, targetPlayer, source);
            }
        });
    }

    private static void placeLodestoneStructure(ServerLevel level, BlockPos lodestonePos, boolean catalysts) {
        level.setBlockAndUpdate(lodestonePos, Blocks.LODESTONE.defaultBlockState());
        level.setBlockAndUpdate(lodestonePos.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(lodestonePos.above(2), Blocks.AIR.defaultBlockState());
        for (BlockPos offset : SUPPORT_OFFSETS) {
            level.setBlockAndUpdate(lodestonePos.offset(offset), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        }
        for (BlockPos offset : CATALYST_OFFSETS) {
            level.setBlockAndUpdate(
                    lodestonePos.offset(offset),
                    catalysts ? Blocks.AMETHYST_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState()
            );
        }
    }

    private static void removeCatalysts(ServerLevel level, BlockPos lodestonePos) {
        for (BlockPos offset : CATALYST_OFFSETS) {
            level.setBlockAndUpdate(lodestonePos.offset(offset), Blocks.AIR.defaultBlockState());
        }
    }

    @SuppressWarnings("removal")
    private static ServerPlayer createPlayer(GameTestHelper helper, BlockPos feetPos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(feetPos.getX() + 0.5D, feetPos.getY(), feetPos.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    private static void prepareSurvivalPlayer(ServerPlayer player) {
        player.getAbilities().instabuild = false;
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COMPASS));
        player.getInventory().add(new ItemStack(Items.AMETHYST_SHARD, INITIAL_SHARDS));
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
    }

    private static void startTeleport(
            ServerPlayer player,
            String sourceType,
            UUID sourceId,
            UUID targetId) {
        SpaceUnitHandler.establishInterfaceContext(
                player,
                InteractionHand.MAIN_HAND,
                sourceType,
                sourceId
        ).orElseThrow(() -> new IllegalStateException("Could not establish compass interface context"));
        SpaceUnitHandler.startTeleport(player, sourceType, sourceId, targetId);
    }

    private static void placeSafeLanding(ServerLevel level, BlockPos feetPos) {
        level.setBlockAndUpdate(feetPos.below(), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        level.setBlockAndUpdate(feetPos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(feetPos.above(), Blocks.AIR.defaultBlockState());
    }

    private static int countShards(ServerPlayer player) {
        return player.getInventory().countItem(Items.AMETHYST_SHARD);
    }

    private static int lodestoneBaseCost(
            SpaceStructureSnapshot source,
            SpaceStructureSnapshot target
    ) {
        return baseCost(crossDimensionStability(
                source.resonance(),
                target.resonance(),
                SpaceUnitType.LODESTONE,
                false
        ));
    }

    private static double crossDimensionStability(
            double sourceStability,
            double targetStability,
            SpaceUnitType targetType,
            boolean playerSource
    ) {
        double stability = Math.min(sourceStability, targetStability) * 0.65D;
        stability *= switch (targetType) {
            case DEATH -> 0.72D;
            case PLAYER -> 0.65D;
            case TEMPORARY -> 0.85D;
            default -> 1.0D;
        };
        if (playerSource) {
            stability *= 0.85D;
        }
        return Math.max(0.0D, Math.min(1.0D, stability));
    }

    private static int baseCost(double routeStability) {
        return Math.max(2, 2 + (int) Math.ceil((1.0D - routeStability) * 4.0D));
    }

    private static void discover(
            MinecraftServer server,
            ServerPlayer player,
            SpaceUnitRecord... records
    ) {
        DeadRecallSpaceDiscoverySavedData discovery = discovery(server);
        for (SpaceUnitRecord record : records) {
            discovery.markDiscovered(player.getUUID(), record.id());
        }
    }

    private static void makeFriends(
            MinecraftServer server,
            ServerPlayer first,
            ServerPlayer second
    ) {
        DeadRecallFriendSavedData friends = server.overworld().getDataStorage()
                .computeIfAbsent(DeadRecallFriendSavedData.TYPE);
        friends.inviteOrAccept(first.getUUID(), second.getUUID());
        friends.inviteOrAccept(second.getUUID(), first.getUUID());
    }

    private static ServerLevel requireNether(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        if (nether == null) {
            throw helper.assertionException("Nether level was unavailable for catalyst GameTest");
        }
        return nether;
    }

    private static DeadRecallSpaceUnitSavedData units(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);
    }

    private static DeadRecallSpaceDiscoverySavedData discovery(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeadRecallSpaceDiscoverySavedData.TYPE);
    }

    private static Map<UUID, SpaceUnitRecord> unitRecords(DeadRecallSpaceUnitSavedData units) {
        return ((DeadRecallSpaceUnitSavedDataAccessor) (Object) units).deadrecall$getUnitsById();
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Object> sessions() {
        try {
            Field field = SpaceUnitHandler.class.getDeclaredField("teleportSessions");
            field.setAccessible(true);
            return (Map<UUID, Object>) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not inspect teleport sessions in GameTest", failure);
        }
    }

    private static void cleanup(
            MinecraftServer server,
            ServerPlayer player,
            SpaceUnitRecord... records
    ) {
        sessions().remove(player.getUUID());
        SpaceUnitHandler.clearInterfaceContext(player.getUUID());
        DeadRecallSpaceUnitSavedData units = units(server);
        DeadRecallSpaceDiscoverySavedData discovery = discovery(server);
        for (SpaceUnitRecord record : records) {
            discovery.removeDiscovered(player.getUUID(), record.id());
            if (record.isLodestoneAnchor()) {
                ServerLevel level = server.getLevel(record.dimension());
                units.disableLodestone(
                        record.dimension(),
                        record.pos(),
                        level == null ? 0L : level.getGameTime()
                );
            } else {
                unitRecords(units).remove(record.id());
                units.setDirty();
            }
        }
        if (!player.isRemoved()) {
            player.discard();
        }
    }

    private static void cleanupFriendRoute(
            MinecraftServer server,
            ServerPlayer requester,
            ServerPlayer target,
            SpaceUnitRecord source
    ) {
        server.overworld().getDataStorage()
                .computeIfAbsent(DeadRecallFriendSavedData.TYPE)
                .removeRelationship(requester.getUUID(), target.getUUID());
        cleanup(server, requester, source);
        sessions().remove(target.getUUID());
        SpaceUnitHandler.clearInterfaceContext(target.getUUID());
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
