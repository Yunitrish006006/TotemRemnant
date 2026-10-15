package dev.totem.remnant.inventory;

import dev.totem.remnant.item.AbstractBackpackItem;
import dev.totem.remnant.item.TieredBackpackItem;
import dev.totem.remnant.registry.RemnantDataComponents;
import dev.totem.remnant.upgrade.BackpackCapacity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/** Native Bundle gestures over CONTAINER, which remains the only contents store. */
public final class BackpackItemInteraction {
    private BackpackItemInteraction() {}

    public static List<Integer> choices(ItemStack bag) {
        var items = contents(bag);
        if (items == null) return List.of();
        var result = new ArrayList<Integer>();
        for (int i = 0; i < Math.min(capacity(bag), items.size()); i++)
            if (!items.get(i).isEmpty()) result.add(i);
        return result;
    }

    public static int selected(ItemStack bag) {
        var choices = choices(bag);
        int selected = bag.getOrDefault(RemnantDataComponents.SELECTED_ITEM, -1);
        return choices.contains(selected) ? selected : choices.isEmpty() ? -1 : choices.getFirst();
    }

    public static ItemStack selectedStack(ItemStack bag) {
        int selected = selected(bag);
        var items = contents(bag);
        return selected < 0 || items == null ? ItemStack.EMPTY : items.get(selected).copy();
    }

    public static boolean select(ItemStack bag, int index) {
        if (!(bag.getItem() instanceof TieredBackpackItem) || bag.getCount() != 1
                || (index != -1 && !choices(bag).contains(index))) return false;
        if (index == -1) bag.remove(RemnantDataComponents.SELECTED_ITEM);
        else bag.set(RemnantDataComponents.SELECTED_ITEM, index);
        return true;
    }

    /** Backpack on cursor, another stack/empty slot underneath. */
    public static boolean onOther(ItemStack bag, Slot slot, ClickAction action, Player player) {
        ItemStack other = slot.getItem();
        boolean deposit = action == ClickAction.PRIMARY && !other.isEmpty();
        boolean withdraw = action == ClickAction.SECONDARY && other.isEmpty();
        if (!deposit && !withdraw) return false;
        if (!valid(player, bag, slot) || player.containerMenu.getCarried() != bag) return true;
        if (!slot.allowModification(player)) return true;
        if (deposit) transfer(player, bag, other, true, slot.getMaxStackSize(), next -> {
            slot.set(next); return true;
        });
        else {
            ItemStack chosen = selectedStack(bag);
            if (!chosen.isEmpty() && slot.mayPlace(chosen))
                transfer(player, bag, other, false, slot.getMaxStackSize(chosen), next -> {
                    slot.set(next); return true;
                });
        }
        return true;
    }

    /** Backpack in slot, carried stack/empty cursor over it. */
    public static boolean onMe(ItemStack bag, ItemStack carried, Slot slot, ClickAction action,
                               Player player, SlotAccess cursor) {
        if (!player.isAlive() || player.isSpectator()) return true;
        if (action == ClickAction.PRIMARY && carried.isEmpty()) {
            select(bag, -1);
            return false; // Native pickup of the entire backpack.
        }
        boolean deposit = action == ClickAction.PRIMARY && !carried.isEmpty();
        boolean withdraw = action == ClickAction.SECONDARY && carried.isEmpty();
        if (!deposit && !withdraw) { select(bag, -1); return false; }
        if (!valid(player, bag, slot) || slot.getItem() != bag
                || player.containerMenu.getCarried() != carried || !slot.allowModification(player)) return true;
        transfer(player, bag, carried, deposit, 64, cursor::set);
        return true;
    }

    private interface Setter { boolean set(ItemStack next); }

    private static void transfer(Player player, ItemStack bag, ItemStack other, boolean deposit,
                                 int limit, Setter setter) {
        var original = bag.get(DataComponents.CONTAINER);
        var selection = bag.get(RemnantDataComponents.SELECTED_ITEM);
        var items = contents(bag);
        if (items == null || !legal(other)) return;
        int capacity = capacity(bag);
        while (items.size() < capacity) items.add(ItemStack.EMPTY);
        ItemStack next = other.copy();
        if (deposit) {
            if (other.getItem() instanceof AbstractBackpackItem
                    || !(player instanceof ServerPlayer sp
                    ? PortableContainerPolicy.mayInsertIntoBackpack(sp.level(), other)
                    : PortableContainerPolicy.mayInsertIntoBackpack(other))) return;
            for (int pass = 0; pass < 2 && !next.isEmpty(); pass++) {
                for (int i = 0; i < capacity && !next.isEmpty(); i++) {
                    ItemStack at = items.get(i);
                    if (pass == 0 && !at.isEmpty() && ItemStack.isSameItemSameComponents(at, next)) {
                        int move = Math.min(next.getCount(), at.getMaxStackSize() - at.getCount());
                        at.grow(move); next.shrink(move);
                    } else if (pass == 1 && at.isEmpty()) {
                        items.set(i, next.split(Math.min(next.getCount(), next.getMaxStackSize())));
                    }
                }
            }
            if (next.getCount() == other.getCount()) return;
        } else {
            int index = selected(bag);
            if (index < 0 || !other.isEmpty()) return;
            ItemStack at = items.get(index);
            int count = Math.min(at.getCount(), Math.min(limit, at.getMaxStackSize()));
            if (count <= 0) return;
            next = at.split(count);
        }
        try {
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            select(bag, -1);
            if (!setter.set(next)) throw new IllegalStateException("Inventory destination rejected transfer");
            refresh(player);
        } catch (RuntimeException error) {
            if (original == null) bag.remove(DataComponents.CONTAINER); else bag.set(DataComponents.CONTAINER, original);
            if (selection == null) bag.remove(RemnantDataComponents.SELECTED_ITEM);
            else bag.set(RemnantDataComponents.SELECTED_ITEM, selection);
            try { setter.set(other); } catch (RuntimeException ignored) { /* Continue restoring all views. */ }
            refresh(player);
        }
    }

    private static boolean valid(Player player, ItemStack bag, Slot slot) {
        var menu = player.containerMenu;
        return bag.getItem() instanceof TieredBackpackItem && bag.getCount() == 1
                && player.isAlive() && !player.isSpectator()
                && menu != null && menu.stillValid(player) && slot.isActive()
                && slot.index >= 0 && slot.index < menu.slots.size() && menu.getSlot(slot.index) == slot;
    }

    private static int capacity(ItemStack bag) {
        return Math.clamp(BackpackCapacity.configuredSlots(bag), 0, 72);
    }

    private static ArrayList<ItemStack> contents(ItemStack bag) {
        var items = bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .itemCopies().limit(1025).toList();
        if (items.size() > 1024 || items.stream().anyMatch(stack -> !legal(stack))) return null;
        return new ArrayList<>(items);
    }

    private static boolean legal(ItemStack stack) {
        return stack.isEmpty() || (stack.getCount() > 0 && stack.getCount() <= stack.getMaxStackSize());
    }

    private static void refresh(Player player) {
        if (player.inventoryMenu instanceof BackpackPanelMenuAccess access)
            access.totem$getBackpackPanel().refreshAfterExternalCommit();
        if (player.containerMenu != player.inventoryMenu && BackpackPanelMenuAccess.supports(player.containerMenu) && player.containerMenu instanceof BackpackPanelMenuAccess access)
            access.totem$getBackpackPanel().refreshAfterExternalCommit();
        if (player.containerMenu instanceof BackpackMenu menu && menu.storage() != null)
            menu.storage().refreshAfterExternalCommit();
    }
}
