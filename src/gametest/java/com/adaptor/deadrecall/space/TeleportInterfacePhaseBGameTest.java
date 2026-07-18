package com.adaptor.deadrecall.space;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TeleportInterfacePhaseBGameTest {
    private static final BlockPos PLAYER_POS = new BlockPos(4, 20, 4);
    private static final BlockPos LODESTONE_POS = new BlockPos(14, 20, 4);
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
    public void bookSessionUsesSpecializedLodestonePreparationTime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = createPlayer(helper, PLAYER_POS);
        BlockPos lodestonePos = helper.absolutePos(LODESTONE_POS);
        placeLodestoneStructure(level, lodestonePos);
        SpaceUnitRecord lodestone = units(level.getServer()).getOrCreateLodestone(level, lodestonePos, player);
        discovery(level.getServer()).markDiscovered(player.getUUID(), lodestone.id());

        try {
            int compassTicks = startAndReadTotalTicks(player, Items.COMPASS, lodestone.id());
            sessions().remove(player.getUUID());
            SpaceUnitHandler.clearInterfaceContext(player.getUUID());

            int bookTicks = startAndReadTotalTicks(player, Items.BOOK, lodestone.id());
            int expectedBookTicks = Math.max(30, (int) Math.ceil(compassTicks * 0.80D));

            require(helper, bookTicks == expectedBookTicks,
                    "Book session did not use the Phase B preparation-time specialization: compass="
                            + compassTicks + ", book=" + bookTicks + ", expected=" + expectedBookTicks);
            helper.succeed();
        } finally {
            sessions().remove(player.getUUID());
            SpaceUnitHandler.clearInterfaceContext(player.getUUID());
            discovery(level.getServer()).removeDiscovered(player.getUUID(), lodestone.id());
            units(level.getServer()).disableLodestone(
                    lodestone.dimension(),
                    lodestone.pos(),
                    level.getGameTime()
            );
            if (!player.isRemoved()) {
                player.discard();
            }
        }
    }

    private static int startAndReadTotalTicks(ServerPlayer player, Item interfaceItem, UUID targetId) {
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(interfaceItem));
        SpaceUnitHandler.establishInterfaceContext(
                player,
                InteractionHand.MAIN_HAND,
                SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                player.getUUID()
        ).orElseThrow(() -> new IllegalStateException("Could not establish Phase B interface context"));
        SpaceUnitHandler.startTeleport(
                player,
                SpaceUnitHandler.SOURCE_TYPE_PLAYER,
                player.getUUID(),
                targetId
        );

        Object session = sessions().get(player.getUUID());
        if (session == null) {
            throw new IllegalStateException("Phase B test route did not create a teleport session");
        }
        try {
            Method totalTicks = session.getClass().getDeclaredMethod("totalTicks");
            totalTicks.setAccessible(true);
            return (int) totalTicks.invoke(session);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not inspect Phase B session preparation time", failure);
        }
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

    @SuppressWarnings("removal")
    private static ServerPlayer createPlayer(GameTestHelper helper, BlockPos relativePos) {
        BlockPos feetPos = helper.absolutePos(relativePos);
        helper.getLevel().setBlockAndUpdate(feetPos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(feetPos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(feetPos.above(), Blocks.AIR.defaultBlockState());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(feetPos.getX() + 0.5D, feetPos.getY(), feetPos.getZ() + 0.5D, 0.0F, 0.0F);
        player.getAbilities().instabuild = true;
        return player;
    }

    private static DeadRecallSpaceUnitSavedData units(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);
    }

    private static DeadRecallSpaceDiscoverySavedData discovery(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeadRecallSpaceDiscoverySavedData.TYPE);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Object> sessions() {
        try {
            Field field = SpaceUnitHandler.class.getDeclaredField("teleportSessions");
            field.setAccessible(true);
            return (Map<UUID, Object>) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not inspect teleport sessions in Phase B GameTest", failure);
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
