package com.adaptor.deadrecall.registry;

import com.adaptor.deadrecall.item.copper.CopperWrenchItem;
import net.minecraft.world.item.Item;

public final class TotemAutomataItemRegistration {
    public static final Item COPPER_WRENCH = DeadRecallItemRegistrar.register("copper_wrench",
            props -> new CopperWrenchItem(props.stacksTo(1)));

    private TotemAutomataItemRegistration() {
    }

    public static void register() {
        // Class loading registers this owner's items.
    }
}
