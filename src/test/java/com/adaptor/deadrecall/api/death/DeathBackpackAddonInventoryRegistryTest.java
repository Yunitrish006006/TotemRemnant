package com.adaptor.deadrecall.api.death;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeathBackpackAddonInventoryRegistryTest {
    @Test
    void rejectsDuplicateProviderIdsAndRetainsRegisteredProvider() {
        DeathBackpackAddonInventoryProvider provider = new DeathBackpackAddonInventoryProvider() {
            @Override public Identifier id() { return Identifier.fromNamespaceAndPath("remnant_test", "slots"); }
            @Override public List<? extends DeathBackpackAddonSlot> collectDroppableSlots(net.minecraft.server.level.ServerPlayer player) { return List.of(); }
        };

        DeathBackpackAddonInventoryRegistry.register(provider);
        assertEquals(List.of(provider), DeathBackpackAddonInventoryRegistry.providers());
        assertThrows(IllegalArgumentException.class, () -> DeathBackpackAddonInventoryRegistry.register(provider));
    }
}
