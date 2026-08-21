package dev.totem.remnant.upgrade;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

/** Event-driven, recipe-authoritative lossless compression for vanilla metals. */
public final class BackpackCompaction {
    private static final List<Compression> COMPRESSIONS = List.of(
            new Compression(Items.RAW_IRON, Items.RAW_IRON_BLOCK),
            new Compression(Items.RAW_COPPER, Items.RAW_COPPER_BLOCK),
            new Compression(Items.RAW_GOLD, Items.RAW_GOLD_BLOCK),
            // Nugget routes precede ingot routes so 81 nuggets can safely chain to one block.
            new Compression(Items.IRON_NUGGET, Items.IRON_INGOT),
            new Compression(Items.GOLD_NUGGET, Items.GOLD_INGOT),
            new Compression(Items.IRON_INGOT, Items.IRON_BLOCK),
            new Compression(Items.COPPER_INGOT, Items.COPPER_BLOCK.weathering().unaffected()),
            new Compression(Items.GOLD_INGOT, Items.GOLD_BLOCK),
            new Compression(Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK)
    );

    private BackpackCompaction() {
    }

    public static boolean compactIfEnabled(ServerLevel level, ItemStack backpack) {
        if (!BackpackUpgradeData.has(backpack, BackpackUpgradeType.COMPACTION)
                || !(backpack.getItem() instanceof dev.totem.remnant.item.TieredBackpackItem)) {
            return false;
        }
        NonNullList<ItemStack> contents = NonNullList.withSize(BackpackCapacity.slots(backpack), ItemStack.EMPTY);
        backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        boolean changed = false;
        for (Compression compression : COMPRESSIONS) {
            while (count(contents, compression.input()) >= 9) {
                ItemStack result = recipeResult(level, compression);
                if (result.isEmpty()) break;
                NonNullList<ItemStack> compacted = copy(contents);
                remove(compacted, compression.input(), 9);
                if (!canInsert(compacted, result)) break;
                insert(compacted, result.copy());
                contents = compacted;
                changed = true;
            }
        }
        if (changed) {
            backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        }
        return changed;
    }

    private static ItemStack recipeResult(ServerLevel level, Compression compression) {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int index = 0; index < 9; index++) grid.add(new ItemStack(compression.input()));
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(holder -> holder.value().assemble(input))
                .filter(stack -> stack.is(compression.output()))
                .orElse(ItemStack.EMPTY);
    }

    private static int count(List<ItemStack> contents, Item item) {
        return contents.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static void remove(List<ItemStack> contents, Item item, int amount) {
        int remaining = amount;
        for (int index = 0; index < contents.size() && remaining > 0; index++) {
            ItemStack stack = contents.get(index);
            if (!stack.is(item)) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
            if (stack.isEmpty()) contents.set(index, ItemStack.EMPTY);
        }
    }

    private static boolean canInsert(List<ItemStack> contents, ItemStack incoming) {
        int room = 0;
        for (ItemStack stack : contents) {
            if (stack.isEmpty()) room += incoming.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(stack, incoming)) room += stack.getMaxStackSize() - stack.getCount();
            if (room >= incoming.getCount()) return true;
        }
        return false;
    }

    private static void insert(List<ItemStack> contents, ItemStack incoming) {
        for (ItemStack stack : contents) {
            if (!ItemStack.isSameItemSameComponents(stack, incoming)) continue;
            int moved = Math.min(incoming.getCount(), stack.getMaxStackSize() - stack.getCount());
            stack.grow(moved);
            incoming.shrink(moved);
            if (incoming.isEmpty()) return;
        }
        for (int index = 0; index < contents.size(); index++) {
            if (!contents.get(index).isEmpty()) continue;
            int moved = Math.min(incoming.getCount(), incoming.getMaxStackSize());
            contents.set(index, incoming.copyWithCount(moved));
            incoming.shrink(moved);
            if (incoming.isEmpty()) return;
        }
    }

    private static NonNullList<ItemStack> copy(List<ItemStack> contents) {
        NonNullList<ItemStack> result = NonNullList.withSize(contents.size(), ItemStack.EMPTY);
        for (int index = 0; index < contents.size(); index++) {
            result.set(index, contents.get(index).copy());
        }
        return result;
    }

    private record Compression(Item input, Item output) {
    }
}
