package com.adaptor.deadrecall.registry;

import com.adaptor.deadrecall.advancement.SimplePlayerCriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class TotemAutomataCriteriaRegistration {
    public static final SimplePlayerCriterionTrigger FIRST_COPPER_GOLEM_BINDING = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES,
            Identifier.fromNamespaceAndPath("deadrecall", "first_copper_golem_binding"),
            new SimplePlayerCriterionTrigger()
    );

    private TotemAutomataCriteriaRegistration() {
    }

    public static void register() {
        // Class loading registers this owner's criteria.
    }
}
