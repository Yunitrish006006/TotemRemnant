package com.adaptor.deadrecall.network.registration;

import com.adaptor.deadrecall.network.SortBackpackPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class LegacyContainerSortService {
    private static final int PLAYER_HOTBAR_SLOT_COUNT = 9;

    private LegacyContainerSortService() {
    }

    static boolean sortOpenContainer(ServerPlayer player, SortBackpackPayload.Target target) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return false;
        }

        if (target == SortBackpackPayload.Target.PLAYER) {
            return sortPlayerInventorySlots(menu, player);
        }

        boolean sorted;
        if (menu instanceof InventoryMenu) {
            sorted = sortSlotRange(menu, 0, InventoryMenu.INV_SLOT_START);
        } else {
            int topSlotCount = findTopSlotCount(menu, player);
            if (topSlotCount <= 0) {
                return false;
            }
            sorted = sortSlotRange(menu, 0, topSlotCount);
        }

        if (sorted) {
            menu.broadcastChanges();
        }
        return sorted;
    }

    private static boolean sortPlayerInventorySlots(AbstractContainerMenu menu, ServerPlayer player) {
        List<Integer> playerSlots = new ArrayList<>();
        int nonEquipmentSlotCount = player.getInventory().getNonEquipmentItems().size();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int containerSlot = slot.getContainerSlot();
            if (slot.container == player.getInventory()
                    && containerSlot >= PLAYER_HOTBAR_SLOT_COUNT
                    && containerSlot < nonEquipmentSlotCount) {
                playerSlots.add(i);
            }
        }

        if (playerSlots.isEmpty()) {
            return false;
        }

        List<ItemStack> stacks = new ArrayList<>(playerSlots.size());
        for (int slotIndex : playerSlots) {
            ItemStack stack = menu.getSlot(slotIndex).getItem();
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }

        if (stacks.isEmpty()) {
            return false;
        }

        applySortedStacks(menu, playerSlots, stacks);
        menu.broadcastChanges();
        return true;
    }

    private static int findTopSlotCount(AbstractContainerMenu menu, ServerPlayer player) {
        int count = 0;
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                break;
            }
            count++;
        }
        return count;
    }

    private static boolean sortSlotRange(AbstractContainerMenu menu, int startInclusive, int endExclusive) {
        List<Integer> slotIndexes = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = startInclusive; i < endExclusive; i++) {
            slotIndexes.add(i);
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }

        if (stacks.isEmpty()) {
            return false;
        }

        applySortedStacks(menu, slotIndexes, stacks);
        return true;
    }

    private static void applySortedStacks(AbstractContainerMenu menu, List<Integer> targetSlots, List<ItemStack> stacks) {
        stacks.sort((left, right) -> {
            String leftId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(left.getItem()).toString();
            String rightId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(right.getItem()).toString();
            int compare = leftId.compareTo(rightId);
            if (compare != 0) {
                return compare;
            }
            return Integer.compare(right.getCount(), left.getCount());
        });

        List<ItemStack> compacted = compactStacks(stacks);
        for (int i = 0; i < targetSlots.size(); i++) {
            ItemStack stack = i < compacted.size() ? compacted.get(i).copy() : ItemStack.EMPTY;
            menu.getSlot(targetSlots.get(i)).setByPlayer(stack);
        }
    }

    private static List<ItemStack> compactStacks(List<ItemStack> stacks) {
        List<ItemStack> compacted = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ItemStack remaining = stack.copy();
            if (!compacted.isEmpty()) {
                ItemStack last = compacted.get(compacted.size() - 1);
                if (ItemStack.isSameItemSameComponents(last, remaining)) {
                    int movable = Math.min(last.getMaxStackSize() - last.getCount(), remaining.getCount());
                    if (movable > 0) {
                        last.grow(movable);
                        remaining.shrink(movable);
                    }
                }
            }
            while (!remaining.isEmpty()) {
                int count = remaining.getCount();
                int max = remaining.getMaxStackSize();
                int take = Math.min(count, max);
                ItemStack next = remaining.copy();
                next.setCount(take);
                compacted.add(next);
                remaining.shrink(take);
            }
        }
        return compacted;
    }
}
