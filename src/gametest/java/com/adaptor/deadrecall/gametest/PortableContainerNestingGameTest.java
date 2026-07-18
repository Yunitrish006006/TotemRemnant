package com.adaptor.deadrecall.gametest;

import com.adaptor.deadrecall.inventory.PortableContainerPolicy;
import com.adaptor.deadrecall.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public final class PortableContainerNestingGameTest {
    private static final BlockPos SHULKER_POS = new BlockPos(2, 1, 2);
    private static final BlockPos HOPPER_POS = SHULKER_POS.above();

    @GameTest(maxTicks = 20)
    public void runtimePolicyRejectsBothNestingDirections(GameTestHelper helper) {
        ItemStack normalBackpack = new ItemStack(ModItems.BACKPACK_BASIC);
        ItemStack deathBackpack = new ItemStack(ModItems.DEATH_BACKPACK);

        require(helper, !normalBackpack.getItem().canFitInsideContainerItems(),
                "Normal backpack did not opt out of vanilla container items");
        require(helper, !deathBackpack.getItem().canFitInsideContainerItems(),
                "Death backpack did not opt out of vanilla container items");
        require(helper, !PortableContainerPolicy.mayInsertIntoPortableContainer(normalBackpack),
                "Portable-container policy accepted a normal backpack");
        require(helper, !PortableContainerPolicy.mayInsertIntoPortableContainer(deathBackpack),
                "Portable-container policy accepted a death backpack");

        assertRestrictedInsideBackpack(helper, new ItemStack(Items.BUNDLE), "minecraft:bundle");
        for (String path : shulkerBoxPaths()) {
            assertRestrictedInsideBackpack(helper, new ItemStack(vanillaItem(helper, path)), "minecraft:" + path);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void shulkerMenuAndEverySidedAutomationFaceRejectBackpacks(GameTestHelper helper) {
        helper.setBlock(SHULKER_POS, Blocks.SHULKER_BOX);
        ShulkerBoxBlockEntity shulker = shulker(helper);
        ShulkerBoxSlot slot = new ShulkerBoxSlot(shulker, 0, 0, 0);

        ItemStack normalBackpack = new ItemStack(ModItems.BACKPACK_BASIC);
        ItemStack deathBackpack = new ItemStack(ModItems.DEATH_BACKPACK);

        require(helper, !slot.mayPlace(normalBackpack), "Shulker menu accepted a normal backpack");
        require(helper, !slot.mayPlace(deathBackpack), "Shulker menu accepted a death backpack");
        for (Direction direction : Direction.values()) {
            require(helper, !shulker.canPlaceItemThroughFace(0, normalBackpack, direction),
                    "Sided automation accepted a normal backpack from " + direction);
            require(helper, !shulker.canPlaceItemThroughFace(0, deathBackpack, direction),
                    "Sided automation accepted a death backpack from " + direction);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void hopperCannotPushBackpackIntoShulker(GameTestHelper helper) {
        placeHopperOverShulker(helper);
        HopperBlockEntity hopper = hopper(helper);
        ShulkerBoxBlockEntity shulker = shulker(helper);
        hopper.setItem(0, new ItemStack(ModItems.BACKPACK_NETHERITE));

        helper.runAtTickTime(20, () -> {
            require(helper, hopper.getItem(0).is(ModItems.BACKPACK_NETHERITE),
                    "Hopper removed the backpack while targeting a Shulker Box");
            require(helper, isEmpty(shulker), "Hopper nested the backpack inside a Shulker Box");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void hopperStillPushesOrdinaryItemsIntoShulker(GameTestHelper helper) {
        placeHopperOverShulker(helper);
        HopperBlockEntity hopper = hopper(helper);
        ShulkerBoxBlockEntity shulker = shulker(helper);
        hopper.setItem(0, new ItemStack(Items.DIRT, 3));

        helper.runAtTickTime(20, () -> {
            require(helper, hopper.getItem(0).isEmpty(), "Control hopper did not move ordinary items");
            require(helper, shulker.getItem(0).is(Items.DIRT) && shulker.getItem(0).getCount() == 3,
                    "Control Shulker Box did not receive ordinary items");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void hopperMinecartChainCannotNestBackpackInShulker(GameTestHelper helper) {
        placeHopperOverShulker(helper);
        HopperBlockEntity hopper = hopper(helper);
        ShulkerBoxBlockEntity shulker = shulker(helper);
        MinecartHopper minecart = hopperMinecart(helper);
        minecart.setItem(0, new ItemStack(ModItems.BACKPACK_NETHERITE));

        helper.runAtTickTime(36, () -> {
            int sourceCount = countItem(minecart, ModItems.BACKPACK_NETHERITE);
            int intermediateCount = countItem(hopper, ModItems.BACKPACK_NETHERITE);
            require(helper, sourceCount + intermediateCount == 1,
                    "Hopper Minecart chain duplicated or deleted the rejected backpack");
            require(helper, isEmpty(shulker),
                    "Hopper Minecart chain nested the backpack inside a Shulker Box");
            minecart.discard();
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void hopperMinecartChainStillMovesOrdinaryItemIntoShulker(GameTestHelper helper) {
        placeHopperOverShulker(helper);
        HopperBlockEntity hopper = hopper(helper);
        ShulkerBoxBlockEntity shulker = shulker(helper);
        MinecartHopper minecart = hopperMinecart(helper);
        minecart.setItem(0, new ItemStack(Items.DIRT));

        helper.runAtTickTime(36, () -> {
            require(helper, countItem(minecart, Items.DIRT) == 0,
                    "Control Hopper Minecart retained the ordinary item");
            require(helper, countItem(hopper, Items.DIRT) == 0,
                    "Control Hopper retained the ordinary item from the Minecart");
            require(helper, shulker.getItem(0).is(Items.DIRT) && shulker.getItem(0).getCount() == 1,
                    "Control Shulker Box did not receive the Hopper Minecart item exactly once");
            minecart.discard();
            helper.succeed();
        });
    }

    private static void assertRestrictedInsideBackpack(GameTestHelper helper, ItemStack stack, String id) {
        require(helper, PortableContainerPolicy.isRestrictedPortableContainer(stack),
                id + " was not classified as a portable container");
        require(helper, !PortableContainerPolicy.mayInsertIntoBackpack(stack),
                id + " was accepted inside a DeadRecall backpack");
    }

    private static String[] shulkerBoxPaths() {
        return new String[]{
                "shulker_box",
                "white_shulker_box",
                "orange_shulker_box",
                "magenta_shulker_box",
                "light_blue_shulker_box",
                "yellow_shulker_box",
                "lime_shulker_box",
                "pink_shulker_box",
                "gray_shulker_box",
                "light_gray_shulker_box",
                "cyan_shulker_box",
                "purple_shulker_box",
                "blue_shulker_box",
                "brown_shulker_box",
                "green_shulker_box",
                "red_shulker_box",
                "black_shulker_box"
        };
    }

    private static Item vanillaItem(GameTestHelper helper, String path) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", path));
        if (item == null) {
            throw helper.assertionException("Missing vanilla item minecraft:" + path);
        }
        return item;
    }

    private static void placeHopperOverShulker(GameTestHelper helper) {
        helper.setBlock(SHULKER_POS, Blocks.SHULKER_BOX);
        helper.setBlock(
                HOPPER_POS,
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN)
        );
    }

    private static HopperBlockEntity hopper(GameTestHelper helper) {
        Object blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(HOPPER_POS));
        if (blockEntity instanceof HopperBlockEntity hopper) {
            return hopper;
        }
        throw helper.assertionException("Missing HopperBlockEntity fixture");
    }

    private static MinecartHopper hopperMinecart(GameTestHelper helper) {
        MinecartHopper minecart = new MinecartHopper(EntityTypes.HOPPER_MINECART, helper.getLevel());
        BlockPos aboveHopper = helper.absolutePos(HOPPER_POS.above());
        minecart.snapTo(
                aboveHopper.getX() + 0.5D,
                aboveHopper.getY(),
                aboveHopper.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        require(helper, helper.getLevel().addFreshEntity(minecart),
                "Could not add Hopper Minecart fixture");
        return minecart;
    }

    private static ShulkerBoxBlockEntity shulker(GameTestHelper helper) {
        Object blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(SHULKER_POS));
        if (blockEntity instanceof ShulkerBoxBlockEntity shulker) {
            return shulker;
        }
        throw helper.assertionException("Missing ShulkerBoxBlockEntity fixture");
    }

    private static boolean isEmpty(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static int countItem(Container container, Item item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
