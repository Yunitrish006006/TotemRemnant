package com.adaptor.deadrecall.item.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public final class CopperGolemTransportGameTest {
    private static final BlockPos GOLEM_POS = new BlockPos(2, 2, 2);
    private static final BlockPos SOURCE_POS = new BlockPos(4, 1, 2);
    private static final BlockPos DESTINATION_POS = new BlockPos(7, 1, 2);

    @GameTest(maxTicks = 50)
    public void sortingPickupReturnAndDepositPreserveExactlyOnceTransactions(GameTestHelper helper) {
        helper.setBlock(SOURCE_POS, Blocks.CHEST);
        helper.setBlock(DESTINATION_POS, Blocks.CHEST);
        Container source = containerAt(helper, SOURCE_POS);
        Container destination = containerAt(helper, DESTINATION_POS);
        source.setItem(0, new ItemStack(Items.DIAMOND, 32));
        destination.setItem(0, new ItemStack(Items.DIAMOND, 1));

        CopperGolem golem = createGolem(helper, GOLEM_POS);
        try {
            configureSortingRoute(helper, golem);
            CopperGolemWrenchHandler.setFuelStackFromMenu(golem, new ItemStack(Items.COAL));

            ItemStack firstPickup = CopperGolemWrenchHandler.pickUpNextItem(
                    golem,
                    helper.getLevel(),
                    source,
                    helper.absolutePos(SOURCE_POS)
            );
            require(helper, firstPickup.is(Items.DIAMOND) && firstPickup.getCount() == 16,
                    "Sorting pickup did not take exactly 16 items");
            require(helper, source.getItem(0).getCount() == 16,
                    "Sorting pickup removed an incorrect source quantity");

            golem.setItemInHand(InteractionHand.MAIN_HAND, firstPickup);
            require(helper, CopperGolemWrenchHandler.returnCarriedItemToSource(golem, helper.getLevel()),
                    "Sorting rollback could not return cargo to its remembered source");
            require(helper, golem.getMainHandItem().isEmpty(),
                    "Sorting rollback left cargo in the golem hand");
            require(helper, source.getItem(0).is(Items.DIAMOND) && source.getItem(0).getCount() == 32,
                    "Sorting rollback did not restore the source exactly once");

            ItemStack secondPickup = CopperGolemWrenchHandler.pickUpNextItem(
                    golem,
                    helper.getLevel(),
                    source,
                    helper.absolutePos(SOURCE_POS)
            );
            require(helper, secondPickup.is(Items.DIAMOND) && secondPickup.getCount() == 16,
                    "Second sorting pickup did not take exactly 16 items");
            golem.setItemInHand(InteractionHand.MAIN_HAND, secondPickup);

            Optional<ItemStack> remaining = CopperGolemWrenchHandler.putCarriedItemIntoDestination(
                    golem,
                    helper.getLevel(),
                    helper.absolutePos(DESTINATION_POS),
                    destination
            );
            require(helper, remaining.isPresent() && remaining.get().isEmpty(),
                    "Sorting destination did not accept the complete carried stack");
            require(helper, destination.getItem(0).is(Items.DIAMOND)
                            && destination.getItem(0).getCount() == 17,
                    "Sorting destination received an incorrect quantity");
            require(helper, source.getItem(0).is(Items.DIAMOND) && source.getItem(0).getCount() == 16,
                    "Sorting deposit changed the source after pickup");
            helper.succeed();
        } finally {
            golem.discard();
        }
    }

    @GameTest(maxTicks = 30)
    public void gatheringHomePreflightAndToolDamageAreAtomic(GameTestHelper helper) {
        SimpleContainer fullHome = new SimpleContainer(3);
        for (int slot = 0; slot < fullHome.getContainerSize(); slot++) {
            fullHome.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        List<ItemStack> deposit = List.of(new ItemStack(Items.COBBLESTONE, 4));
        require(helper, !canInsertAll(fullHome, deposit),
                "Full Home container passed the deposit preflight");
        require(helper, fullHome.getItem(0).getCount() == 64
                        && fullHome.getItem(1).getCount() == 64
                        && fullHome.getItem(2).getCount() == 64,
                "Failed Home preflight mutated container contents");

        SimpleContainer mergeHome = new SimpleContainer(3);
        mergeHome.setItem(0, new ItemStack(Items.COBBLESTONE, 60));
        require(helper, canInsertAll(mergeHome, deposit),
                "Home preflight rejected an exact component-compatible merge");
        require(helper, insertAll(mergeHome, deposit),
                "Home deposit failed after a successful preflight");
        require(helper, mergeHome.getItem(0).is(Items.COBBLESTONE)
                        && mergeHome.getItem(0).getCount() == 64,
                "Home deposit did not merge into the existing stack");

        ItemStack healthyTool = new ItemStack(Items.IRON_PICKAXE);
        healthyTool.setDamageValue(5);
        Object damaged = damageTool(helper.getLevel(), healthyTool);
        ItemStack damagedStack = invokeRecord(damaged, "stack");
        boolean broken = invokeRecord(damaged, "broken");
        require(helper, !broken && damagedStack.is(Items.IRON_PICKAXE)
                        && damagedStack.getDamageValue() == 6,
                "Gathering tool damage did not advance by exactly one");
        require(helper, healthyTool.getDamageValue() == 5,
                "Gathering tool damage mutated the input stack before commit");

        ItemStack lastUseTool = new ItemStack(Items.IRON_PICKAXE);
        lastUseTool.setDamageValue(lastUseTool.getMaxDamage() - 1);
        Object brokenResult = damageTool(helper.getLevel(), lastUseTool);
        ItemStack brokenStack = invokeRecord(brokenResult, "stack");
        boolean didBreak = invokeRecord(brokenResult, "broken");
        require(helper, didBreak && brokenStack.isEmpty(),
                "Last gathering-tool durability point did not produce an atomic broken result");
        helper.succeed();
    }

    private static void configureSortingRoute(GameTestHelper helper, CopperGolem golem) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        SortingBindingService.writeSourceContainer(tag, new CopperGolemWrenchHandler.Binding(
                helper.getLevel().dimension(),
                helper.absolutePos(SOURCE_POS)
        ));
        SortingBindingService.writeBindings(tag, List.of(new CopperGolemWrenchHandler.Binding(
                helper.getLevel().dimension(),
                helper.absolutePos(DESTINATION_POS)
        )));
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.SORTING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, true);
        CopperGolemData.writeEntityTag(golem, tag);
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

    private static boolean canInsertAll(Container container, List<ItemStack> stacks) {
        return invokeWrench("canInsertAll", new Class<?>[]{Container.class, List.class}, container, stacks);
    }

    private static boolean insertAll(Container container, List<ItemStack> stacks) {
        return invokeWrench("insertAll", new Class<?>[]{Container.class, List.class}, container, stacks);
    }

    private static Object damageTool(ServerLevel level, ItemStack tool) {
        return invokeWrench(
                "damageGatheringToolStackAfterBreak",
                new Class<?>[]{ServerLevel.class, ItemStack.class},
                level,
                tool
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

    @SuppressWarnings("unchecked")
    private static <T> T invokeRecord(Object record, String accessor) {
        try {
            Method method = record.getClass().getDeclaredMethod(accessor);
            method.setAccessible(true);
            return (T) method.invoke(record);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not read transaction result accessor " + accessor, exception);
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
