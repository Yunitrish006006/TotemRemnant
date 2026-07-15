package com.adaptor.deadrecall.item.copper;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortingBackpackRegressionTest {
    @BeforeAll
    static void bootStrap() {
        MinecraftTestBootstrap.bootStrap();
    }

    @Test
    void mergesMatchingStackBeforeUsingEmptyBackpackSlot() {
        NonNullList<ItemStack> backpackItems = NonNullList.withSize(9, ItemStack.EMPTY);
        backpackItems.set(0, stack(Items.COBBLESTONE, 63));

        ItemStack remaining = BackpackSortingHelper.insertInto(backpackItems, stack(Items.COBBLESTONE, 2));

        assertTrue(remaining.isEmpty());
        assertEquals(64, backpackItems.get(0).getCount());
        assertStack(Items.COBBLESTONE, 1, backpackItems.get(1));
    }

    @Test
    void refusesDifferentItemWhenBackpackOnlyHasEmptySlots() {
        NonNullList<ItemStack> backpackItems = NonNullList.withSize(9, ItemStack.EMPTY);

        assertFalse(BackpackSortingHelper.canSortInto(backpackItems, stack(Items.DIRT, 1)));
    }

    @Test
    void acceptsSameItemWhenMatchingStackIsFullAndEmptySlotExists() {
        NonNullList<ItemStack> backpackItems = NonNullList.withSize(9, ItemStack.EMPTY);
        backpackItems.set(0, stack(Items.DIRT, 64));

        assertTrue(BackpackSortingHelper.canSortInto(backpackItems, stack(Items.DIRT, 1)));
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(Holder.direct(item, DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 64)
                .build()), count);
    }

    private static void assertStack(Item item, int count, ItemStack stack) {
        assertTrue(ItemStack.isSameItemSameComponents(stack(item, count), stack));
        assertEquals(count, stack.getCount());
    }
}
