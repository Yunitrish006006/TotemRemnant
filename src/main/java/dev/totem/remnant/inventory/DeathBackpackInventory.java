package dev.totem.remnant.inventory;

import dev.totem.remnant.death.DeathBackpackRecoveryService;
import dev.totem.remnant.item.BackpackItemHelper;
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

    public DeathBackpackInventory(Player owner, InteractionHand hand, int size) {
        this.owner = owner;
        this.hand = hand;
        this.backpack = owner.getItemInHand(hand);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        int index = 0;
        for (ItemStack stack : backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList()) {
            if (index >= size) break;
            items.set(index++, stack.copy());
        }
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
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
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return !BackpackItemHelper.isBackpackItem(stack); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public void setChanged() {
        List<ItemStack> contents = new ArrayList<>(items.size());
        items.forEach(stack -> contents.add(stack.copy()));
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
    }
    @Override public boolean stillValid(Player player) { return player == owner && owner.getItemInHand(hand) == backpack; }
    @Override public void stopOpen(ContainerUser user) {
        if (user instanceof ServerPlayer player && isEmpty()) {
            DeathBackpackRecoveryService.recoverBoundNode(player, backpack);
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
    }
}
