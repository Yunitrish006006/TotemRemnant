package com.adaptor.deadrecall.inventory;

import com.adaptor.deadrecall.space.SpaceUnitHandler;
import com.adaptor.deadrecall.item.DeathBackpackItem;
import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.item.TieredBackpackItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BackpackInventory implements Container {
    private final NonNullList<ItemStack> items;
    private final Player player;
    private final InteractionHand hand;
    private final ItemStack backpackStack;
    private final int size;

    public BackpackInventory(Player player, InteractionHand hand, TieredBackpackItem.BackpackTier tier) {
        this(player, hand, tier.getSlots());
    }

    // 動態大小構造函數（用於死亡背包）
    public BackpackInventory(Player player, InteractionHand hand, int size) {
        this.player = player;
        this.hand = hand;
        this.backpackStack = player.getItemInHand(hand);
        this.size = size;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        loadFromStack();
    }

    public ItemStack getBackpackStack() {
        return isTrackedBackpackStack() ? backpackStack : ItemStack.EMPTY;
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !BackpackItemHelper.isBackpackItem(stack);
    }

    @Override
    public void setChanged() {
        saveToStack();
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && isTrackedBackpackStack() && playerHasTrackedBackpackStack();
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (user instanceof Player closingPlayer) {
            onClose(closingPlayer);
        }
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
        if (isTrackedBackpackStack()) {
            List<ItemStack> toSave = new ArrayList<>(items.size());
            for (ItemStack item : items) {
                toSave.add(item.copy());
            }
            backpackStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(toSave));
        }
    }

    public void onClose(Player player) {
        removeEmptyDeathBackpack();
    }

    public void sortContents() {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }

        stacks.sort(Comparator
                .comparing((ItemStack stack) -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed()));

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
                int amount = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack split = remaining.copy();
                split.setCount(amount);
                compacted.add(split);
                remaining.shrink(amount);
            }
        }

        for (int slot = 0; slot < size; slot++) {
            items.set(slot, slot < compacted.size() ? compacted.get(slot) : ItemStack.EMPTY);
        }

        setChanged();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    private boolean isTrackedBackpackStack() {
        return !backpackStack.isEmpty() && BackpackItemHelper.isBackpackItem(backpackStack);
    }

    private boolean playerHasTrackedBackpackStack() {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == backpackStack) {
                return true;
            }
        }
        return player.getItemInHand(hand) == backpackStack;
    }

    private void removeEmptyDeathBackpack() {
        if (!(backpackStack.getItem() instanceof DeathBackpackItem) || !isEmpty()) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            SpaceUnitHandler.disableDeathNodeFromBackpack(serverPlayer, backpackStack);
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == backpackStack) {
                inventory.setItem(slot, ItemStack.EMPTY);
                inventory.setChanged();
                return;
            }
        }

        if (hand == InteractionHand.MAIN_HAND && player.getItemInHand(InteractionHand.MAIN_HAND) == backpackStack) {
            inventory.setSelectedItem(ItemStack.EMPTY);
            inventory.setChanged();
        } else if (hand == InteractionHand.OFF_HAND && player.getItemInHand(InteractionHand.OFF_HAND) == backpackStack) {
            inventory.setItem(Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
            inventory.setChanged();
        }
    }
}
