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

    public static boolean sort(ServerPlayer player, boolean playerSide) {
        AbstractContainerMenu menu = player.containerMenu;
        if (player.isSpectator() || !player.isAlive() || !menu.stillValid(player)) return false;
        BackpackMenu backpack = menu instanceof BackpackMenu b ? b : null;
        BackpackPanelContainer panel = menu == player.inventoryMenu && menu instanceof BackpackPanelMenuAccess a
                ? a.totem$getBackpackPanel() : null;
        if (backpack == null && panel == null && !MatchingBackpackSort.isVanillaChest(menu)) return false;
        ItemStack pinned = backpack != null ? backpack.trackedBackpack()
                : panel != null ? panel.selectedBackpack() : ItemStack.EMPTY;
        if (backpack != null && (backpack.storage() == null || !backpack.storage().contentsCurrent())) return false;
        if (playerSide || MatchingBackpackSort.isVanillaChest(menu))
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
        for (int i = 0; i < targets.size(); i++) {
            Slot slot = targets.get(i);
            ItemStack item = proposal.get(i);
            if (!slot.isActive() || !slot.mayPickup(player) || (!item.isEmpty()
                    && (!slot.mayPlace(item) || !slot.container.canPlaceItem(slot.getContainerSlot(), item)
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
