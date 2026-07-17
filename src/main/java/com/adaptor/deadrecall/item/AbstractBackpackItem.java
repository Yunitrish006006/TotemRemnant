package com.adaptor.deadrecall.item;

import net.minecraft.world.item.Item;

/**
 * Shared base for every DeadRecall backpack item.
 *
 * <p>Vanilla portable item containers, including bundles and shulker boxes, consult
 * {@link #canFitInsideContainerItems()} before accepting a stack. Returning {@code false}
 * makes the restriction server authoritative for direct interaction and automated transfer
 * paths that honor the target container's insertion rules.</p>
 */
public abstract class AbstractBackpackItem extends Item {
    protected AbstractBackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
