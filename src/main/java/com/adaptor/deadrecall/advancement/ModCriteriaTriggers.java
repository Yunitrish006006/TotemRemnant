package com.adaptor.deadrecall.advancement;

import com.adaptor.deadrecall.Deadrecall;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModCriteriaTriggers {
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
    public static final SimplePlayerCriterionTrigger FIRST_COPPER_GOLEM_BINDING = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES,
            Identifier.fromNamespaceAndPath("deadrecall", "first_copper_golem_binding"),
            new SimplePlayerCriterionTrigger()
    );

    private ModCriteriaTriggers() {
    }

    public static void registerModCriteriaTriggers() {
        Deadrecall.LOGGER.info("正在註冊模組進度觸發器...");
    }
}
