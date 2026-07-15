package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.space.AmethystCatalystDiscount;
import com.adaptor.deadrecall.space.DeadRecallSpaceUnitSavedData;
import com.adaptor.deadrecall.space.SpaceUnitHandler;
import com.adaptor.deadrecall.space.SpaceUnitRecord;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpaceUnitHandler.class)
public abstract class SpaceUnitHandlerCatalystMixin {
    @Redirect(
            method = "calculateTeleportQuote",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I")
    )
    private static int deadrecall$applyAmethystCatalystDiscount(
            int minimumCost,
            int calculatedCost,
            ServerPlayer player,
            @Coerce Object source,
            @Coerce Object target
    ) {
        int baseCost = Math.max(minimumCost, calculatedCost);
        MinecraftServer server = player.level().getServer();
        DeadRecallSpaceUnitSavedData units = server.overworld()
                .getDataStorage()
                .computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);

        SpaceUnitMapSourceAccessor sourceAccessor = (SpaceUnitMapSourceAccessor) source;
        SpaceUnitTeleportTargetAccessor targetAccessor = (SpaceUnitTeleportTargetAccessor) target;

        int sourceCatalysts = 0;
        if (SpaceUnitHandler.SOURCE_TYPE_LODESTONE.equals(sourceAccessor.deadrecall$getType())) {
            sourceCatalysts = catalystBlocks(units, sourceAccessor.deadrecall$getId());
        }

        int targetCatalysts = targetAccessor.deadrecall$isLodestoneAnchor()
                ? catalystBlocks(units, targetAccessor.deadrecall$getId())
                : 0;

        return AmethystCatalystDiscount.finalCost(baseCost, sourceCatalysts, targetCatalysts);
    }

    private static int catalystBlocks(DeadRecallSpaceUnitSavedData units, java.util.UUID unitId) {
        return units.get(unitId)
                .filter(SpaceUnitRecord::isLodestoneAnchor)
                .map(SpaceUnitRecord::structure)
                .map(snapshot -> snapshot.amethystCatalystBlocks())
                .orElse(0);
    }
}
