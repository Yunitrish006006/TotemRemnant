package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DiscordBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

@Mixin(Raid.class)
public abstract class RaidMixin {
    @Unique
    private boolean deadrecall$endedNotified = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void deadrecall$notifyRaidEnded(ServerLevel level, CallbackInfo ci) {
        Raid raid = (Raid) (Object) this;
        if (deadrecall$endedNotified || !raid.isOver()) {
            return;
        }

        deadrecall$endedNotified = true;
        OptionalInt id = level.getRaids().getId(raid);
        String raidKey = level.dimension().identifier() + ":" + id.orElse(System.identityHashCode(raid));
        DiscordBridge.sendRaidEnded(raidKey, raidResult(raid));
    }

    @Unique
    private static String raidResult(Raid raid) {
        if (raid.isVictory()) {
            return "victory";
        }
        if (raid.isLoss()) {
            return "defeat";
        }
        if (raid.isStopped()) {
            return "stopped";
        }
        return "ended";
    }
}
