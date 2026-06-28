package com.adaptor.deadrecall.inventory;

import com.adaptor.deadrecall.item.DeathBackpackItem;
import com.adaptor.deadrecall.item.TieredBackpackItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public class BackpackInventory implements Container {
    private final NonNullList<ItemStack> items;
    private final Player player;
    private final InteractionHand hand;
    private final int size;

    public BackpackInventory(Player player, InteractionHand hand, TieredBackpackItem.BackpackTier tier) {
        this(player, hand, tier.getSlots());
    }

    // 動態大小構造函數（用於死亡背包）
    public BackpackInventory(Player player, InteractionHand hand, int size) {
        this.player = player;
        this.hand = hand;
        this.size = size;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        loadFromStack();
    }

    public ItemStack getBackpackStack() {
        return player.getItemInHand(hand);
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack removeItem(int slot, int amount) {
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
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        saveToStack();
        // 檢查是否是空的死亡背包，如果是則立即移除
        ItemStack backpackStack = getBackpackStack();
        if (!backpackStack.isEmpty() && backpackStack.getItem() instanceof DeathBackpackItem && isEmpty()) {
            // 移除空的死亡背包
            if (hand == InteractionHand.MAIN_HAND) {
                player.getInventory().setSelectedItem(ItemStack.EMPTY);
            } else {
                player.getInventory().setItem(Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && !getBackpackStack().isEmpty();
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    private void loadFromStack() {
        ItemStack backpackStack = getBackpackStack();
        if (backpackStack.isEmpty()) {
            return;
        }

        ItemContainerContents container = backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        int index = 0;
        for (ItemStack stack : container.nonEmptyItemCopyStream().toList()) {
            if (index < size) {
                items.set(index, stack.copy());
                index++;
            }
        }
    }

    private void saveToStack() {
        ItemStack backpackStack = getBackpackStack();
        if (backpackStack != null && !backpackStack.isEmpty()) {
            List<ItemStack> toSave = new ArrayList<>(items.size());
            for (ItemStack item : items) {
                toSave.add(item.copy());
            }
            backpackStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(toSave));
        }
    }

    public void onClose(Player player) {
        // 檢查是否是死亡背包且為空
        ItemStack backpackStack = getBackpackStack();
        if (!backpackStack.isEmpty() && backpackStack.getItem() instanceof DeathBackpackItem && isEmpty()) {
            // 移除空的死亡背包
            if (hand == InteractionHand.MAIN_HAND) {
                player.getInventory().setSelectedItem(ItemStack.EMPTY);
            } else {
                player.getInventory().setItem(Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }
}
