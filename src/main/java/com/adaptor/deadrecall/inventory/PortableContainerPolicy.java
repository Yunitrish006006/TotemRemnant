package com.adaptor.deadrecall.inventory;

import com.adaptor.deadrecall.item.BackpackItemHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

/**
 * Central policy for portable-container nesting.
 *
 * <p>Legacy invalid contents are not mutated on load. Callers should only use this policy when
 * inserting or transferring an item into another portable container, so players can still remove
 * old nested items safely.</p>
 */
public final class PortableContainerPolicy {
    public static final TagKey<Item> PORTABLE_CONTAINERS = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("deadrecall", "portable_containers")
    );

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

    public static boolean isConfiguredPortableContainer(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(PORTABLE_CONTAINERS);
    }

    public static boolean isRestrictedPortableContainer(ItemStack stack) {
        return isBackpack(stack)
                || isBundle(stack)
                || isShulkerBox(stack)
                || isConfiguredPortableContainer(stack);
    }

    /**
     * Items rejected when moving from player inventory into a DeadRecall backpack.
     */
    public static boolean mayInsertIntoBackpack(ItemStack incoming) {
        return !isRestrictedPortableContainer(incoming);
    }

    /**
     * Backpacks are rejected when moving into vanilla or addon portable containers.
     *
     * <p>Vanilla bundle paths also consult {@code Item#canFitInsideContainerItems()}, which every
     * DeadRecall backpack overrides. Shulker menu and sided-transfer paths are guarded separately
     * because they expose different vanilla insertion APIs.</p>
     */
    public static boolean mayInsertIntoPortableContainer(ItemStack incoming) {
        return !isBackpack(incoming);
    }

    /**
     * Applies the reverse-direction rule to mod-owned transfer code that writes directly to a
     * {@link Container} instead of going through vanilla sided insertion helpers.
     */
    public static boolean mayInsertIntoContainer(Container target, ItemStack incoming) {
        return !(target instanceof ShulkerBoxBlockEntity)
                || mayInsertIntoPortableContainer(incoming);
    }
}
