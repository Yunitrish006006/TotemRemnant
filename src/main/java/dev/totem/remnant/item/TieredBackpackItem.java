package dev.totem.remnant.item;

import net.minecraft.world.item.Item;

/** Remnant-owned tier metadata for legacy portable backpack identifiers. */
public final class TieredBackpackItem extends AbstractBackpackItem {
    private final BackpackTier tier;
    public TieredBackpackItem(Properties properties, BackpackTier tier) { super(properties); this.tier = tier; }
    public BackpackTier tier() { return tier; }
    public enum BackpackTier {
        BASIC(9), STANDARD(18), ADVANCED(27), NETHERITE(36);
        private final int slots;
        BackpackTier(int slots) { this.slots = slots; }
        public int slots() { return slots; }
    }
}
