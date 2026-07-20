package com.adaptor.deadrecall.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;

public final class TotemAutomataItemGroupRegistration {
    private TotemAutomataItemGroupRegistration() {
    }

    public static void addItems(FabricCreativeModeTabOutput output) {
        DeadRecallItemGroupSupport.add(output, TotemAutomataItemRegistration.COPPER_WRENCH);
    }
}
