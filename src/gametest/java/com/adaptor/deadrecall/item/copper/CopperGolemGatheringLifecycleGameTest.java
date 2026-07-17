package com.adaptor.deadrecall.item.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public final class CopperGolemGatheringLifecycleGameTest {
    private static final BlockPos GOLEM_POS = new BlockPos(3, 2, 3);
    private static final BlockPos HOME_POS = new BlockPos(2, 2, 3);
    private static final BlockPos FAR_TARGET_POS = new BlockPos(9, 2, 3);
    private static final BlockPos NEAR_TARGET_POS = new BlockPos(4, 2, 3);

    @GameTest(maxTicks = 500)
    public void autonomousGatheringScansPathsBreaksReturnsAndDeposits(GameTestHelper helper) {
        prepareWalkway(helper);
        Block copperChest = copperChest(helper);
        helper.setBlock(HOME_POS, copperChest);
        helper.setBlock(FAR_TARGET_POS, Blocks.STONE);

        ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        configureGathering(helper, golem, operator, FAR_TARGET_POS, new ItemStack(Items.IRON_PICKAXE), ItemStack.EMPTY);

        boolean[] observed = new boolean[4];
        for (int tick = 1; tick < 480; tick++) {
            helper.runAtTickTime(tick, () -> {
                CopperGolemActivity activity = CopperGolemData.activity(golem);
                observed[0] |= activity == CopperGolemActivity.SEARCHING;
                observed[1] |= activity == CopperGolemActivity.MOVING_TO_TARGET;
                observed[2] |= activity == CopperGolemActivity.WORKING;
                observed[3] |= activity == CopperGolemActivity.RETURNING_HOME;

                Container home = containerAt(helper, HOME_POS);
                ItemStack stored = CopperGolemWrenchHandler.getGatheringStorageStackForMenu(golem);
                if (helper.getLevel().getBlockState(helper.absolutePos(FAR_TARGET_POS)).isAir()
                        && stored.isEmpty()
                        && countItem(home, Items.COBBLESTONE) == 1) {
                    require(helper, observed[0], "Gathering lifecycle never entered SEARCHING");
                    require(helper, observed[1], "Gathering lifecycle never entered MOVING_TO_TARGET");
                    require(helper, observed[2], "Gathering lifecycle never entered WORKING");
                    require(helper, observed[3], "Gathering lifecycle never entered RETURNING_HOME");

                    ItemStack tool = CopperGolemWrenchHandler.getGatheringToolStackForMenu(golem);
                    require(helper, tool.is(Items.IRON_PICKAXE) && tool.getDamageValue() == 1,
                            "Gathering lifecycle did not damage the tool exactly once");
                    cleanup(golem, operator);
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(maxTicks = 500)
    public void homeRemovalDuringReturnPreservesStorageAndBlocks(GameTestHelper helper) {
        prepareWalkway(helper);
        helper.setBlock(HOME_POS, copperChest(helper));
        helper.setBlock(FAR_TARGET_POS, Blocks.STONE);

        ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        configureGathering(helper, golem, operator, FAR_TARGET_POS, new ItemStack(Items.IRON_PICKAXE), ItemStack.EMPTY);

        boolean[] homeRemoved = {false};
        for (int tick = 1; tick < 480; tick++) {
            helper.runAtTickTime(tick, () -> {
                CopperGolemActivity activity = CopperGolemData.activity(golem);
                if (!homeRemoved[0] && activity == CopperGolemActivity.RETURNING_HOME) {
                    ItemStack stored = CopperGolemWrenchHandler.getGatheringStorageStackForMenu(golem);
                    require(helper, stored.is(Items.COBBLESTONE) && stored.getCount() == 1,
                            "Return began without preserving the gathered stack");
                    helper.setBlock(HOME_POS, Blocks.AIR);
                    homeRemoved[0] = true;
                    return;
                }

                if (homeRemoved[0] && activity == CopperGolemActivity.BLOCKED_HOME_UNAVAILABLE) {
                    ItemStack stored = CopperGolemWrenchHandler.getGatheringStorageStackForMenu(golem);
                    require(helper, stored.is(Items.COBBLESTONE) && stored.getCount() == 1,
                            "Home removal changed or deleted gathering storage");
                    require(helper, helper.getLevel().getBlockState(helper.absolutePos(HOME_POS)).isAir(),
                            "Home removal fixture unexpectedly restored the container");
                    cleanup(golem, operator);
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(maxTicks = 180)
    public void finalToolDurabilityKeepsToolBrokenActivityAndStorage(GameTestHelper helper) {
        prepareWalkway(helper);
        helper.setBlock(HOME_POS, copperChest(helper));
        helper.setBlock(NEAR_TARGET_POS, Blocks.STONE);

        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        tool.setDamageValue(tool.getMaxDamage() - 1);
        ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        configureGathering(helper, golem, operator, NEAR_TARGET_POS, tool, ItemStack.EMPTY);

        int[] brokenTicks = {0};
        for (int tick = 1; tick < 170; tick++) {
            helper.runAtTickTime(tick, () -> {
                if (!helper.getLevel().getBlockState(helper.absolutePos(NEAR_TARGET_POS)).isAir()) {
                    return;
                }

                brokenTicks[0]++;
                CopperGolemActivity activity = CopperGolemData.activity(golem);
                require(helper, activity == CopperGolemActivity.BLOCKED_TOOL_BROKEN,
                        "Tool break activity was overwritten by " + activity.id());
                require(helper, CopperGolemWrenchHandler.getGatheringToolStackForMenu(golem).isEmpty(),
                        "Broken gathering tool remained in the tool slot");
                ItemStack stored = CopperGolemWrenchHandler.getGatheringStorageStackForMenu(golem);
                require(helper, stored.is(Items.COBBLESTONE) && stored.getCount() == 1,
                        "Tool break did not preserve the successfully gathered drop");

                if (brokenTicks[0] >= 30) {
                    cleanup(golem, operator);
                    helper.succeed();
                }
            });
        }
    }

    private static void prepareWalkway(GameTestHelper helper) {
        for (int x = 1; x <= 10; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
            }
        }
    }

    private static void configureGathering(
            GameTestHelper helper,
            CopperGolem golem,
            ServerPlayer operator,
            BlockPos relativeTarget,
            ItemStack tool,
            ItemStack storage
    ) {
        BlockPos home = helper.absolutePos(HOME_POS);
        BlockPos target = helper.absolutePos(relativeTarget);
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.GATHERING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, true);
        SortingBindingService.writeSourceContainer(tag, new CopperGolemWrenchHandler.Binding(
                helper.getLevel().dimension(),
                home
        ));
        tag.putString("deadrecall_gathering_area_dim", helper.getLevel().dimension().identifier().toString());
        writePos(tag, target,
                "deadrecall_gathering_corner_a_x",
                "deadrecall_gathering_corner_a_y",
                "deadrecall_gathering_corner_a_z");
        writePos(tag, target,
                "deadrecall_gathering_corner_b_x",
                "deadrecall_gathering_corner_b_y",
                "deadrecall_gathering_corner_b_z");
        CopperGolemData.writeStringList(
                tag,
                "deadrecall_gathering_manual_targets",
                List.of("minecraft:stone"),
                64
        );
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_tool_stack", tool);
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_storage_stack", storage);
        CopperGolemData.writeEntityTag(golem, tag);
        CopperGolemWrenchHandler.setFuelStackFromMenu(golem, new ItemStack(Items.COAL));
        invokeWrench("rememberLastOperator", new Class<?>[]{CopperGolem.class, ServerPlayer.class}, golem, operator);
    }

    private static void writePos(CompoundTag tag, BlockPos pos, String xKey, String yKey, String zKey) {
        tag.putInt(xKey, pos.getX());
        tag.putInt(yKey, pos.getY());
        tag.putInt(zKey, pos.getZ());
    }

    private static Block copperChest(GameTestHelper helper) {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_chest")
        );
        if (block == null || block == Blocks.AIR) {
            throw helper.assertionException("Missing minecraft:copper_chest block");
        }
        return block;
    }

    private static Container containerAt(GameTestHelper helper, BlockPos relativePos) {
        Object blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(relativePos));
        if (blockEntity instanceof Container container) {
            return container;
        }
        throw helper.assertionException("Missing container fixture at " + relativePos);
    }

    private static int countItem(Container container, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static CopperGolem createGolem(GameTestHelper helper, BlockPos relativePos) {
        Object entityType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_golem")
        );
        if (entityType == null) {
            throw helper.assertionException("Missing minecraft:copper_golem entity type");
        }

        try {
            for (Constructor<?> constructor : CopperGolem.class.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 2
                        || !parameterTypes[0].isInstance(entityType)
                        || !parameterTypes[1].isInstance(helper.getLevel())) {
                    continue;
                }

                constructor.setAccessible(true);
                CopperGolem golem = (CopperGolem) constructor.newInstance(entityType, helper.getLevel());
                BlockPos absolutePos = helper.absolutePos(relativePos);
                golem.snapTo(
                        absolutePos.getX() + 0.5D,
                        absolutePos.getY(),
                        absolutePos.getZ() + 0.5D,
                        0.0F,
                        0.0F
                );
                require(helper, helper.getLevel().addFreshEntity(golem),
                        "Could not add copper golem to the GameTest level");
                return golem;
            }
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not construct copper golem fixture", exception);
        }
        throw helper.assertionException("No compatible CopperGolem constructor was found");
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeWrench(String name, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = CopperGolemWrenchHandler.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(null, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not invoke CopperGolemWrenchHandler#" + name, exception);
        }
    }

    private static void cleanup(CopperGolem golem, ServerPlayer operator) {
        CopperGolemWrenchHandler.untrackCopperGolem(golem);
        golem.discard();
        operator.discard();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
