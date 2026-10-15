package dev.totem.remnant.inventory;

import dev.totem.remnant.death.DeathBackpackRecoveryService;
import dev.totem.remnant.item.DeathBackpackItem;
import dev.totem.remnant.upgrade.BackpackCompaction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/** Remnant-owned mutable view of a death backpack's container component. */
public final class DeathBackpackInventory implements Container {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack backpack;
    private final NonNullList<ItemStack> items;
    private final List<ItemStack> overflow;
    private ItemContainerContents loadedContents;

    public DeathBackpackInventory(Player owner, InteractionHand hand, int size) {
        this.owner = owner;
        this.hand = hand;
        this.backpack = owner.getItemInHand(hand);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        loadedContents = backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        loadedContents.copyInto(items);
        overflow = new ArrayList<>(loadedContents.itemCopies().skip(size).toList());
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty) && overflow.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = stack.split(amount);
        setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        setChanged();
        return result;
    }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return owner.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                ? PortableContainerPolicy.mayInsertIntoBackpack(serverLevel, stack)
                : PortableContainerPolicy.mayInsertIntoBackpack(stack);
    }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public void setChanged() {
        List<ItemStack> contents = new ArrayList<>(items.size());
        items.forEach(stack -> contents.add(stack.copy()));
        overflow.forEach(stack -> contents.add(stack.copy()));
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        if (overflow.isEmpty() && owner.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && BackpackCompaction.compactIfEnabled(serverLevel, backpack)) {
            for (int index = 0; index < items.size(); index++) items.set(index, ItemStack.EMPTY);
            backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        }
        loadedContents = backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        owner.getInventory().setChanged();
    }
    public void refreshAfterExternalCommit() {
        if (!stillValid(owner)) return;
        loadedContents = backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        loadedContents.copyInto(items);
        overflow.clear();
        loadedContents.itemCopies().skip(items.size()).forEach(overflow::add);
    }
    public boolean contentsCurrent() {
        return stillValid(owner) && loadedContents.equals(backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }
    public boolean replaceContents(List<ItemStack> replacement) {
        if (replacement.size() != items.size() || !contentsCurrent()) return false;
        ItemContainerContents before = loadedContents;
        try {
            for (int i = 0; i < items.size(); i++) items.set(i, replacement.get(i).copy());
            setChanged();
            return true;
        } catch (RuntimeException failure) {
            backpack.set(DataComponents.CONTAINER, before);
            loadedContents = before;
            before.copyInto(items);
            return false;
        }
    }
    @Override public boolean stillValid(Player player) { return player == owner && owner.getItemInHand(hand) == backpack; }
    public ItemStack getBackpackStack() { return backpack; }
    @Override public void stopOpen(ContainerUser user) {
        if (user instanceof ServerPlayer player && stillValid(player) && isEmpty() && backpack.getItem() instanceof DeathBackpackItem) {
            DeathBackpackRecoveryService.recoverBoundNode(player, backpack);
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
    }
}
