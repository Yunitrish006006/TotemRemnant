package com.adaptor.deadrecall.api.death;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeathBackpackAddonInventoryRegistryTest {
    @BeforeEach
    void setUp() {
        DeathBackpackAddonInventoryRegistry.clearForTesting();
    }

    @AfterEach
    void tearDown() {
        DeathBackpackAddonInventoryRegistry.clearForTesting();
    }

    @Test
    void preservesRegistrationOrderAndReturnsImmutableSnapshot() {
        TestProvider first = new TestProvider("first");
        TestProvider second = new TestProvider("second");

        DeathBackpackAddonInventoryRegistry.register(first);
        DeathBackpackAddonInventoryRegistry.register(second);

        List<DeathBackpackAddonInventoryProvider> providers =
                DeathBackpackAddonInventoryRegistry.providers();
        assertEquals(List.of(first, second), providers);
        assertThrows(UnsupportedOperationException.class, () -> providers.add(first));
    }

    @Test
    void rejectsDuplicateProviderIds() {
        DeathBackpackAddonInventoryRegistry.register(new TestProvider("duplicate"));
        assertThrows(
                IllegalArgumentException.class,
                () -> DeathBackpackAddonInventoryRegistry.register(new TestProvider("duplicate"))
        );
    }

    private record TestProvider(Identifier id) implements DeathBackpackAddonInventoryProvider {
        private TestProvider(String path) {
            this(Identifier.fromNamespaceAndPath("deadrecall_test", path));
        }

        @Override
        public List<? extends DeathBackpackAddonSlot> collectDroppableSlots(ServerPlayer player) {
            return List.of();
        }
    }
}
