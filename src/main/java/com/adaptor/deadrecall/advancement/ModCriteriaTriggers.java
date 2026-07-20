package com.adaptor.deadrecall.advancement;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.registry.LegacyGameplayCriteriaRegistration;
import com.adaptor.deadrecall.registry.TotemAutomataCriteriaRegistration;

public final class ModCriteriaTriggers {
    public static final SimplePlayerCriterionTrigger PIG_MANURE_HIT_ENTITY =
            LegacyGameplayCriteriaRegistration.PIG_MANURE_HIT_ENTITY;
    public static final SimplePlayerCriterionTrigger PIG_MANURE_GOT_HIT =
            LegacyGameplayCriteriaRegistration.PIG_MANURE_GOT_HIT;
    public static final SimplePlayerCriterionTrigger FIRST_COPPER_GOLEM_BINDING =
            TotemAutomataCriteriaRegistration.FIRST_COPPER_GOLEM_BINDING;

    private ModCriteriaTriggers() {
    }

    public static void registerModCriteriaTriggers() {
        Deadrecall.LOGGER.info("正在註冊模組進度觸發器...");
    }
}
