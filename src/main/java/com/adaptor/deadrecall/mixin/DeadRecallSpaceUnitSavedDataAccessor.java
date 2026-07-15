package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.space.DeadRecallSpaceUnitSavedData;
import com.adaptor.deadrecall.space.SpaceStructureSnapshot;
import com.adaptor.deadrecall.space.SpaceUnitRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;
import java.util.UUID;

@Mixin(DeadRecallSpaceUnitSavedData.class)
public interface DeadRecallSpaceUnitSavedDataAccessor {
    @Accessor("unitsById")
    Map<UUID, SpaceUnitRecord> deadrecall$getUnitsById();

    @Invoker("scanStructure")
    static SpaceStructureSnapshot deadrecall$invokeScanStructure(ServerLevel level, BlockPos lodestonePos) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
