package com.adaptor.deadrecall;

import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryRegistry;
import com.adaptor.deadrecall.integration.trinkets.TrinketsDeathBackpackInventoryProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Registers optional addon inventory adapters without making those mods hard dependencies. */
public final class DeathBackpackAddonIntegrationInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("trinkets_updated")) {
            DeathBackpackAddonInventoryRegistry.register(new TrinketsDeathBackpackInventoryProvider());
            Deadrecall.LOGGER.info("Enabled death-backpack inventory integration for Trinkets Updated");
        }
    }
}
