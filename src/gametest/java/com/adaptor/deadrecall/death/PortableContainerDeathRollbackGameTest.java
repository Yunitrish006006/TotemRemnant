package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.item.BackpackItemHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class PortableContainerDeathRollbackGameTest {
    private static final BlockPos CAPTURE_POS = new BlockPos(2, 2, 2);

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void restrictedCursorContainerDropsExactlyOnceWhenCaptureRollsBack(GameTestHelper helper) {
        helper.setBlock(CAPTURE_POS.below(), Blocks.STONE);
        BlockPos absolutePos = helper.absolutePos(CAPTURE_POS);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(
                absolutePos.getX() + 0.5,
                absolutePos.getY(),
                absolutePos.getZ() + 0.5,
                0.0F,
                0.0F
        );
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        player.containerMenu.setCarried(new ItemStack(Items.BUNDLE));

        DeathBackpackCaptureService.forceFailureForTesting(
                player.getUUID(),
                DeathBackpackCaptureService.CaptureFailurePoint.AFTER_ENTITY_ADD
        );

        try {
            boolean captured = DeathBackpackCaptureService.captureBeforeVanillaDrop(player, helper.getLevel());
            require(helper, !captured, "Forced failure unexpectedly committed the death capture");
            require(helper, player.containerMenu.getCarried().isEmpty(),
                    "Restricted cursor Bundle remained attached to the menu after independent drop");
            require(helper, player.getInventory().getItem(0).is(Items.DIAMOND)
                            && player.getInventory().getItem(0).getCount() == 5,
                    "Capturable inventory stack was not restored after rollback");

            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(absolutePos).inflate(4.0),
                    ItemEntity::isAlive
            );
            long bundleDrops = drops.stream()
                    .filter(entity -> entity.getItem().is(Items.BUNDLE))
                    .count();
            require(helper, bundleDrops == 1,
                    "Restricted cursor Bundle was duplicated or lost during rollback; count=" + bundleDrops);
            require(helper, drops.stream().noneMatch(entity -> BackpackItemHelper.isDeathBackpackItem(entity.getItem())),
                    "Incomplete death backpack remained after rollback");
            helper.succeed();
        } finally {
            DeathBackpackCaptureService.clearForcedFailureForTesting(player.getUUID());
            player.discard();
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
