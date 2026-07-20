package com.adaptor.deadrecall.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;

public final class TotemRemnantItemGroupRegistration {
    private TotemRemnantItemGroupRegistration() {
    }

    public static void addItems(FabricCreativeModeTabOutput output) {
        DeadRecallItemGroupSupport.add(output, TotemRemnantItemRegistration.BACKPACK_BASIC);
        DeadRecallItemGroupSupport.add(output, TotemRemnantItemRegistration.BACKPACK_STANDARD);
        DeadRecallItemGroupSupport.add(output, TotemRemnantItemRegistration.BACKPACK_ADVANCED);
        DeadRecallItemGroupSupport.add(output, TotemRemnantItemRegistration.BACKPACK_NETHERITE);
        DeadRecallItemGroupSupport.add(output, TotemRemnantItemRegistration.DEATH_BACKPACK);
    }
}
