package dev.totem.remnant.inventory;

import dev.totem.remnant.upgrade.BackpackUpgradeData;
import dev.totem.remnant.upgrade.BackpackUpgradeItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Mutable view of the upgrade slots stored on one backpack ItemStack. */
public final class BackpackUpgradeInventory implements Container {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack backpack;
    private final NonNullList<ItemStack> modules;
    private final boolean persistent;

    public BackpackUpgradeInventory(Player owner, InteractionHand hand, int size) {
        this.owner = owner;
        this.hand = hand;
        this.backpack = owner.getItemInHand(hand);
        this.modules = NonNullList.withSize(size, ItemStack.EMPTY);
        List<ItemStack> stored = BackpackUpgradeData.read(backpack, size);
        for (int index = 0; index < size; index++) {
            modules.set(index, stored.get(index).copy());
        }
        this.persistent = true;
    }

    private BackpackUpgradeInventory(int size) {
        this.owner = null;
        this.hand = null;
        this.backpack = ItemStack.EMPTY;
        this.modules = NonNullList.withSize(size, ItemStack.EMPTY);
        this.persistent = false;
    }

    public static BackpackUpgradeInventory clientSide(int size) {
        return new BackpackUpgradeInventory(size);
    }

    @Override public int getContainerSize() { return modules.size(); }
    @Override public boolean isEmpty() { return modules.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return modules.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = modules.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) modules.set(slot, ItemStack.EMPTY);
        setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = modules.get(slot);
        modules.set(slot, ItemStack.EMPTY);
        setChanged();
        return removed;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        modules.set(slot, stack.getItem() instanceof BackpackUpgradeItem
                ? stack.copyWithCount(1) : ItemStack.EMPTY);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof BackpackUpgradeItem incoming)) return false;
        // Capacity modules intentionally stack across separate upgrade slots: each adds one row.
        if (incoming.type() == dev.totem.remnant.upgrade.BackpackUpgradeType.CAPACITY) return true;
        for (int index = 0; index < modules.size(); index++) {
            if (index != slot && modules.get(index).getItem() instanceof BackpackUpgradeItem existing
                    && existing.type() == incoming.type()) return false;
        }
        return true;
    }
    @Override public void clearContent() { modules.clear(); setChanged(); }
    @Override public void setChanged() {
        if (!persistent) return;
        List<ItemStack> snapshot = new ArrayList<>(modules.size());
        modules.forEach(stack -> snapshot.add(stack.copy()));
        BackpackUpgradeData.write(backpack, snapshot, modules.size());
    }
    @Override public boolean stillValid(Player player) {
        return !persistent || player == owner && owner.getItemInHand(hand) == backpack;
    }
}
