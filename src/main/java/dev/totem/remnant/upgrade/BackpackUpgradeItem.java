package dev.totem.remnant.upgrade;

import net.minecraft.world.item.Item;

/** Item representation of a removable backpack upgrade module. */
public final class BackpackUpgradeItem extends Item {
    private final BackpackUpgradeType type;

    public BackpackUpgradeItem(Properties properties, BackpackUpgradeType type) {
        super(properties);
        this.type = type;
    }

    public BackpackUpgradeType type() {
        return type;
    }
}
