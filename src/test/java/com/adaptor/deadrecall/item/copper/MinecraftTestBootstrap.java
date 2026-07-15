package com.adaptor.deadrecall.item.copper;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

final class MinecraftTestBootstrap {
    private MinecraftTestBootstrap() {
    }

    static void bootStrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Bootstrap.validate();
    }
}
