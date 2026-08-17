package dev.totem.remnant.upgrade;

import dev.totem.remnant.item.TieredBackpackItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/** Persistent, component-safe storage for the removable modules installed in one backpack. */
public final class BackpackUpgradeData {
    private static final String TAG_UPGRADES = "totem_remnant_upgrades";

    private BackpackUpgradeData() {
    }

    public static List<ItemStack> read(ItemStack backpack, int capacity) {
        List<ItemStack> result = new ArrayList<>(capacity);
        ListTag list = backpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getListOrEmpty(TAG_UPGRADES);
        for (int index = 0; index < capacity; index++) {
            String rawId = list.getStringOr(index, "");
            Identifier id = Identifier.tryParse(rawId);
            Item item = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
            result.add(item instanceof BackpackUpgradeItem ? new ItemStack(item) : ItemStack.EMPTY);
        }
        return List.copyOf(result);
    }

    public static void write(ItemStack backpack, List<ItemStack> modules, int capacity) {
        CompoundTag tag = backpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = new ListTag();
        for (int index = 0; index < capacity; index++) {
            ItemStack module = index < modules.size() ? modules.get(index) : ItemStack.EMPTY;
            String id = module.getItem() instanceof BackpackUpgradeItem
                    ? BuiltInRegistries.ITEM.getKey(module.getItem()).toString()
                    : "";
            list.add(StringTag.valueOf(id));
        }
        tag.put(TAG_UPGRADES, list);
        backpack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean has(ItemStack backpack, BackpackUpgradeType type) {
        return count(backpack, type) > 0;
    }

    public static int count(ItemStack backpack, BackpackUpgradeType type) {
        if (!(backpack.getItem() instanceof TieredBackpackItem tiered)) {
            return 0;
        }
        return (int) read(backpack, tiered.tier().upgradeSlots()).stream()
                .filter(stack -> stack.getItem() instanceof BackpackUpgradeItem module
                        && module.type() == type)
                .count();
    }

    public static boolean consume(ItemStack backpack, BackpackUpgradeType type) {
        if (!(backpack.getItem() instanceof TieredBackpackItem tiered)) {
            return false;
        }
        List<ItemStack> modules = new ArrayList<>(read(backpack, tiered.tier().upgradeSlots()));
        for (int index = 0; index < modules.size(); index++) {
            if (modules.get(index).getItem() instanceof BackpackUpgradeItem module
                    && module.type() == type) {
                modules.set(index, ItemStack.EMPTY);
                write(backpack, modules, tiered.tier().upgradeSlots());
                return true;
            }
        }
        return false;
    }
}
