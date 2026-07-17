package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.network.SpaceUnitMapPayload;
import com.adaptor.deadrecall.space.AmethystCatalystDiscount;
import com.adaptor.deadrecall.space.DeadRecallSpaceUnitSavedData;
import com.adaptor.deadrecall.space.SpaceUnitHandler;
import com.adaptor.deadrecall.space.SpaceUnitRecord;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Adds catalyst-specific quote details after the authoritative map payload has been built. */
@Mixin(SpaceUnitHandler.class)
public abstract class SpaceUnitMapCatalystBreakdownMixin {
    private static final int BASE_CROSS_DIMENSION_COST = 2;

    @Inject(method = "buildMapPayload", at = @At("RETURN"), cancellable = true)
    private static void deadrecall$appendCatalystBreakdown(
            ServerPlayer player,
            @Coerce Object source,
            List<SpaceUnitRecord> visibleUnits,
            CallbackInfoReturnable<SpaceUnitMapPayload> cir
    ) {
        SpaceUnitMapPayload payload = cir.getReturnValue();
        if (payload == null || payload.entries().isEmpty()) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        DeadRecallSpaceUnitSavedData units = server.overworld()
                .getDataStorage()
                .computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);
        boolean sourceLodestone = SpaceUnitHandler.SOURCE_TYPE_LODESTONE.equals(payload.sourceType());
        int sourceCatalysts = deadrecall$storedCatalystBlocks(units, payload.sourceUnitId());

        List<SpaceUnitMapPayload.Entry> enriched = new ArrayList<>(payload.entries().size());
        for (SpaceUnitMapPayload.Entry entry : payload.entries()) {
            boolean crossDimension = !payload.sourceDimension().equals(entry.dimension());
            boolean targetLodestone = SpaceUnitHandler.SOURCE_TYPE_LODESTONE.equals(entry.type());
            int baseCost = crossDimension
                    ? Math.max(BASE_CROSS_DIMENSION_COST,
                    BASE_CROSS_DIMENSION_COST + (int) Math.ceil((1.0D - entry.resonance()) * 4.0D))
                    : 0;
            AmethystCatalystDiscount.Quote quote = AmethystCatalystDiscount.quoteForEndpoints(
                    baseCost,
                    sourceLodestone,
                    sourceCatalysts,
                    targetLodestone,
                    deadrecall$storedCatalystBlocks(units, entry.id())
            );

            enriched.add(new SpaceUnitMapPayload.Entry(
                    entry.id(),
                    entry.type(),
                    entry.name(),
                    entry.visibility(),
                    entry.friendShared(),
                    entry.dimension(),
                    entry.x(),
                    entry.y(),
                    entry.z(),
                    entry.resonance(),
                    entry.tier(),
                    entry.distanceBlocks(),
                    entry.saturationCost(),
                    entry.hungerCost(),
                    entry.foodPointsNeeded(),
                    entry.safeFoodPointsAvailable(),
                    entry.amethystCost(),
                    entry.amethystAvailable(),
                    quote.baseCost(),
                    quote.sourceCatalysts(),
                    quote.targetCatalysts(),
                    quote.appliedDiscount(),
                    entry.prepareTicks(),
                    entry.maxHorizontalDeviation(),
                    entry.damageChancePercent(),
                    entry.favorite(),
                    entry.manageable(),
                    entry.owned(),
                    entry.administratorCount(),
                    entry.allowedPlayerCount(),
                    entry.canTeleport(),
                    entry.blockedReason()
            ));
        }

        cir.setReturnValue(new SpaceUnitMapPayload(
                payload.sourceUnitId(),
                payload.sourceType(),
                payload.sourceName(),
                payload.sourceDimension(),
                payload.sourceX(),
                payload.sourceY(),
                payload.sourceZ(),
                List.copyOf(enriched)
        ));
    }

    private static int deadrecall$storedCatalystBlocks(
            DeadRecallSpaceUnitSavedData units,
            java.util.UUID unitId
    ) {
        return units.get(unitId)
                .map(SpaceUnitRecord::structure)
                .map(snapshot -> snapshot.amethystCatalystBlocks())
                .orElse(0);
    }
}
