package com.adaptor.deadrecall.bootstrap;

import com.adaptor.deadrecall.space.DistributedSpawnHandler;
import com.adaptor.deadrecall.space.SpaceUnitHandler;

/**
 * Owns registration for the future TotemNexus module.
 */
public final class TotemNexusBootstrap {
    private TotemNexusBootstrap() {
    }

    public static void register() {
        DistributedSpawnHandler.register();
        SpaceUnitHandler.register();
    }
}
