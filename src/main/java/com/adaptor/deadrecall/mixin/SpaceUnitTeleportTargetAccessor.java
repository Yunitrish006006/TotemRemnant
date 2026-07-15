package com.adaptor.deadrecall.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(targets = "com.adaptor.deadrecall.space.SpaceUnitHandler$TeleportTarget")
public interface SpaceUnitTeleportTargetAccessor {
    @Accessor("id")
    UUID deadrecall$getId();

    @Accessor("lodestoneAnchor")
    boolean deadrecall$isLodestoneAnchor();
}
