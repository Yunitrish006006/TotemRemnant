package dev.totem.remnant.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Remnant backpack helpers used by capture and nesting policy. */
public final class BackpackItemHelper {
    private BackpackItemHelper() { }

    public static boolean isBackpackItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof AbstractBackpackItem;
    }

    public static int countStoredStacks(ItemStack backpackStack) {
        if (!isBackpackItem(backpackStack)) return 0;
        return (int) backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream().count();
    }
}
