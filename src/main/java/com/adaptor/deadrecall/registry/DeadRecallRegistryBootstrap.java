package com.adaptor.deadrecall.registry;

import com.adaptor.deadrecall.advancement.ModCriteriaTriggers;
import com.adaptor.deadrecall.block.ModBlocks;
import com.adaptor.deadrecall.block.entity.ModBlockEntities;
import com.adaptor.deadrecall.effect.ModMobEffects;
import com.adaptor.deadrecall.item.ModItemGroups;
import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.menu.ModMenus;

/**
 * Composes registry owners in the legacy all-in-one registration order.
 */
public final class DeadRecallRegistryBootstrap {
    private DeadRecallRegistryBootstrap() {
    }

    public static void registerContent() {
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerModBlockEntities();
        ModMobEffects.registerModEffects();

        LegacyGameplayCriteriaRegistration.register();
        TotemAutomataCriteriaRegistration.register();
        ModCriteriaTriggers.registerModCriteriaTriggers();

        TotemAutomataMenuRegistration.register();
        ModMenus.registerModMenus();

        TotemRemnantItemRegistration.register();
        LegacyGameplayItemRegistration.register();
        TotemAutomataItemRegistration.register();
        ModItems.registerModItems();

        ModItemGroups.registerModItemGroups();
    }
}
