package com.adaptor.deadrecall.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;

public final class LegacyGameplayItemGroupRegistration {
    private LegacyGameplayItemGroupRegistration() {
    }

    public static void addItems(FabricCreativeModeTabOutput output) {
        DeadRecallItemGroupSupport.add(output, LegacyGameplayItemRegistration.SALTPETER);
        DeadRecallItemGroupSupport.add(output, LegacyGameplayItemRegistration.PIG_MANURE);
        DeadRecallItemGroupSupport.add(output, LegacyGameplayItemRegistration.WOOD_ASH);
        DeadRecallItemGroupSupport.add(output, LegacyGameplayItemRegistration.COCOA_POWDER);
        DeadRecallItemGroupSupport.add(output, LegacyGameplayItemRegistration.HOT_COCOA);
        DeadRecallItemGroupSupport.add(output, LegacyGameplayItemRegistration.CHERRY_BREW);
        DeadRecallItemGroupSupport.add(output, LegacyGameplayItemRegistration.STONE_BOWL);
        DeadRecallItemGroupSupport.add(output, LegacyGameplayItemRegistration.SULFUR_BOWL);
    }
}
