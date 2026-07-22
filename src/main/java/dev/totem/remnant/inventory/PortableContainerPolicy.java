package dev.totem.remnant.inventory;

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
import dev.totem.remnant.item.BackpackItemHelper;

/** Remnant-owned policy for portable-container nesting; retains the legacy tag identifier. */
public final class PortableContainerPolicy {
    public static final TagKey<Item> PORTABLE_CONTAINERS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath("deadrecall", "portable_containers"));
    private PortableContainerPolicy() { }
    public static boolean isBackpack(ItemStack stack) { return BackpackItemHelper.isBackpackItem(stack); }
    public static boolean isBundle(ItemStack stack) { return stack != null && !stack.isEmpty() && stack.is(Items.BUNDLE); }
    public static boolean isShulkerBox(ItemStack stack) { return stack != null && !stack.isEmpty() && stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock; }
    public static boolean isConfiguredPortableContainer(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(PORTABLE_CONTAINERS);
    }
    public static boolean isRestrictedPortableContainer(ItemStack stack) {
        return isBackpack(stack) || isBundle(stack) || isShulkerBox(stack) || isConfiguredPortableContainer(stack);
    }
    public static boolean mayInsertIntoBackpack(ItemStack incoming) { return !isRestrictedPortableContainer(incoming); }
    public static boolean mayInsertIntoPortableContainer(ItemStack incoming) { return !isBackpack(incoming); }
    public static boolean mayInsertIntoContainer(Container target, ItemStack incoming) { return !(target instanceof ShulkerBoxBlockEntity) || mayInsertIntoPortableContainer(incoming); }
}
