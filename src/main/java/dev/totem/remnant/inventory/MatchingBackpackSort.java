package dev.totem.remnant.inventory;

import dev.totem.remnant.item.AbstractBackpackItem;
import dev.totem.remnant.item.TieredBackpackItem;
import dev.totem.remnant.upgrade.BackpackCapacity;
import dev.totem.remnant.upgrade.BackpackCompaction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** One bounded transaction: matching deposits and loose-item sorting on one surface. */
public final class MatchingBackpackSort {
    private MatchingBackpackSort() { }

    public static boolean isVanillaChest(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        return menu.getClass() == ChestMenu.class;
    }

    static boolean sort(ServerPlayer player, boolean playerSide, ItemStack pinned) {
        var menu = player.containerMenu;
        if (player.isSpectator() || !player.isAlive() || !menu.stillValid(player)) return false;
        if (!(menu instanceof BackpackMenu) && !BackpackPanelMenuAccess.supports(menu) && !isVanillaChest(menu)) return false;
        if (!playerSide && !isVanillaChest(menu)) return false;
        List<Slot> loose = new ArrayList<>();
        List<Bag> bags = new ArrayList<>();
        Set<ItemStack> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        java.util.Map<net.minecraft.world.Container, Set<Integer>> addresses = new IdentityHashMap<>();
        int inventorySize = player.getInventory().getNonEquipmentItems().size();
        for (Slot slot : menu.slots) {
            if (slot instanceof BackpackPanelSlot) continue;
            boolean inventory = slot.container == player.getInventory();
            int index = slot.getContainerSlot();
            if (playerSide ? !inventory || index < 0 || index >= inventorySize : inventory) continue;
            if (!addresses.computeIfAbsent(slot.container, ignored -> new java.util.HashSet<>()).add(index)) return false;
            ItemStack item = slot.getItem();
            if (item.getItem() instanceof TieredBackpackItem) {
                if (!identities.add(item)) return false;
                bags.add(new Bag(slot, item));
            } else if (!(item.getItem() instanceof AbstractBackpackItem) && (!playerSide || index >= 9) && (pinned.isEmpty() || item != pinned)) {
                loose.add(slot);
            }
        }
        // Vanilla storage has at most 54 slots, player inventory at most 36.
        if (loose.size() > 54 || bags.size() > 54 || loose.isEmpty()) return false;
        List<ItemStack> before = loose.stream().map(slot -> slot.getItem().copy()).toList();
        List<ItemStack> remaining = new ArrayList<>();
        before.forEach(item -> remaining.add(item.copy()));
        for (int i = 0; i < loose.size(); i++) {
            if (!validSource(loose.get(i), player) || !validCount(before.get(i))) return false;
        }
        try {
            for (Bag bag : bags) {
                if (!validSource(bag.slot, player) || bag.capacity < 1 || bag.capacity > 72
                        || bag.contents.stream().anyMatch(item -> !validCount(item))) return false;
                for (ItemStack incoming : remaining) {
                    if (incoming.isEmpty() || incoming.getItem() instanceof AbstractBackpackItem
                            || bag.originalContents.stream().noneMatch(item -> !item.isEmpty()
                            && ItemStack.isSameItemSameComponents(item, incoming))
                            || !PortableContainerPolicy.mayInsertIntoBackpack(player.level(), incoming)) continue;
                    for (int pass = 0; pass < 2 && !incoming.isEmpty(); pass++) {
                        for (int slot = 0; slot < bag.capacity && !incoming.isEmpty(); slot++) {
                            ItemStack stored = bag.contents.get(slot);
                            if (pass == 0 ? stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, incoming)
                                    : !stored.isEmpty()) continue;
                            int amount = Math.min(incoming.getCount(), incoming.getMaxStackSize() - stored.getCount());
                            if (amount > 0) {
                                bag.contents.set(slot, incoming.copyWithCount(stored.getCount() + amount));
                                incoming.shrink(amount);
                                bag.changed = true;
                            }
                        }
                    }
                }
                if (bag.changed) bag.prepare(player);
            }
        } catch (RuntimeException failure) {
            return false; // Staging never touches the live inventory or backpack components.
        }
        List<ItemStack> proposal = BackpackActions.sorted(remaining);
        if (proposal.size() != loose.size()) return false;
        for (int i = 0; i < loose.size(); i++) {
            Slot slot = loose.get(i);
            if (!ItemStack.matches(slot.getItem(), before.get(i)) || !validDestination(slot, proposal.get(i), player)) return false;
        }
        for (Bag bag : bags) {
            if (bag.slot.getItem() != bag.stack || !ItemStack.matches(bag.stack, bag.before)
                    || !validDestination(bag.slot, bag.staged, player)) return false;
        }
        if (!menu.stillValid(player)) return false;
        try {
            for (int i = 0; i < loose.size(); i++) loose.get(i).setByPlayer(proposal.get(i).copy());
            for (Bag bag : bags) if (bag.changed) {
                bag.stack.set(DataComponents.CONTAINER, bag.staged.get(DataComponents.CONTAINER));
                bag.slot.setChanged();
            }
            refreshViews(player);
        } catch (RuntimeException failure) {
            // Bags keep their exact original object/slot throughout the transaction.
            for (Bag bag : bags) bag.stack.set(DataComponents.CONTAINER, bag.before.get(DataComponents.CONTAINER));
            for (int i = 0; i < loose.size(); i++) {
                try {
                    loose.get(i).setByPlayer(before.get(i).copy());
                } catch (RuntimeException callbackFailure) {
                    // Vanilla backing containers store before notifying listeners. A
                    // failing listener must not prevent restoration of later slots.
                    // Keep restoring even if a listener rethrows the same exception instance.
                }
            }
            refreshViews(player);
            return false;
        }
        player.getInventory().setChanged();
        menu.broadcastChanges();
        return true;
    }

    private static boolean validCount(ItemStack item) {
        return item.isEmpty() || item.getCount() <= item.getMaxStackSize();
    }
    private static boolean validSource(Slot slot, ServerPlayer player) {
        return slot.isActive() && slot.mayPickup(player);
    }
    private static boolean validDestination(Slot slot, ItemStack item, ServerPlayer player) {
        return validSource(slot, player) && (item.isEmpty() || slot.mayPlace(item)
                && slot.container.canPlaceItem(slot.getContainerSlot(), item)
                && item.getCount() <= slot.getMaxStackSize(item)
                && item.getCount() <= slot.container.getMaxStackSize(item));
    }
    static void refreshViews(ServerPlayer player) {
        if (player.inventoryMenu instanceof BackpackPanelMenuAccess access)
            access.totem$getBackpackPanel().refreshAfterExternalCommit();
        if (player.containerMenu != player.inventoryMenu && BackpackPanelMenuAccess.supports(player.containerMenu) && player.containerMenu instanceof BackpackPanelMenuAccess access)
            access.totem$getBackpackPanel().refreshAfterExternalCommit();
        if (player.containerMenu instanceof BackpackMenu menu && menu.storage() != null)
            menu.storage().refreshAfterExternalCommit();
    }

    private static final class Bag {
        final Slot slot;
        final ItemStack stack;
        final ItemStack before;
        final int capacity;
        final List<ItemStack> originalContents;
        final List<ItemStack> contents;
        ItemStack staged;
        boolean changed;
        Bag(Slot slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
            this.before = stack.copy();
            this.capacity = BackpackCapacity.configuredSlots(stack);
            this.originalContents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).itemCopies().toList();
            this.contents = new ArrayList<>();
            originalContents.forEach(item -> contents.add(item.copy()));
            while (contents.size() < capacity) contents.add(ItemStack.EMPTY);
            this.staged = before.copy();
        }
        void prepare(ServerPlayer player) {
            staged.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
            // Existing compression only handles plain recipe ingredients. Preserve
            // orphaned rows and custom component-bearing items verbatim.
            if (originalContents.size() <= capacity && contents.stream().allMatch(item -> item.isEmpty()
                    || ItemStack.isSameItemSameComponents(item, new ItemStack(item.getItem())))) {
                BackpackCompaction.compactIfEnabled(player.level(), staged);
            }
        }
    }
}
