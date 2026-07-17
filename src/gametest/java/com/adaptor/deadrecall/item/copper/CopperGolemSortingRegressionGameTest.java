package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public final class CopperGolemSortingRegressionGameTest {
    private static final BlockPos GOLEM_POS = new BlockPos(2, 2, 2);
    private static final BlockPos SOURCE_POS = new BlockPos(4, 1, 2);
    private static final BlockPos DESTINATION_POS = new BlockPos(7, 1, 2);
    private static final BlockPos SECOND_DESTINATION_POS = new BlockPos(9, 1, 2);

    @GameTest(maxTicks = 50)
    public void blockedSnapshotOnlyClearsWhenSourceTargetOrBindingsChange(GameTestHelper helper) {
        helper.setBlock(SOURCE_POS, Blocks.CHEST);
        helper.setBlock(DESTINATION_POS, Blocks.CHEST);
        helper.setBlock(SECOND_DESTINATION_POS, Blocks.CHEST);
        Container source = containerAt(helper, SOURCE_POS);
        Container destination = containerAt(helper, DESTINATION_POS);
        source.setItem(0, new ItemStack(Items.DIAMOND, 8));
        destination.setItem(0, new ItemStack(Items.DIRT, 64));

        CopperGolem golem = createGolem(helper, GOLEM_POS);
        try {
            configureSortingRoute(helper, golem, List.of(DESTINATION_POS));
            CopperGolemWrenchHandler.markSortingBlocked(
                    golem,
                    helper.getLevel(),
                    helper.absolutePos(SOURCE_POS),
                    source
            );

            require(helper, isSortingBlocked(golem), "Sorting blocked snapshot was not persisted");
            require(helper, !CopperGolemWrenchHandler.shouldClearSortingBlocked(golem, helper.getLevel()),
                    "Unchanged blocked snapshot cleared immediately");

            destination.setItem(1, new ItemStack(Items.DIAMOND));
            destination.setChanged();
            require(helper, CopperGolemWrenchHandler.shouldClearSortingBlocked(golem, helper.getLevel()),
                    "Destination inventory change did not invalidate the blocked snapshot");

            CopperGolemWrenchHandler.clearSortingBlocked(golem);
            require(helper, !isSortingBlocked(golem), "Clearing the blocked snapshot left blocked state behind");

            destination.setItem(1, ItemStack.EMPTY);
            destination.setChanged();
            CopperGolemWrenchHandler.markSortingBlocked(
                    golem,
                    helper.getLevel(),
                    helper.absolutePos(SOURCE_POS),
                    source
            );
            source.setItem(0, new ItemStack(Items.DIAMOND, 7));
            source.setChanged();
            require(helper, CopperGolemWrenchHandler.shouldClearSortingBlocked(golem, helper.getLevel()),
                    "Source inventory change did not invalidate the blocked snapshot");

            CopperGolemWrenchHandler.markSortingBlocked(
                    golem,
                    helper.getLevel(),
                    helper.absolutePos(SOURCE_POS),
                    source
            );
            setBindings(golem, List.of(
                    binding(helper, DESTINATION_POS),
                    binding(helper, SECOND_DESTINATION_POS)
            ));
            require(helper, !isSortingBlocked(golem),
                    "Authoritative binding update did not clear the stale blocked snapshot");
            helper.succeed();
        } finally {
            golem.discard();
        }
    }

    @GameTest(maxTicks = 50)
    public void nestedDeadRecallBackpackReceivesMatchingCargoBeforeOuterChest(GameTestHelper helper) {
        helper.setBlock(SOURCE_POS, Blocks.CHEST);
        helper.setBlock(DESTINATION_POS, Blocks.CHEST);
        Container destination = containerAt(helper, DESTINATION_POS);

        ItemStack backpack = new ItemStack(ModItems.BACKPACK_BASIC);
        backpack.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND)))
        );
        destination.setItem(0, backpack);

        CopperGolem golem = createGolem(helper, GOLEM_POS);
        try {
            configureSortingRoute(helper, golem, List.of(DESTINATION_POS));
            golem.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 16));

            Optional<ItemStack> remaining = CopperGolemWrenchHandler.putCarriedItemIntoDestination(
                    golem,
                    helper.getLevel(),
                    helper.absolutePos(DESTINATION_POS),
                    destination
            );
            require(helper, remaining.isPresent() && remaining.get().isEmpty(),
                    "Nested backpack did not accept matching cargo");
            require(helper, destination.getItem(1).isEmpty(),
                    "Cargo was written into the outer chest instead of the matching nested backpack");
            require(helper, countNestedItem(destination.getItem(0), Items.DIAMOND) == 17,
                    "Nested backpack received an incorrect quantity");

            ItemStack nestedBeforeRejectedCargo = destination.getItem(0).copy();
            ItemStack carriedBackpack = new ItemStack(ModItems.BACKPACK_BASIC);
            golem.setItemInHand(InteractionHand.MAIN_HAND, carriedBackpack);
            Optional<ItemStack> rejected = CopperGolemWrenchHandler.putCarriedItemIntoDestination(
                    golem,
                    helper.getLevel(),
                    helper.absolutePos(DESTINATION_POS),
                    destination
            );
            require(helper, rejected.isPresent()
                            && rejected.get().is(ModItems.BACKPACK_BASIC)
                            && rejected.get().getCount() == 1,
                    "Sorting nested a DeadRecall backpack inside another backpack or destination");
            require(helper, ItemStack.isSameItemSameComponents(nestedBeforeRejectedCargo, destination.getItem(0))
                            && countNestedItem(destination.getItem(0), Items.DIAMOND) == 17,
                    "Rejected backpack cargo mutated the existing nested backpack");
            helper.succeed();
        } finally {
            golem.discard();
        }
    }

    @GameTest(maxTicks = 50)
    public void removingLastDestinationRollsCargoBackExactlyOnce(GameTestHelper helper) {
        helper.setBlock(SOURCE_POS, Blocks.CHEST);
        helper.setBlock(DESTINATION_POS, Blocks.CHEST);
        Container source = containerAt(helper, SOURCE_POS);
        source.setItem(0, new ItemStack(Items.EMERALD, 32));

        CopperGolem golem = createGolem(helper, GOLEM_POS);
        try {
            configureSortingRoute(helper, golem, List.of(DESTINATION_POS));
            CopperGolemWrenchHandler.setFuelStackFromMenu(golem, new ItemStack(Items.COAL));
            ItemStack picked = CopperGolemWrenchHandler.pickUpNextItem(
                    golem,
                    helper.getLevel(),
                    source,
                    helper.absolutePos(SOURCE_POS)
            );
            require(helper, picked.is(Items.EMERALD) && picked.getCount() == 16,
                    "Fixture pickup did not take the expected cargo");
            golem.setItemInHand(InteractionHand.MAIN_HAND, picked);

            CopperGolemWrenchHandler.Binding destinationBinding = binding(helper, DESTINATION_POS);
            require(helper, removeBinding(golem, destinationBinding),
                    "Removing the last destination binding was rejected");
            require(helper, CopperGolemWrenchHandler.getBindings(golem).isEmpty(),
                    "Last destination binding remained after removal");
            require(helper, golem.getMainHandItem().isEmpty(),
                    "Cargo remained in the golem hand after the last destination was removed");
            require(helper, source.getItem(0).is(Items.EMERALD) && source.getItem(0).getCount() == 32,
                    "Removing the last destination did not roll cargo back exactly once");
            require(helper, CopperGolemWrenchHandler.getRememberedSource(golem).isEmpty(),
                    "Successful rollback retained stale remembered-source data");

            require(helper, !removeBinding(golem, destinationBinding),
                    "Removing an already removed destination unexpectedly succeeded");
            require(helper, source.getItem(0).getCount() == 32,
                    "Repeated binding removal duplicated the rolled-back cargo");
            helper.succeed();
        } finally {
            golem.discard();
        }
    }

    @GameTest(maxTicks = 80)
    public void unloadedBindingIsRetainedAndBecomesUsableAfterChunkLoad(GameTestHelper helper) {
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        ServerLevel level = helper.getLevel();
        BlockPos farPos = findUnloadedPosition(helper);
        CopperGolemWrenchHandler.Binding farBinding = new CopperGolemWrenchHandler.Binding(
                level.dimension(),
                farPos
        );
        try {
            setBindings(golem, List.of(farBinding));
            require(helper, !level.isLoaded(farPos),
                    "Could not create an unloaded binding fixture");
            require(helper, !pruneUnavailableBindings(golem, level),
                    "Unloaded destination was incorrectly pruned");
            require(helper, CopperGolemWrenchHandler.getBindings(golem).equals(List.of(farBinding)),
                    "Unloaded destination binding was not retained exactly");

            level.getChunk(farPos.getX() >> 4, farPos.getZ() >> 4);
            require(helper, level.setBlockAndUpdate(farPos, Blocks.CHEST.defaultBlockState()),
                    "Could not create the destination after loading its chunk");
            require(helper, level.isLoaded(farPos), "Destination chunk did not become loaded");
            require(helper, CopperGolemWrenchHandler.tryCreateBoundTarget(level, farPos) != null,
                    "Retained binding did not resolve after its chunk loaded");
            require(helper, !pruneUnavailableBindings(golem, level),
                    "Valid destination was pruned after chunk load");

            level.setBlockAndUpdate(farPos, Blocks.AIR.defaultBlockState());
            require(helper, pruneUnavailableBindings(golem, level),
                    "Loaded destination removal did not prune the invalid binding");
            require(helper, CopperGolemWrenchHandler.getBindings(golem).isEmpty(),
                    "Removed destination binding remained after authoritative pruning");
            helper.succeed();
        } finally {
            if (level.isLoaded(farPos)) {
                level.setBlockAndUpdate(farPos, Blocks.AIR.defaultBlockState());
            }
            golem.discard();
        }
    }

    private static void configureSortingRoute(
            GameTestHelper helper,
            CopperGolem golem,
            List<BlockPos> relativeDestinations
    ) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        SortingBindingService.writeSourceContainer(tag, binding(helper, SOURCE_POS));
        SortingBindingService.writeBindings(tag, relativeDestinations.stream()
                .map(pos -> binding(helper, pos))
                .toList());
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.SORTING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, true);
        CopperGolemData.writeEntityTag(golem, tag);
    }

    private static CopperGolemWrenchHandler.Binding binding(GameTestHelper helper, BlockPos relativePos) {
        return new CopperGolemWrenchHandler.Binding(
                helper.getLevel().dimension(),
                helper.absolutePos(relativePos)
        );
    }

    private static BlockPos findUnloadedPosition(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(GOLEM_POS);
        for (int distance = 512; distance <= 8192; distance += 512) {
            BlockPos candidate = new BlockPos(origin.getX() + distance, origin.getY(), origin.getZ() + distance);
            if (!helper.getLevel().isLoaded(candidate)) {
                return candidate;
            }
        }
        throw helper.assertionException("Could not locate an unloaded chunk for the sorting fixture");
    }

    private static int countNestedItem(ItemStack backpack, net.minecraft.world.item.Item item) {
        return backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream()
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static boolean isSortingBlocked(CopperGolem golem) {
        return CopperGolemData.readEntityTag(golem).getBooleanOr("deadrecall_sorting_blocked", false);
    }

    private static Container containerAt(GameTestHelper helper, BlockPos relativePos) {
        Object blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(relativePos));
        if (blockEntity instanceof Container container) {
            return container;
        }
        throw helper.assertionException("Missing container fixture at " + relativePos);
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

    private static boolean removeBinding(
            CopperGolem golem,
            CopperGolemWrenchHandler.Binding binding
    ) {
        return invokeWrench(
                "removeBinding",
                new Class<?>[]{CopperGolem.class, CopperGolemWrenchHandler.Binding.class},
                golem,
                binding
        );
    }

    private static void setBindings(
            CopperGolem golem,
            List<CopperGolemWrenchHandler.Binding> bindings
    ) {
        invokeWrench("setBindings", new Class<?>[]{CopperGolem.class, List.class}, golem, bindings);
    }

    private static boolean pruneUnavailableBindings(CopperGolem golem, ServerLevel level) {
        return invokeWrench(
                "pruneUnavailableBindings",
                new Class<?>[]{CopperGolem.class, net.minecraft.server.MinecraftServer.class},
                golem,
                level.getServer()
        );
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

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
