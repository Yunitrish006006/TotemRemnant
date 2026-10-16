package dev.totem.remnant.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Bounded server-thread operations. Containers commit complete proposals, never per-slot persistence. */
public final class BackpackActions {
    private BackpackActions() { }

    /** Reorder one existing backpack, preserving overflow and every captured nested stack. */
    public static boolean sortHovered(ServerPlayer player, int index, ItemStack expected) {
        var menu = player.containerMenu;
        if (player.isSpectator() || !player.isAlive() || !menu.stillValid(player)
                || (!(menu instanceof BackpackMenu) && !BackpackPanelMenuAccess.supports(menu)
                    && !MatchingBackpackSort.isVanillaChest(menu))
                || index < 0 || index >= menu.slots.size()) return false;
        Slot slot = menu.getSlot(index);
        boolean inventory = slot.container == player.getInventory()
                && (slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36 || slot.getContainerSlot() == 40);
        boolean storage = slot instanceof BackpackPanelSlot
                || menu instanceof BackpackMenu b && index < b.storageSlotCount();
        boolean chest = MatchingBackpackSort.isVanillaChest(menu) && index < ((net.minecraft.world.inventory.ChestMenu) menu).getRowCount() * 9;
        ItemStack bag = slot.getItem();
        if ((!inventory && !storage && !chest) || !slot.isActive() || !slot.mayPickup(player)
                || !BackpackPanelContainer.isBackpack(bag) || !ItemStack.matches(bag, expected)) return false;
        var before = bag.getOrDefault(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY);
        var all = new ArrayList<>(before.itemCopies().limit(1025).toList());
        if (all.size() > 1024 || all.stream().anyMatch(item -> !item.isEmpty()
                && (item.getCount() <= 0 || item.getCount() > item.getMaxStackSize()))) return false;
        int capacity = bag.getItem() instanceof dev.totem.remnant.item.DeathBackpackItem ? 256
                : Math.clamp(dev.totem.remnant.upgrade.BackpackCapacity.configuredSlots(bag), 0, 72);
        if (capacity == 0) return false;
        while (all.size() < capacity) all.add(ItemStack.EMPTY);
        var proposal = sorted(all.subList(0, capacity));
        if (proposal.size() != capacity) return false;
        for (int i = 0; i < capacity; i++) all.set(i, proposal.get(i));
        var staged = bag.copy();
        staged.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(all));
        staged.remove(dev.totem.remnant.registry.RemnantDataComponents.SELECTED_ITEM);
        if (slot.container instanceof DeathBackpackInventory container) {
            var outer = new ArrayList<ItemStack>();
            for (int i = 0; i < container.getContainerSize(); i++) outer.add(container.getItem(i).copy());
            outer.set(slot.getContainerSlot(), staged);
            if (!container.replaceContents(outer)) return false;
        } else if (slot.container instanceof BackpackPanelContainer container) {
            var root = container.selectedBackpack();
            var outer = new ArrayList<ItemStack>();
            for (int i = 0; i < container.activeSlotCount(); i++) outer.add(container.getItem(i).copy());
            outer.set(slot.getContainerSlot(), staged);
            if (!container.replaceActiveContents(outer, root)) return false;
        } else {
            var original = bag.get(DataComponents.CONTAINER);
            var selection = bag.get(dev.totem.remnant.registry.RemnantDataComponents.SELECTED_ITEM);
            try {
                bag.set(DataComponents.CONTAINER, staged.get(DataComponents.CONTAINER));
                bag.remove(dev.totem.remnant.registry.RemnantDataComponents.SELECTED_ITEM);
                slot.setChanged();
            } catch (RuntimeException failure) {
                if (original == null) bag.remove(DataComponents.CONTAINER); else bag.set(DataComponents.CONTAINER, original);
                if (selection == null) bag.remove(dev.totem.remnant.registry.RemnantDataComponents.SELECTED_ITEM);
                else bag.set(dev.totem.remnant.registry.RemnantDataComponents.SELECTED_ITEM, selection);
                try { slot.setChanged(); } catch (RuntimeException ignored) { }
                MatchingBackpackSort.refreshViews(player);
                return false;
            }
        }
        MatchingBackpackSort.refreshViews(player);
        player.getInventory().setChanged(); menu.broadcastChanges();
        return true;
    }

    public static boolean sort(ServerPlayer player, boolean playerSide) {
        return sort(player, playerSide, false);
    }

    public static boolean sort(ServerPlayer player, boolean playerSide, boolean panelSide) {
        AbstractContainerMenu menu = player.containerMenu;
        if (player.isSpectator() || !player.isAlive() || !menu.stillValid(player)) return false;
        BackpackMenu backpack = menu instanceof BackpackMenu b ? b : null;
        BackpackPanelContainer panel = BackpackPanelMenuAccess.supports(menu) && menu instanceof BackpackPanelMenuAccess a
                ? a.totem$getBackpackPanel() : null;
        if (backpack == null && panel == null && !MatchingBackpackSort.isVanillaChest(menu)) return false;
        ItemStack pinned = backpack != null ? backpack.trackedBackpack()
                : panel != null ? panel.selectedBackpack() : ItemStack.EMPTY;
        if (backpack != null && (backpack.storage() == null || !backpack.storage().contentsCurrent())) return false;
        if (playerSide || (!panelSide && MatchingBackpackSort.isVanillaChest(menu)))
            return MatchingBackpackSort.sort(player, playerSide, pinned);
        List<Slot> targets = new ArrayList<>();
        if (playerSide) {
            for (Slot slot : menu.slots) {
                if (slot.container == player.getInventory() && slot.getContainerSlot() >= 9
                        && slot.getContainerSlot() < player.getInventory().getNonEquipmentItems().size()
                        && (pinned.isEmpty() || slot.getItem() != pinned)) targets.add(slot);
            }
        } else if (backpack != null) {
            for (int i = 0; i < backpack.storageSlotCount(); i++) targets.add(menu.getSlot(i));
        } else {
            int start = ((BackpackPanelMenuAccess) menu).totem$getBackpackPanelSlotStart();
            for (int i = 0; i < panel.activeSlotCount(); i++) targets.add(menu.getSlot(start + i));
        }
        if (targets.isEmpty()) return false;
        List<ItemStack> before = targets.stream().map(slot -> slot.getItem().copy()).toList();
        List<ItemStack> proposal = sorted(before);
        if (proposal.size() != targets.size()) return false;
        boolean deathStorage = backpack != null ? backpack.isDeathBackpack()
                : pinned.getItem() instanceof dev.totem.remnant.item.DeathBackpackItem;
        List<ItemStack> capturedBackpacks = new ArrayList<>(before);
        for (int i = 0; i < targets.size(); i++) {
            Slot slot = targets.get(i);
            ItemStack item = proposal.get(i);
            // Death capture can contain a backpack that normal insertion forbids. Sorting may
            // relocate each exact existing stack once, without admitting any new nested stack.
            boolean retainedCapture = false;
            if (deathStorage && item.getItem() instanceof dev.totem.remnant.item.AbstractBackpackItem) {
                for (int j = 0; j < capturedBackpacks.size(); j++) {
                    if (ItemStack.matches(capturedBackpacks.get(j), item)) {
                        capturedBackpacks.set(j, ItemStack.EMPTY);
                        retainedCapture = true;
                        break;
                    }
                }
            }
            if (!slot.isActive() || !slot.mayPickup(player) || (!item.isEmpty()
                    && ((!retainedCapture && (!slot.mayPlace(item) || !slot.container.canPlaceItem(slot.getContainerSlot(), item)))
                    || item.getCount() > slot.getMaxStackSize(item)
                    || item.getCount() > slot.container.getMaxStackSize(item)))) return false;
        }
        if (playerSide) {
            // The backing vanilla Inventory has no per-slot compaction/persistence callbacks.
            for (int i = 0; i < targets.size(); i++) player.getInventory().setItem(targets.get(i).getContainerSlot(), proposal.get(i));
            player.getInventory().setChanged();
        } else if (backpack != null) {
            DeathBackpackInventory storage = backpack.storage();
            List<ItemStack> all = new ArrayList<>();
            for (int i = 0; i < storage.getContainerSize(); i++) all.add(storage.getItem(i).copy());
            for (int i = 0; i < proposal.size(); i++) all.set(i, proposal.get(i));
            if (!storage.replaceContents(all)) return false;
        } else if (!panel.replaceActiveContents(proposal, pinned)) return false;
        menu.broadcastChanges();
        return true;
    }

    static List<ItemStack> sorted(List<ItemStack> before) {
        List<ItemStack> piles = new ArrayList<>();
        for (ItemStack original : before) {
            ItemStack remaining = original.copy();
            for (ItemStack pile : piles) {
                if (ItemStack.isSameItemSameComponents(pile, remaining)) {
                    int amount = Math.min(pile.getMaxStackSize() - pile.getCount(), remaining.getCount());
                    if (amount > 0) { pile.grow(amount); remaining.shrink(amount); }
                }
            }
            while (!remaining.isEmpty()) {
                int amount = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                piles.add(remaining.copyWithCount(amount)); remaining.shrink(amount);
            }
        }
        piles.sort(java.util.Comparator.comparing((ItemStack item) -> BuiltInRegistries.ITEM.getKey(item.getItem()).toString())
                .thenComparing(java.util.Comparator.comparingInt(ItemStack::getCount).reversed()));
        while (piles.size() < before.size()) piles.add(ItemStack.EMPTY);
        return piles;
    }

    public static boolean equip(ServerPlayer player, BackpackMenu menu) {
        DeathBackpackInventory storage = menu.storage();
        if (!menu.isDeathBackpack() || player.containerMenu != menu || player.isSpectator() || !player.isAlive()
                || storage == null || !storage.contentsCurrent() || !menu.stillValid(player)) return false;
        List<ItemStack> before = new ArrayList<>();
        for (int i = 0; i < storage.getContainerSize(); i++) before.add(storage.getItem(i).copy());
        List<ItemStack> after = new ArrayList<>();
        before.forEach(item -> after.add(item.copy()));
        EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        for (int i = 0; i < after.size(); i++) {
            ItemStack item = after.get(i);
            var equippable = item.get(DataComponents.EQUIPPABLE);
            if (item.isEmpty() || equippable == null) continue;
            EquipmentSlot slot = equippable.slot();
            if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST
                    && slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) continue;
            if (!equipment.containsKey(slot) && player.getItemBySlot(slot).isEmpty()
                    && player.isEquippableInSlot(item, slot) && menu.getSlot(i).mayPickup(player)) {
                equipment.put(slot, item.copyWithCount(1));
                item.shrink(1);
            }
        }
        if (equipment.isEmpty()) return false;
        try {
            for (var entry : equipment.entrySet()) player.setItemSlot(entry.getKey(), entry.getValue());
            if (!storage.replaceContents(after)) throw new IllegalStateException("Death backpack changed before equip commit");
        } catch (RuntimeException failure) {
            for (EquipmentSlot slot : equipment.keySet()) player.setItemSlot(slot, ItemStack.EMPTY);
            storage.replaceContents(before);
            return false;
        }
        player.getInventory().setChanged();
        menu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        return true;
    }
}
