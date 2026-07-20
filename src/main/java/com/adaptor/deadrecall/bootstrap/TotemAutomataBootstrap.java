package com.adaptor.deadrecall.bootstrap;

import com.adaptor.deadrecall.item.copper.CopperGolemWrenchHandler;

/**
 * Owns registration for the future TotemAutomata module.
 */
public final class TotemAutomataBootstrap {
    private TotemAutomataBootstrap() {
    }

    public static void register() {
        CopperGolemWrenchHandler.register();
    }
}
