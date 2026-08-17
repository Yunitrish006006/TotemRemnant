package dev.totem.remnant.item;

import dev.totem.remnant.registry.RemnantGameRules;
import net.minecraft.world.item.Item;

/** Base item for Remnant backpacks; blocks nesting in vanilla portable containers. */
public abstract class AbstractBackpackItem extends Item {
    protected AbstractBackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return !RemnantGameRules.preventPortableContainerNesting();
    }
}
