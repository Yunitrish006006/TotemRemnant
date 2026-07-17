package com.adaptor.deadrecall.api.death;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Public registry for optional addon-owned player inventory integrations. */
public final class DeathBackpackAddonInventoryRegistry {
    private static final Map<Identifier, DeathBackpackAddonInventoryProvider> PROVIDERS = new LinkedHashMap<>();

    private DeathBackpackAddonInventoryRegistry() {
    }

    public static synchronized void register(DeathBackpackAddonInventoryProvider provider) {
        Objects.requireNonNull(provider, "provider");
        Identifier id = Objects.requireNonNull(provider.id(), "provider.id()");
        if (PROVIDERS.putIfAbsent(id, provider) != null) {
            throw new IllegalArgumentException("Duplicate death backpack addon inventory provider: " + id);
        }
    }

    public static synchronized List<DeathBackpackAddonInventoryProvider> providers() {
        return List.copyOf(PROVIDERS.values());
    }

    /** Test-only cleanup kept package-private so production integrations cannot unregister each other. */
    static synchronized void clearForTesting() {
        PROVIDERS.clear();
    }

    static synchronized List<Identifier> providerIdsForTesting() {
        return new ArrayList<>(PROVIDERS.keySet());
    }
}
