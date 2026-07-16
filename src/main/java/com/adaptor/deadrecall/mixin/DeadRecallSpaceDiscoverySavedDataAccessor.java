package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.space.DeadRecallSpaceDiscoverySavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(DeadRecallSpaceDiscoverySavedData.class)
public interface DeadRecallSpaceDiscoverySavedDataAccessor {
    @Accessor("discoveredByPlayer")
    Map<UUID, Set<UUID>> deadrecall$getDiscoveredByPlayer();

    @Accessor("favoritesByPlayer")
    Map<UUID, Set<UUID>> deadrecall$getFavoritesByPlayer();
}
