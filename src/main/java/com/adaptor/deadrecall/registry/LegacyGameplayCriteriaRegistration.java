package com.adaptor.deadrecall.registry;

import com.adaptor.deadrecall.advancement.SimplePlayerCriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class LegacyGameplayCriteriaRegistration {
    public static final SimplePlayerCriterionTrigger PIG_MANURE_HIT_ENTITY = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES,
            Identifier.fromNamespaceAndPath("deadrecall", "pig_manure_hit_entity"),
            new SimplePlayerCriterionTrigger()
    );

    public static final SimplePlayerCriterionTrigger PIG_MANURE_GOT_HIT = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES,
            Identifier.fromNamespaceAndPath("deadrecall", "pig_manure_got_hit"),
            new SimplePlayerCriterionTrigger()
    );

    private LegacyGameplayCriteriaRegistration() {
    }

    public static void register() {
        // Class loading registers this owner's criteria.
    }
}
