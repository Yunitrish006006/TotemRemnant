package com.adaptor.deadrecall.space;

import net.minecraft.world.level.saveddata.maps.MapId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeleportInterfaceItemResolverTest {
    @Test
    void resolvedIdentityEnforcesMapIdInvariant() {
        TeleportInterfaceItemResolver.ResolvedInterface compass =
                new TeleportInterfaceItemResolver.ResolvedInterface(
                        TeleportInterfaceType.COMPASS,
                        null
                );
        assertNull(compass.mapId());
        assertThrows(
                IllegalArgumentException.class,
                () -> new TeleportInterfaceItemResolver.ResolvedInterface(
                        TeleportInterfaceType.FILLED_MAP,
                        null
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new TeleportInterfaceItemResolver.ResolvedInterface(
                        TeleportInterfaceType.BOOK,
                        new MapId(7)
                )
        );
    }

}
