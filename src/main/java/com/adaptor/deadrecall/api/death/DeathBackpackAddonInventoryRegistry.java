package com.adaptor.deadrecall.api.death;

import net.minecraft.resources.Identifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Public compatibility registry for optional addon inventory integrations. */
public final class DeathBackpackAddonInventoryRegistry {
    private static final Map<Identifier, DeathBackpackAddonInventoryProvider> PROVIDERS = new LinkedHashMap<>();
    private DeathBackpackAddonInventoryRegistry() { }
    public static synchronized void register(DeathBackpackAddonInventoryProvider provider) {
        Objects.requireNonNull(provider, "provider");
        Identifier id = Objects.requireNonNull(provider.id(), "provider.id()");
        if (PROVIDERS.putIfAbsent(id, provider) != null) throw new IllegalArgumentException("Duplicate death backpack addon inventory provider: " + id);
    }
    public static synchronized List<DeathBackpackAddonInventoryProvider> providers() { return List.copyOf(PROVIDERS.values()); }
}
