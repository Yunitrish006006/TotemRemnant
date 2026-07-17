package com.adaptor.deadrecall;

import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryProvider;
import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Registers optional addon inventory adapters without linking their classes when the addon is absent. */
public final class DeathBackpackAddonIntegrationInitializer implements ModInitializer {
    private static final String TRINKETS_PROVIDER_CLASS =
            "com.adaptor.deadrecall.integration.trinkets.TrinketsDeathBackpackInventoryProvider";

    @Override
    public void onInitialize() {
        if (!FabricLoader.getInstance().isModLoaded("trinkets_updated")) {
            return;
        }

        try {
            Class<?> providerClass = Class.forName(TRINKETS_PROVIDER_CLASS);
            DeathBackpackAddonInventoryProvider provider =
                    (DeathBackpackAddonInventoryProvider) providerClass.getDeclaredConstructor().newInstance();
            DeathBackpackAddonInventoryRegistry.register(provider);
            Deadrecall.LOGGER.info("Enabled death-backpack inventory integration for Trinkets Updated");
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Could not initialize Trinkets Updated death-backpack integration", exception);
        }
    }
}
