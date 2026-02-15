package com.adaptor.deadrecall.inventory;

import com.adaptor.deadrecall.item.TieredBackpackItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

public class BackpackInventory implements Inventory {
    private final DefaultedList<ItemStack> items;
    private final PlayerEntity player;
    private final Hand hand;
    private final int size;

    public BackpackInventory(PlayerEntity player, Hand hand, TieredBackpackItem.BackpackTier tier) {
        this.player = player;
        this.hand = hand;
        this.size = tier.getSlots();
        this.items = DefaultedList.ofSize(size, ItemStack.EMPTY);
        loadFromStack();
    }

    private ItemStack getBackpackStack() {
        return player.getStackInHand(hand);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = items.get(slot);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed;
        if (result.getCount() <= amount) {
            items.set(slot, ItemStack.EMPTY);
            removed = result;
        } else {
            removed = result.split(amount);
        }

        if (!removed.isEmpty()) {
            markDirty();
        }
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        markDirty();
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public void markDirty() {
        saveToStack();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return player == this.player && !getBackpackStack().isEmpty();
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    private void loadFromStack() {
        ItemStack backpackStack = getBackpackStack();
        if (backpackStack.isEmpty()) {
            return;
        }

        ContainerComponent container = backpackStack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        int index = 0;
        for (ItemStack stack : container.iterateNonEmpty()) {
            if (index < size) {
                items.set(index, stack.copy());
                index++;
            }
        }
    }

    private void saveToStack() {
        ItemStack backpackStack = getBackpackStack();
        if (backpackStack != null && !backpackStack.isEmpty()) {
            backpackStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(items));
        }
    }
}


