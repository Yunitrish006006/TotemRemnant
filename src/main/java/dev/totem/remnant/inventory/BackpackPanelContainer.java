package dev.totem.remnant.inventory;

import dev.totem.remnant.item.TieredBackpackItem;
import dev.totem.remnant.upgrade.BackpackCapacity;
import dev.totem.remnant.upgrade.BackpackCompaction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/** Mutable, bounded view of one ordinary backpack selected in the player inventory. */
public final class BackpackPanelContainer implements Container {
    public static final int MAX_PANEL_SLOTS = 72;

    private final Inventory playerInventory;
    private final Player owner;
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(MAX_PANEL_SLOTS, ItemStack.EMPTY);
    private List<ItemStack> overflow = List.of();
    private int selectedInventorySlot = -1;
    private ItemStack selectedBackpack = ItemStack.EMPTY;
    private ItemContainerContents loadedContents = ItemContainerContents.EMPTY;
    private int activeSlots;
    private long lastContentRefreshGameTime = Long.MIN_VALUE;

    public BackpackPanelContainer(Inventory playerInventory) {
        this.playerInventory = playerInventory;
        this.owner = playerInventory.player;
    }

    public boolean selectFirstBackpack() {
        for (int slot = 0; slot < playerInventory.getContainerSize(); slot++) {
            if (playerInventory.getItem(slot).getItem() instanceof TieredBackpackItem) {
                return select(slot);
            }
        }
        return select(-1);
    }

    public boolean select(int inventorySlot) {
        ItemStack candidate = validBackpackAt(inventorySlot);
        int validatedSlot = candidate.isEmpty() ? -1 : inventorySlot;
        boolean changed = validatedSlot != selectedInventorySlot || candidate != selectedBackpack;
        if (!changed) {
            refresh();
            return false;
        }
        selectedInventorySlot = validatedSlot;
        load(candidate);
        return true;
    }

    public void refresh() {
        ItemStack current = validBackpackAt(selectedInventorySlot);
        if (current.isEmpty()) {
            if (selectedInventorySlot != -1 || !selectedBackpack.isEmpty()) {
                selectedInventorySlot = -1;
                load(ItemStack.EMPTY);
            }
            return;
        }
        long gameTime = owner.level().getGameTime();
        if (current == selectedBackpack && gameTime == lastContentRefreshGameTime) {
            return;
        }
        lastContentRefreshGameTime = gameTime;
        ItemContainerContents contents = current.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );
        int configured = Math.min(MAX_PANEL_SLOTS, BackpackCapacity.configuredSlots(current));
        if (current != selectedBackpack
                || configured != activeSlots
                || !contents.equals(loadedContents)) {
            load(current);
        }
    }

    public int selectedInventorySlot() {
        refresh();
        return selectedInventorySlot;
    }

    public int activeSlotCount() {
        refresh();
        return activeSlots;
    }

    public ItemStack selectedBackpack() {
        refresh();
        return selectedBackpack;
    }

    public boolean isSlotActive(int slot) {
        refresh();
        return slot >= 0 && slot < activeSlots && !selectedBackpack.isEmpty();
    }

    @Override
    public int getContainerSize() {
        return MAX_PANEL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        refresh();
        for (int slot = 0; slot < activeSlots; slot++) {
            if (!items.get(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        refresh();
        return inBounds(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        refresh();
        if (!isSlotActive(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stored = items.get(slot);
        if (stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stored.split(amount);
        if (stored.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        refresh();
        if (!isSlotActive(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        refresh();
        if (!isSlotActive(slot)) {
            return;
        }
        ItemStack stored = stack == null ? ItemStack.EMPTY : stack;
        if (!stored.isEmpty() && stored.getCount() > getMaxStackSize(stored)) {
            stored = stored.copyWithCount(getMaxStackSize(stored));
        }
        items.set(slot, stored);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        refresh();
        if (!isSlotActive(slot) || stack == selectedBackpack) {
            return false;
        }
        return owner.level() instanceof ServerLevel serverLevel
                ? PortableContainerPolicy.mayInsertIntoBackpack(serverLevel, stack)
                : PortableContainerPolicy.mayInsertIntoBackpack(stack);
    }

    @Override
    public void setChanged() {
        ItemStack current = validBackpackAt(selectedInventorySlot);
        if (current.isEmpty() || current != selectedBackpack) {
            selectedInventorySlot = -1;
            load(ItemStack.EMPTY);
            return;
        }

        List<ItemStack> serialized = new ArrayList<>(MAX_PANEL_SLOTS + overflow.size());
        items.forEach(stack -> serialized.add(stack.copy()));
        overflow.forEach(stack -> serialized.add(stack.copy()));
        ItemContainerContents contents = ItemContainerContents.fromItems(serialized);
        current.set(DataComponents.CONTAINER, contents);
        loadedContents = contents;
        playerInventory.setChanged();

        if (owner.level() instanceof ServerLevel serverLevel
                && BackpackCompaction.compactIfEnabled(serverLevel, current)) {
            load(current);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        refresh();
        return player == owner && !selectedBackpack.isEmpty();
    }

    @Override
    public void clearContent() {
        refresh();
        for (int slot = 0; slot < activeSlots; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    private ItemStack validBackpackAt(int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= playerInventory.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        ItemStack candidate = playerInventory.getItem(inventorySlot);
        return candidate.getItem() instanceof TieredBackpackItem
                ? candidate
                : ItemStack.EMPTY;
    }

    private void load(ItemStack backpack) {
        selectedBackpack = backpack;
        lastContentRefreshGameTime = owner.level().getGameTime();
        activeSlots = backpack.isEmpty()
                ? 0
                : Math.min(MAX_PANEL_SLOTS, BackpackCapacity.configuredSlots(backpack));
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (backpack.isEmpty()) {
            overflow = List.of();
            loadedContents = ItemContainerContents.EMPTY;
            return;
        }

        loadedContents = backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> serialized = loadedContents.allItemsCopyStream().toList();
        int visible = Math.min(MAX_PANEL_SLOTS, serialized.size());
        for (int slot = 0; slot < visible; slot++) {
            items.set(slot, serialized.get(slot));
        }
        overflow = serialized.size() <= MAX_PANEL_SLOTS
                ? List.of()
                : serialized.subList(MAX_PANEL_SLOTS, serialized.size()).stream()
                .map(ItemStack::copy)
                .toList();
    }

    private static boolean inBounds(int slot) {
        return slot >= 0 && slot < MAX_PANEL_SLOTS;
    }
}
