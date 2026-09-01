package dev.totem.remnant.echo;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Covers the Remnant-owned renewable Echo Shard interaction. */
public final class EchoShardCrystallizationGameTest {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest(maxTicks = 20)
    public void amethystCrystallizesSculkIntoOneEchoShard(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.SCULK);
        BlockPos absoluteTarget = helper.absolutePos(TARGET);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AMETHYST_SHARD, 2));

        InteractionResult result = EchoShardCrystallization.interact(
                player, helper.getLevel(), InteractionHand.MAIN_HAND, hit(absoluteTarget));

        require(helper, result.consumesAction(), "Sculk crystallization did not consume the interaction");
        require(helper, helper.getLevel().getBlockState(absoluteTarget).is(Blocks.DEEPSLATE),
                "Crystallized Sculk did not leave Deepslate behind");
        require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.AMETHYST_SHARD)
                        && player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                "Sculk crystallization did not consume exactly one Amethyst Shard");
        require(helper, player.getInventory().countItem(Items.ECHO_SHARD) == 1,
                "Sculk crystallization did not grant exactly one Echo Shard");

        AABB area = new AABB(absoluteTarget).inflate(2.0D);
        require(helper, helper.getLevel().getEntitiesOfClass(
                        ExperienceOrb.class, area, ExperienceOrb::isAlive).isEmpty(),
                "Sculk crystallization also spawned experience");
        require(helper, helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class, area, ItemEntity::isAlive).isEmpty(),
                "Sculk crystallization dropped an extra item despite free inventory space");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void amethystDoesNotCrystallizeOtherSculkFamilyBlocks(GameTestHelper helper) {
        List<Block> excludedBlocks = List.of(
                Blocks.SCULK_CATALYST,
                Blocks.SCULK_SENSOR,
                Blocks.SCULK_SHRIEKER,
                Blocks.SCULK_VEIN
        );
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(Items.AMETHYST_SHARD, excludedBlocks.size()));

        for (int index = 0; index < excludedBlocks.size(); index++) {
            BlockPos relativePos = TARGET.offset(index, 0, 0);
            Block block = excludedBlocks.get(index);
            helper.setBlock(relativePos, block);
            BlockPos absolutePos = helper.absolutePos(relativePos);

            InteractionResult result = EchoShardCrystallization.interact(
                    player, helper.getLevel(), InteractionHand.MAIN_HAND, hit(absolutePos));
            require(helper, result == InteractionResult.PASS,
                    "Amethyst unexpectedly handled " + block.getName().getString());
            require(helper, helper.getLevel().getBlockState(absolutePos).is(block),
                    "Amethyst unexpectedly replaced " + block.getName().getString());
        }

        require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == excludedBlocks.size(),
                "Rejected Sculk-family blocks still consumed Amethyst Shards");
        require(helper, player.getInventory().countItem(Items.ECHO_SHARD) == 0,
                "Rejected Sculk-family blocks granted an Echo Shard");
        helper.succeed();
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
