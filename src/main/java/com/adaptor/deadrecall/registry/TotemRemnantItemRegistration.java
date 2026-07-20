package com.adaptor.deadrecall.registry;

import com.adaptor.deadrecall.item.DeathBackpackItem;
import com.adaptor.deadrecall.item.TieredBackpackItem;
import net.minecraft.world.item.Item;

public final class TotemRemnantItemRegistration {
    public static final Item BACKPACK_BASIC = DeadRecallItemRegistrar.register("backpack_basic",
            props -> new TieredBackpackItem(props.stacksTo(1), TieredBackpackItem.BackpackTier.BASIC));

    public static final Item BACKPACK_STANDARD = DeadRecallItemRegistrar.register("backpack_standard",
            props -> new TieredBackpackItem(props.stacksTo(1), TieredBackpackItem.BackpackTier.STANDARD));

    public static final Item BACKPACK_ADVANCED = DeadRecallItemRegistrar.register("backpack_advanced",
            props -> new TieredBackpackItem(props.stacksTo(1), TieredBackpackItem.BackpackTier.ADVANCED));

    public static final Item BACKPACK_NETHERITE = DeadRecallItemRegistrar.register("backpack_netherite",
            props -> new TieredBackpackItem(props.stacksTo(1).fireResistant(), TieredBackpackItem.BackpackTier.NETHERITE));

    public static final Item DEATH_BACKPACK = DeadRecallItemRegistrar.register("death_backpack",
            props -> new DeathBackpackItem(props.stacksTo(1).fireResistant()));

    private TotemRemnantItemRegistration() {
    }

    public static void register() {
        // Class loading registers this owner's items.
    }
}
