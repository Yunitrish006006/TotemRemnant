package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public final class CopperGolemRegressionGameTest {
    private static final BlockPos GOLEM_POS = new BlockPos(2, 2, 2);
    private static final Component NAMED_DIAMOND = Component.literal("Copper golem component regression");

    @GameTest(maxTicks = 40)
    public void modeSwitchMatrixRejectsDirtyStatesAndResetsSuccessfulSwitch(GameTestHelper helper) {
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        ServerPlayer player = createPlayerNear(helper, GOLEM_POS.offset(1, 0, 0));
        try {
            CopperGolemData.migrate(golem);
            invoke("setTransportEnabled", new Class<?>[]{CopperGolem.class, boolean.class}, golem, false);
            require(helper, canSwitchMode(player, golem, CopperGolemMode.GATHERING),
                    "Clean stopped sorting golem could not switch to gathering");

            golem.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
            require(helper, !canSwitchMode(player, golem, CopperGolemMode.GATHERING),
                    "Sorting golem switched modes while carrying cargo");
            golem.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            tag.putString("deadrecall_source_container_dim", Level.OVERWORLD.identifier().toString());
            tag.putInt("deadrecall_source_container_x", 4);
            tag.putInt("deadrecall_source_container_y", 2);
            tag.putInt("deadrecall_source_container_z", 4);
            tag.putInt("deadrecall_source_slot", 0);
            CopperGolemData.writeEntityTag(golem, tag);
            require(helper, !canSwitchMode(player, golem, CopperGolemMode.GATHERING),
                    "Sorting golem switched modes with a pending source transaction");

            tag = CopperGolemData.readEntityTag(golem);
            tag.remove("deadrecall_source_container_dim");
            tag.remove("deadrecall_source_container_x");
            tag.remove("deadrecall_source_container_y");
            tag.remove("deadrecall_source_container_z");
            tag.remove("deadrecall_source_slot");
            tag.putString("deadrecall_activity", CopperGolemActivity.WORKING.id());
            tag.putInt("deadrecall_tried_destinations", 1);
            tag.putLong("deadrecall_gathering_scan_index", 23L);
            CopperGolemData.writeEntityTag(golem, tag);

            int revisionBeforeGathering = CopperGolemData.revision(golem);
            setMode(golem, CopperGolemMode.GATHERING);
            CompoundTag gatheringTag = CopperGolemData.readEntityTag(golem);
            require(helper, CopperGolemData.mode(golem) == CopperGolemMode.GATHERING,
                    "Successful mode switch did not persist gathering mode");
            require(helper, CopperGolemData.revision(golem) > revisionBeforeGathering,
                    "Successful mode switch did not advance the server revision");
            require(helper, !gatheringTag.contains("deadrecall_activity")
                            && !gatheringTag.contains("deadrecall_tried_destinations")
                            && !gatheringTag.contains("deadrecall_gathering_scan_index"),
                    "Successful mode switch did not reset activity and scanner state");

            CopperGolemWrenchHandler.setGatheringToolStackFromMenu(golem, new ItemStack(Items.IRON_PICKAXE));
            require(helper, !canSwitchMode(player, golem, CopperGolemMode.SORTING),
                    "Gathering golem switched modes while its tool slot was occupied");
            CopperGolemWrenchHandler.setGatheringToolStackFromMenu(golem, ItemStack.EMPTY);

            CopperGolemWrenchHandler.setGatheringStorageStackFromMenu(golem, new ItemStack(Items.COBBLESTONE, 3));
            require(helper, !canSwitchMode(player, golem, CopperGolemMode.SORTING),
                    "Gathering golem switched modes while storage was occupied");
            CopperGolemWrenchHandler.setGatheringStorageStackFromMenu(golem, ItemStack.EMPTY);

            tag = CopperGolemData.readEntityTag(golem);
            tag.putInt("deadrecall_gathering_target_x", 6);
            tag.putInt("deadrecall_gathering_target_y", 2);
            tag.putInt("deadrecall_gathering_target_z", 6);
            CopperGolemData.writeEntityTag(golem, tag);
            require(helper, !canSwitchMode(player, golem, CopperGolemMode.SORTING),
                    "Gathering golem switched modes while a work target was active");

            tag = CopperGolemData.readEntityTag(golem);
            tag.remove("deadrecall_gathering_target_x");
            tag.remove("deadrecall_gathering_target_y");
            tag.remove("deadrecall_gathering_target_z");
            CopperGolemData.writeEntityTag(golem, tag);
            require(helper, canSwitchMode(player, golem, CopperGolemMode.SORTING),
                    "Clean stopped gathering golem could not switch to sorting");
            setMode(golem, CopperGolemMode.SORTING);
            require(helper, CopperGolemData.mode(golem) == CopperGolemMode.SORTING,
                    "Successful return to sorting mode was not persisted");
            helper.succeed();
        } finally {
            player.discard();
            golem.discard();
        }
    }

    @GameTest(maxTicks = 30)
    public void gatheringStoragePreservesComponentsAndRejectsOverflowOrMixedDrops(GameTestHelper helper) {
        ItemStack named = new ItemStack(Items.DIAMOND, 8);
        named.set(DataComponents.CUSTOM_NAME, NAMED_DIAMOND);
        ItemStack same = named.copyWithCount(8);

        require(helper, canGatherDrops(ItemStack.EMPTY, List.of(named, same)),
                "Two compatible component-identical drops totaling 16 were rejected");
        ItemStack merged = addGatheringDrops(ItemStack.EMPTY, List.of(named, same));
        require(helper, merged.is(Items.DIAMOND) && merged.getCount() == 16,
                "Compatible gathering drops were not merged to exactly 16");
        require(helper, NAMED_DIAMOND.equals(merged.get(DataComponents.CUSTOM_NAME)),
                "Gathering storage lost the custom-name component");

        ItemStack differentlyNamed = new ItemStack(Items.DIAMOND, 1);
        differentlyNamed.set(DataComponents.CUSTOM_NAME, Component.literal("Different component"));
        require(helper, !canGatherDrops(named, List.of(differentlyNamed)),
                "Storage accepted the same item with different components");
        require(helper, !canGatherDrops(ItemStack.EMPTY, List.of(new ItemStack(Items.DIAMOND, 17))),
                "Storage accepted a single drop larger than 16");
        require(helper, !canGatherDrops(ItemStack.EMPTY, List.of(
                        new ItemStack(Items.DIAMOND, 1),
                        new ItemStack(Items.EMERALD, 1))),
                "Storage accepted mixed item types in one gathering transaction");

        CopperGolem golem = createGolem(helper, GOLEM_POS);
        try {
            ItemStack oversized = named.copyWithCount(32);
            CopperGolemWrenchHandler.setGatheringStorageStackFromMenu(golem, oversized);
            ItemStack stored = CopperGolemWrenchHandler.getGatheringStorageStackForMenu(golem);
            require(helper, stored.getCount() == CopperGolemWrenchHandler.transportStorageMaxStackSize(),
                    "Menu storage write did not clamp to the authoritative 16-item limit");
            require(helper, NAMED_DIAMOND.equals(stored.get(DataComponents.CUSTOM_NAME)),
                    "Menu storage clamp discarded item components");
            helper.succeed();
        } finally {
            golem.discard();
        }
    }

    @GameTest(maxTicks = 30)
    public void gatheringAreaLimitsAndCrossDimensionResetAreServerAuthoritative(GameTestHelper helper) {
        require(helper, areaWithinLimits(new BlockPos(0, 0, 0), new BlockPos(63, 63, 63)),
                "Maximum 64x64x64 gathering area was rejected");
        require(helper, !areaWithinLimits(new BlockPos(0, 0, 0), new BlockPos(64, 0, 0)),
                "Gathering area accepted an axis length of 65");
        require(helper, !areaWithinLimits(new BlockPos(0, 0, 0), new BlockPos(63, 63, 64)),
                "Gathering area accepted a volume above 262144 blocks");

        CopperGolem golem = createGolem(helper, GOLEM_POS);
        try {
            boolean first = setGatheringCorner(golem, helper.getLevel(), new BlockPos(1, 2, 3), false);
            require(helper, first, "Overworld gathering corner A was rejected");

            ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
            require(helper, nether != null, "GameTest server did not load the Nether dimension");
            boolean second = setGatheringCorner(golem, nether, new BlockPos(7, 8, 9), true);
            require(helper, second, "Nether gathering corner B was rejected after dimension change");

            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            require(helper, Level.NETHER.identifier().toString().equals(
                            tag.getStringOr("deadrecall_gathering_area_dim", "")),
                    "Cross-dimension edit retained the stale Overworld area dimension");
            require(helper, !tag.contains("deadrecall_gathering_corner_a_x")
                            && !tag.contains("deadrecall_gathering_corner_a_y")
                            && !tag.contains("deadrecall_gathering_corner_a_z"),
                    "Cross-dimension edit retained the stale corner A");
            require(helper, tag.getIntOr("deadrecall_gathering_corner_b_x", Integer.MIN_VALUE) == 7
                            && tag.getIntOr("deadrecall_gathering_corner_b_y", Integer.MIN_VALUE) == 8
                            && tag.getIntOr("deadrecall_gathering_corner_b_z", Integer.MIN_VALUE) == 9,
                    "Cross-dimension edit did not preserve the new corner B");
            helper.succeed();
        } finally {
            golem.discard();
        }
    }

    @GameTest(maxTicks = 30)
    public void manualGatheringRuleOverridesCachedLlmDenial(GameTestHelper helper) {
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        try {
            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            tag.putString("deadrecall_llm_api_url", "http://127.0.0.1:1");
            tag.putString("deadrecall_llm_api_key", "test-key");
            tag.putString("deadrecall_llm_model", "test-model");
            tag.putBoolean("deadrecall_gathering_llm_enabled", true);
            tag.putString("deadrecall_gathering_llm_prompt", "deny stone");
            tag.putInt("deadrecall_gathering_llm_prompt_revision", 1);
            CopperGolemData.writeStringList(
                    tag,
                    "deadrecall_gathering_llm_denied_block_ids",
                    List.of("minecraft:stone"),
                    128
            );
            CopperGolemData.writeEntityTag(golem, tag);

            BlockState stone = Blocks.STONE.defaultBlockState();
            ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
            List<ItemStack> drops = List.of(new ItemStack(Items.COBBLESTONE));
            require(helper, !isGatheringBlockAllowed(golem, helper.getLevel(), stone, tool, drops, List.of()),
                    "Cached LLM denial did not reject stone without a manual rule");
            require(helper, isGatheringBlockAllowed(
                            golem,
                            helper.getLevel(),
                            stone,
                            tool,
                            drops,
                            List.of("minecraft:stone")),
                    "Manual gathering rule did not override a cached LLM denial");
            helper.succeed();
        } finally {
            golem.discard();
        }
    }

    @GameTest(maxTicks = 30)
    public void menuAuthorityRejectsUnboundWrongIdFarAndRunningSlotEdits(GameTestHelper helper) {
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        ServerPlayer player = createPlayerNear(helper, GOLEM_POS.offset(1, 0, 0));
        try {
            ItemStack wrench = new ItemStack(ModItems.COPPER_WRENCH);
            player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            require(helper, !CopperGolemWrenchHandler.canUseMenu(player, golem.getUUID(), golem),
                    "Unbound wrench was accepted for copper golem management");

            setSelectedGolem(wrench, golem.getUUID());
            require(helper, CopperGolemWrenchHandler.canUseMenu(player, golem.getUUID(), golem),
                    "Correctly bound nearby wrench was rejected");
            require(helper, !CopperGolemWrenchHandler.canUseMenu(player, UUID.randomUUID(), golem),
                    "Forged golem UUID was accepted by menu authority");

            player.snapTo(golem.getX() + 100.0D, golem.getY(), golem.getZ(), 0.0F, 0.0F);
            require(helper, !CopperGolemWrenchHandler.canUseMenu(player, golem.getUUID(), golem),
                    "Out-of-range player was accepted by menu authority");
            player.snapTo(golem.getX() + 1.0D, golem.getY(), golem.getZ(), 0.0F, 0.0F);

            invoke("setTransportEnabled", new Class<?>[]{CopperGolem.class, boolean.class}, golem, false);
            setMode(golem, CopperGolemMode.GATHERING);
            require(helper, CopperGolemWrenchHandler.canEditGatheringSlots(golem),
                    "Stopped gathering golem did not permit authoritative slot editing");
            invoke("setTransportEnabled", new Class<?>[]{CopperGolem.class, boolean.class}, golem, true);
            require(helper, !CopperGolemWrenchHandler.canEditGatheringSlots(golem),
                    "Running gathering golem permitted slot editing");
            helper.succeed();
        } finally {
            player.discard();
            golem.discard();
        }
    }

    private static CopperGolem createGolem(GameTestHelper helper, BlockPos relativePos) {
        Object entityType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_golem"));
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

    private static ServerPlayer createPlayerNear(GameTestHelper helper, BlockPos relativePos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absolutePos = helper.absolutePos(relativePos);
        player.snapTo(
                absolutePos.getX() + 0.5D,
                absolutePos.getY(),
                absolutePos.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        return player;
    }

    private static boolean canSwitchMode(ServerPlayer player, CopperGolem golem, CopperGolemMode mode) {
        return invoke("canSwitchMode",
                new Class<?>[]{ServerPlayer.class, CopperGolem.class, CopperGolemMode.class},
                player, golem, mode);
    }

    private static void setMode(CopperGolem golem, CopperGolemMode mode) {
        invoke("setMode", new Class<?>[]{CopperGolem.class, CopperGolemMode.class}, golem, mode);
    }

    private static boolean canGatherDrops(ItemStack storage, List<ItemStack> drops) {
        return invoke("canGatherDropsIntoStorage", new Class<?>[]{ItemStack.class, List.class}, storage, drops);
    }

    private static ItemStack addGatheringDrops(ItemStack storage, List<ItemStack> drops) {
        return invoke("addDropsToGatheringStorage", new Class<?>[]{ItemStack.class, List.class}, storage, drops);
    }

    private static boolean areaWithinLimits(BlockPos first, BlockPos second) {
        return invoke("isGatheringAreaWithinLimits", new Class<?>[]{BlockPos.class, BlockPos.class}, first, second);
    }

    private static boolean setGatheringCorner(CopperGolem golem, ServerLevel level, BlockPos pos, boolean cornerB) {
        return invoke("setGatheringAreaCorner",
                new Class<?>[]{CopperGolem.class, ServerLevel.class, BlockPos.class, boolean.class},
                golem, level, pos, cornerB);
    }

    private static boolean isGatheringBlockAllowed(
            CopperGolem golem,
            ServerLevel level,
            BlockState state,
            ItemStack tool,
            List<ItemStack> drops,
            List<String> manualTargets) {
        return invoke("isGatheringBlockAllowed",
                new Class<?>[]{CopperGolem.class, ServerLevel.class, BlockState.class, ItemStack.class, List.class, List.class},
                golem, level, state, tool, drops, manualTargets);
    }

    private static void setSelectedGolem(ItemStack wrench, UUID golemId) {
        invoke("setSelectedGolem", new Class<?>[]{ItemStack.class, UUID.class}, wrench, golemId);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(String name, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = CopperGolemWrenchHandler.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(null, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not invoke CopperGolemWrenchHandler#" + name, exception);
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
