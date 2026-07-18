package com.adaptor.deadrecall.space;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TeleportInterfacePhaseAGameTest {
    private static final BlockPos PLAYER_POS = new BlockPos(4, 20, 4);
    private static final BlockPos TARGET_POS = new BlockPos(16, 20, 4);
    private static final BlockPos LODESTONE_POS = new BlockPos(6, 20, 6);
    private static final BlockPos UNREGISTERED_POS = new BlockPos(8, 20, 6);

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void fourSupportedItemsOpenPlayerSourceThroughFabricEvent(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, PLAYER_POS);
        try {
            List<ExpectedInterface> interfaces = List.of(
                    new ExpectedInterface(Items.COMPASS.getDefaultInstance(), TeleportInterfaceType.COMPASS, null),
                    new ExpectedInterface(
                            Items.RECOVERY_COMPASS.getDefaultInstance(),
                            TeleportInterfaceType.RECOVERY_COMPASS,
                            null
                    ),
                    new ExpectedInterface(Items.BOOK.getDefaultInstance(), TeleportInterfaceType.BOOK, null),
                    new ExpectedInterface(filledMap(37), TeleportInterfaceType.FILLED_MAP, new MapId(37))
            );

            for (ExpectedInterface expected : interfaces) {
                player.setItemSlot(EquipmentSlot.MAINHAND, expected.stack().copy());
                InteractionResult result = UseItemCallback.EVENT.invoker()
                        .interact(player, helper.getLevel(), InteractionHand.MAIN_HAND);
                require(helper, result.consumesAction(),
                        expected.type() + " did not consume the right-click-air interaction");

                TeleportInterfaceContext context = SpaceUnitHandler.currentInterfaceContext(player)
                        .orElseThrow(() -> helper.assertionException(
                                expected.type() + " did not establish a Server interface context"));
                require(helper, context.interfaceType() == expected.type(),
                        "Resolved the wrong interface type for " + expected.type());
                require(helper, context.matchesSource(
                                SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                                player.getUUID()),
                        expected.type() + " did not bind the PLAYER source");
                require(helper, java.util.Objects.equals(context.mapId(), expected.mapId()),
                        expected.type() + " stored an incorrect map ID");
                require(helper, context.interactionHand() == InteractionHand.MAIN_HAND,
                        expected.type() + " did not retain the initiating hand");
                SpaceUnitHandler.clearInterfaceContext(player.getUUID());
            }
            helper.succeed();
        } finally {
            cleanupPlayer(player);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void unsupportedItemsAndIncompleteMapsPassThrough(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, PLAYER_POS);
        try {
            List<ItemStack> unsupported = List.of(
                    Items.STICK.getDefaultInstance(),
                    Items.MAP.getDefaultInstance(),
                    Items.FILLED_MAP.getDefaultInstance()
            );

            for (ItemStack stack : unsupported) {
                player.setItemSlot(EquipmentSlot.MAINHAND, stack);
                InteractionResult result = UseItemCallback.EVENT.invoker()
                        .interact(player, helper.getLevel(), InteractionHand.MAIN_HAND);
                require(helper, result == InteractionResult.PASS,
                        stack.getItem() + " incorrectly consumed the right-click-air interaction");
                require(helper, SpaceUnitHandler.currentInterfaceContext(player).isEmpty(),
                        stack.getItem() + " incorrectly established an interface context");
            }
            helper.succeed();
        } finally {
            cleanupPlayer(player);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void supportedItemsPassNonLodestoneBlockInteraction(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, PLAYER_POS);
        ServerLevel level = helper.getLevel();
        BlockPos blockPos = helper.absolutePos(LODESTONE_POS);
        level.setBlockAndUpdate(blockPos, Blocks.CHEST.defaultBlockState());

        try {
            for (ItemStack stack : List.of(
                    Items.COMPASS.getDefaultInstance(),
                    Items.RECOVERY_COMPASS.getDefaultInstance(),
                    Items.BOOK.getDefaultInstance(),
                    filledMap(41))) {
                player.setItemSlot(EquipmentSlot.MAINHAND, stack);
                InteractionResult result = UseBlockCallback.EVENT.invoker().interact(
                        player,
                        level,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(blockPos), Direction.UP, blockPos, false)
                );
                require(helper, result == InteractionResult.PASS,
                        stack.getItem() + " consumed a non-lodestone block interaction");
                require(helper, SpaceUnitHandler.currentInterfaceContext(player).isEmpty(),
                        stack.getItem() + " opened the teleport interface before the block interaction resolved");
            }
            helper.succeed();
        } finally {
            cleanupPlayer(player);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void fourItemsOpenRegisteredLodestoneButBookCannotRegisterAnother(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, PLAYER_POS);
        ServerLevel level = helper.getLevel();
        BlockPos registeredPos = helper.absolutePos(LODESTONE_POS);
        BlockPos unregisteredPos = helper.absolutePos(UNREGISTERED_POS);
        level.setBlockAndUpdate(registeredPos, Blocks.LODESTONE.defaultBlockState());
        level.setBlockAndUpdate(unregisteredPos, Blocks.LODESTONE.defaultBlockState());
        DeadRecallSpaceUnitSavedData units = units(level.getServer());
        SpaceUnitRecord registered = units.getOrCreateLodestone(level, registeredPos, player);
        discovery(level.getServer()).markDiscovered(player.getUUID(), registered.id());

        try {
            for (ExpectedInterface expected : List.of(
                    new ExpectedInterface(Items.COMPASS.getDefaultInstance(), TeleportInterfaceType.COMPASS, null),
                    new ExpectedInterface(
                            Items.RECOVERY_COMPASS.getDefaultInstance(),
                            TeleportInterfaceType.RECOVERY_COMPASS,
                            null
                    ),
                    new ExpectedInterface(Items.BOOK.getDefaultInstance(), TeleportInterfaceType.BOOK, null),
                    new ExpectedInterface(filledMap(43), TeleportInterfaceType.FILLED_MAP, new MapId(43)))) {
                player.setItemSlot(EquipmentSlot.MAINHAND, expected.stack().copy());
                InteractionResult registeredResult = useLodestone(player, level, registeredPos);
                require(helper, registeredResult.consumesAction(),
                        expected.type() + " did not consume use on a registered lodestone");
                TeleportInterfaceContext context = SpaceUnitHandler.currentInterfaceContext(player)
                        .orElseThrow(() -> helper.assertionException(
                                expected.type() + " did not establish a registered-lodestone interface context"));
                require(helper, context.interfaceType() == expected.type(),
                        "Registered lodestone resolved " + expected.type() + " as the wrong interface");
                require(helper, context.matchesSource(SpaceUnitHandler.SOURCE_TYPE_LODESTONE, registered.id()),
                        expected.type() + " context did not retain the registered lodestone source");
                require(helper, java.util.Objects.equals(context.mapId(), expected.mapId()),
                        expected.type() + " context stored an incorrect map ID");
                SpaceUnitHandler.clearInterfaceContext(player.getUUID());
            }

            player.setItemSlot(EquipmentSlot.MAINHAND, Items.BOOK.getDefaultInstance());
            InteractionResult unregisteredResult = useLodestone(player, level, unregisteredPos);
            require(helper, unregisteredResult.consumesAction(),
                    "Book did not handle an unregistered lodestone with an explicit rejection");
            require(helper, units.getLodestone(level.dimension(), unregisteredPos).isEmpty(),
                    "Book incorrectly registered an unregistered lodestone");
            require(helper, SpaceUnitHandler.currentInterfaceContext(player).isEmpty(),
                    "Rejected book registration still created an interface context");
            helper.succeed();
        } finally {
            disableLodestone(level, registered);
            cleanupPlayer(player);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void nonCompassContextCannotMutateLodestoneManagement(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, PLAYER_POS);
        ServerLevel level = helper.getLevel();
        BlockPos lodestonePos = helper.absolutePos(LODESTONE_POS);
        level.setBlockAndUpdate(lodestonePos, Blocks.LODESTONE.defaultBlockState());
        SpaceUnitRecord lodestone = units(level.getServer()).getOrCreateLodestone(level, lodestonePos, player);
        discovery(level.getServer()).markDiscovered(player.getUUID(), lodestone.id());

        try {
            player.setItemSlot(EquipmentSlot.MAINHAND, Items.BOOK.getDefaultInstance());
            establish(player, SpaceUnitHandler.SOURCE_TYPE_LODESTONE, lodestone.id());
            SpaceUnitHandler.setLodestoneVisibility(
                    player,
                    SpaceUnitHandler.SOURCE_TYPE_LODESTONE,
                    lodestone.id(),
                    lodestone.id(),
                    SpaceUnitVisibility.PUBLIC.id()
            );
            require(helper, units(level.getServer()).get(lodestone.id()).orElseThrow().visibility()
                            == SpaceUnitVisibility.PRIVATE,
                    "Book context incorrectly received compass-only visibility management");

            player.setItemSlot(EquipmentSlot.MAINHAND, Items.COMPASS.getDefaultInstance());
            establish(player, SpaceUnitHandler.SOURCE_TYPE_LODESTONE, lodestone.id());
            SpaceUnitHandler.setLodestoneVisibility(
                    player,
                    SpaceUnitHandler.SOURCE_TYPE_LODESTONE,
                    lodestone.id(),
                    lodestone.id(),
                    SpaceUnitVisibility.PUBLIC.id()
            );
            require(helper, units(level.getServer()).get(lodestone.id()).orElseThrow().visibility()
                            == SpaceUnitVisibility.PUBLIC,
                    "Ordinary compass context lost its existing management capability");
            helper.succeed();
        } finally {
            disableLodestone(level, lodestone);
            cleanupPlayer(player);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void changingInitiatingItemCancelsSessionBeforePayment(GameTestHelper helper) {
        ServerPlayer requester = createPlayer(helper, PLAYER_POS);
        ServerPlayer target = createPlayer(helper, TARGET_POS);
        requester.getAbilities().instabuild = false;
        requester.getFoodData().setFoodLevel(20);
        requester.getFoodData().setSaturation(20.0F);
        requester.setItemSlot(EquipmentSlot.MAINHAND, Items.RECOVERY_COMPASS.getDefaultInstance());
        makeFriends(helper.getLevel().getServer(), requester, target);

        try {
            establish(requester, SpaceUnitHandler.SOURCE_TYPE_PLAYER, requester.getUUID());
            SpaceUnitHandler.startTeleport(
                    requester,
                    SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                    requester.getUUID(),
                    target.getUUID()
            );
            require(helper, sessions().containsKey(requester.getUUID()),
                    "Recovery compass did not start the baseline Phase A session");

            requester.setItemSlot(EquipmentSlot.MAINHAND, Items.COMPASS.getDefaultInstance());
            SpaceUnitHandler.tickTeleportSessions(helper.getLevel().getServer());

            require(helper, !sessions().containsKey(requester.getUUID()),
                    "Session survived replacement of its initiating interface type");
            require(helper, requester.getFoodData().getFoodLevel() == 20,
                    "Cancelled interface-change session deducted hunger");
            require(helper, requester.getFoodData().getSaturationLevel() == 20.0F,
                    "Cancelled interface-change session deducted saturation");
            helper.succeed();
        } finally {
            friends(helper.getLevel().getServer()).removeRelationship(
                    requester.getUUID(),
                    target.getUUID()
            );
            cleanupPlayer(requester);
            cleanupPlayer(target);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void movingItemToOtherHandInvalidatesContext(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, PLAYER_POS);
        player.setItemSlot(EquipmentSlot.MAINHAND, Items.BOOK.getDefaultInstance());

        try {
            establish(player, SpaceUnitHandler.SOURCE_TYPE_PLAYER, player.getUUID());
            player.setItemSlot(EquipmentSlot.OFFHAND, Items.BOOK.getDefaultInstance());
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

            require(helper, SpaceUnitHandler.currentInterfaceContext(player).isEmpty(),
                    "Context followed the item to a different interaction hand");
            helper.succeed();
        } finally {
            cleanupPlayer(player);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 100)
    public void changingFilledMapIdInvalidatesContext(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, PLAYER_POS);
        player.setItemSlot(EquipmentSlot.MAINHAND, filledMap(51));

        try {
            establish(player, SpaceUnitHandler.SOURCE_TYPE_PLAYER, player.getUUID());
            player.setItemSlot(EquipmentSlot.MAINHAND, filledMap(52));

            require(helper, SpaceUnitHandler.currentInterfaceContext(player).isEmpty(),
                    "Context survived replacement with a different filled-map ID");
            helper.succeed();
        } finally {
            cleanupPlayer(player);
        }
    }

    private static void establish(ServerPlayer player, String sourceType, UUID sourceId) {
        SpaceUnitHandler.establishInterfaceContext(
                player,
                InteractionHand.MAIN_HAND,
                sourceType,
                sourceId
        ).orElseThrow(() -> new IllegalStateException("Could not establish test interface context"));
    }

    private static InteractionResult useLodestone(
            ServerPlayer player,
            ServerLevel level,
            BlockPos pos) {
        return UseBlockCallback.EVENT.invoker().interact(
                player,
                level,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
        );
    }

    private static ItemStack filledMap(int id) {
        ItemStack stack = Items.FILLED_MAP.getDefaultInstance();
        stack.set(DataComponents.MAP_ID, new MapId(id));
        return stack;
    }

    private static ServerPlayer createPlayer(GameTestHelper helper, BlockPos relativePos) {
        BlockPos feetPos = helper.absolutePos(relativePos);
        helper.getLevel().setBlockAndUpdate(feetPos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(feetPos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(feetPos.above(), Blocks.AIR.defaultBlockState());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(feetPos.getX() + 0.5D, feetPos.getY(), feetPos.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    private static void makeFriends(
            MinecraftServer server,
            ServerPlayer first,
            ServerPlayer second) {
        DeadRecallFriendSavedData data = friends(server);
        data.inviteOrAccept(first.getUUID(), second.getUUID());
        data.inviteOrAccept(second.getUUID(), first.getUUID());
    }

    private static DeadRecallSpaceUnitSavedData units(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);
    }

    private static DeadRecallSpaceDiscoverySavedData discovery(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeadRecallSpaceDiscoverySavedData.TYPE);
    }

    private static DeadRecallFriendSavedData friends(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeadRecallFriendSavedData.TYPE);
    }

    private static void disableLodestone(ServerLevel level, SpaceUnitRecord lodestone) {
        units(level.getServer()).disableLodestone(
                lodestone.dimension(),
                lodestone.pos(),
                level.getGameTime()
        );
    }

    private static void cleanupPlayer(ServerPlayer player) {
        sessions().remove(player.getUUID());
        SpaceUnitHandler.clearInterfaceContext(player.getUUID());
        if (!player.isRemoved()) {
            player.discard();
        }
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

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    private record ExpectedInterface(
            ItemStack stack,
            TeleportInterfaceType type,
            MapId mapId) {
    }
}
