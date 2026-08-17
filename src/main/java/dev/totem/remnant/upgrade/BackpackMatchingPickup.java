package dev.totem.remnant.upgrade;

import dev.totem.remnant.item.TieredBackpackItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Redirects incoming stackable items only into backpacks already containing that exact variant. */
public final class BackpackMatchingPickup {
    private BackpackMatchingPickup() {
    }

    public static boolean deposit(Inventory inventory, ItemStack incoming) {
        if (incoming.isEmpty() || incoming.getMaxStackSize() <= 1
                || !(inventory.player.level() instanceof ServerLevel level)) return false;
        int originalCount = incoming.getCount();
        for (int inventorySlot = 0; inventorySlot < inventory.getContainerSize() && !incoming.isEmpty(); inventorySlot++) {
            ItemStack backpack = inventory.getItem(inventorySlot);
            if (!(backpack.getItem() instanceof TieredBackpackItem)
                    || !BackpackUpgradeData.has(backpack, BackpackUpgradeType.MATCHING_PICKUP)) continue;
            NonNullList<ItemStack> contents = NonNullList.withSize(BackpackCapacity.slots(backpack), ItemStack.EMPTY);
            backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
            boolean knownVariant = contents.stream().anyMatch(
                    stack -> ItemStack.isSameItemSameComponents(stack, incoming));
            if (!knownVariant) continue;

            int before = incoming.getCount();
            mergeExisting(contents, incoming);
            fillEmpty(contents, incoming);
            if (incoming.getCount() != before) {
                backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
                BackpackCompaction.compactIfEnabled(level, backpack);
                inventory.setChanged();
            }
        }
        return incoming.getCount() != originalCount;
    }

    private static void mergeExisting(NonNullList<ItemStack> contents, ItemStack incoming) {
        for (ItemStack stored : contents) {
            if (!ItemStack.isSameItemSameComponents(stored, incoming)) continue;
            int moved = Math.min(incoming.getCount(), stored.getMaxStackSize() - stored.getCount());
            stored.grow(moved);
            incoming.shrink(moved);
            if (incoming.isEmpty()) return;
        }
    }

    private static void fillEmpty(NonNullList<ItemStack> contents, ItemStack incoming) {
        for (int index = 0; index < contents.size() && !incoming.isEmpty(); index++) {
            if (!contents.get(index).isEmpty()) continue;
            int moved = Math.min(incoming.getCount(), incoming.getMaxStackSize());
            contents.set(index, incoming.copyWithCount(moved));
            incoming.shrink(moved);
        }
    }
}
