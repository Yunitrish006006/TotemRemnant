package com.adaptor.deadrecall.item.copper;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

final class BackpackSortingHelper {
    private BackpackSortingHelper() {
    }

    static boolean canSortInto(NonNullList<ItemStack> items, ItemStack carried) {
        boolean hasMatchingItem = false;
        boolean hasEmptySlot = false;

        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                hasEmptySlot = true;
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(stack, carried)) {
                continue;
            }

            hasMatchingItem = true;
            if (stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }

        return hasMatchingItem && hasEmptySlot;
    }

    static boolean canPlaceSomewhere(NonNullList<ItemStack> items, ItemStack carried) {
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                return true;
            }

            if (ItemStack.isSameItemSameComponents(stack, carried) && stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    static ItemStack insertInto(NonNullList<ItemStack> items, ItemStack carried) {
        ItemStack remaining = carried.copy();

        for (int slot = 0; slot < items.size() && !remaining.isEmpty(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, remaining)) {
                continue;
            }

            int moveCount = Math.min(remaining.getCount(), stack.getMaxStackSize() - stack.getCount());
            if (moveCount <= 0) {
                continue;
            }

            stack.grow(moveCount);
            remaining.shrink(moveCount);
            items.set(slot, stack);
        }

        for (int slot = 0; slot < items.size() && !remaining.isEmpty(); slot++) {
            if (!items.get(slot).isEmpty()) {
                continue;
            }

            int moveCount = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            ItemStack moved = remaining.copyWithCount(moveCount);
            remaining.shrink(moveCount);
            items.set(slot, moved);
        }

        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }
}
