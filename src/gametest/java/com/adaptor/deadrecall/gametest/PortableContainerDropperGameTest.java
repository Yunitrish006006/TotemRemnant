package com.adaptor.deadrecall.gametest;

import com.adaptor.deadrecall.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public final class PortableContainerDropperGameTest {
    private static final BlockPos SHULKER_POS = new BlockPos(2, 1, 2);
    private static final BlockPos DROPPER_POS = SHULKER_POS.above();
    private static final BlockPos POWER_POS = DROPPER_POS.east();

    @GameTest(maxTicks = 40)
    public void dropperCannotInsertBackpackIntoShulker(GameTestHelper helper) {
        placeFixture(helper);
        DispenserBlockEntity dropper = dropper(helper);
        ShulkerBoxBlockEntity shulker = shulker(helper);
        dropper.setItem(0, new ItemStack(ModItems.BACKPACK_ADVANCED));
        helper.setBlock(POWER_POS, Blocks.REDSTONE_BLOCK);

        helper.runAtTickTime(12, () -> {
            require(helper, dropper.getItem(0).is(ModItems.BACKPACK_ADVANCED),
                    "Dropper removed the backpack while targeting a Shulker Box");
            require(helper, isEmpty(shulker), "Dropper nested the backpack inside a Shulker Box");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void dropperStillInsertsOrdinaryItemIntoShulker(GameTestHelper helper) {
        placeFixture(helper);
        DispenserBlockEntity dropper = dropper(helper);
        ShulkerBoxBlockEntity shulker = shulker(helper);
        dropper.setItem(0, new ItemStack(Items.DIRT, 2));
        helper.setBlock(POWER_POS, Blocks.REDSTONE_BLOCK);

        helper.runAtTickTime(12, () -> {
            require(helper, dropper.getItem(0).is(Items.DIRT) && dropper.getItem(0).getCount() == 1,
                    "Control Dropper did not transfer exactly one ordinary item");
            require(helper, shulker.getItem(0).is(Items.DIRT) && shulker.getItem(0).getCount() == 1,
                    "Control Shulker Box did not receive exactly one ordinary item");
            helper.succeed();
        });
    }

    private static void placeFixture(GameTestHelper helper) {
        helper.setBlock(SHULKER_POS, Blocks.SHULKER_BOX);
        helper.setBlock(
                DROPPER_POS,
                Blocks.DROPPER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.DOWN)
        );
    }

    private static DispenserBlockEntity dropper(GameTestHelper helper) {
        Object blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(DROPPER_POS));
        if (blockEntity instanceof DispenserBlockEntity dropper) {
            return dropper;
        }
        throw helper.assertionException("Missing Dropper block entity fixture");
    }

    private static ShulkerBoxBlockEntity shulker(GameTestHelper helper) {
        Object blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(SHULKER_POS));
        if (blockEntity instanceof ShulkerBoxBlockEntity shulker) {
            return shulker;
        }
        throw helper.assertionException("Missing Shulker Box fixture");
    }

    private static boolean isEmpty(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
