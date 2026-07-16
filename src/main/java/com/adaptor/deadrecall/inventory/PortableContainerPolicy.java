package com.adaptor.deadrecall.inventory;

import com.adaptor.deadrecall.item.BackpackItemHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Central policy for portable-container nesting.
 *
 * <p>Legacy invalid contents are not mutated on load. Callers should only use this policy when
 * inserting or transferring an item into another portable container, so players can still remove
 * old nested items safely.</p>
 */
public final class PortableContainerPolicy {
    private PortableContainerPolicy() {
    }

    public static boolean isBackpack(ItemStack stack) {
        return stack != null && !stack.isEmpty() && BackpackItemHelper.isBackpackItem(stack);
    }

    public static boolean isBundle(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.BUNDLE);
    }

    public static boolean isShulkerBox(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static boolean isRestrictedPortableContainer(ItemStack stack) {
        return isBackpack(stack) || isBundle(stack) || isShulkerBox(stack);
    }

    /**
     * Items rejected when moving from player inventory into a DeadRecall backpack.
     */
    public static boolean mayInsertIntoBackpack(ItemStack incoming) {
        return !isRestrictedPortableContainer(incoming);
    }

    /**
     * Backpacks are rejected when moving into vanilla or addon portable containers.
     */
    public static boolean mayInsertIntoPortableContainer(ItemStack incoming) {
        return !isBackpack(incoming);
    }
}
