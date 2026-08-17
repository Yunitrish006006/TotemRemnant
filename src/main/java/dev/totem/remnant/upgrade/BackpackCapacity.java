package dev.totem.remnant.upgrade;

import dev.totem.remnant.item.TieredBackpackItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Resolves ordinary backpack storage after removable capacity modules. */
public final class BackpackCapacity {
    public static final int SLOTS_PER_MODULE = 9;

    private BackpackCapacity() {
    }

    public static int slots(ItemStack backpack) {
        int configured = configuredSlots(backpack);
        if (configured == 0) return 0;
        int storedFootprint = (int) backpack.getOrDefault(
                DataComponents.CONTAINER, ItemContainerContents.EMPTY).allItemsCopyStream().count();
        int recoverable = (storedFootprint + SLOTS_PER_MODULE - 1) / SLOTS_PER_MODULE
                * SLOTS_PER_MODULE;
        // Never hide legacy/orphaned contents. Once those rows are emptied, fromItems trims them
        // and the backpack naturally returns to its module-configured capacity.
        return Math.max(configured, recoverable);
    }

    public static int configuredSlots(ItemStack backpack) {
        if (!(backpack.getItem() instanceof TieredBackpackItem tiered)) {
            return 0;
        }
        int slots = tiered.tier().slots();
        int modules = BackpackUpgradeData.count(backpack, BackpackUpgradeType.CAPACITY);
        return slots + modules * SLOTS_PER_MODULE;
    }

    public static int rows(ItemStack backpack) {
        return slots(backpack) / 9;
    }
}
