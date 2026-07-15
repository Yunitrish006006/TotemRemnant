package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.death.DeathBackpackCaptureService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

/**
 * Temporary compatibility bridge while the old post-drop collector remains in Deadrecall.
 * Successful pre-drop captures cancel that legacy path; failed captures still fall back to it.
 */
@Mixin(Deadrecall.class)
public abstract class DeadrecallDeathBackpackMixin {
    @Shadow
    @Final
    private static Map<UUID, ?> pendingDeathCollections;

    @Inject(method = "handlePlayerDeath", at = @At("HEAD"), cancellable = true)
    private void deadrecall$skipLegacyCollectorAfterDirectCapture(ServerPlayer player, CallbackInfo ci) {
        UUID playerId = player.getUUID();
        if (!DeathBackpackCaptureService.consumeCompletedCapture(playerId)) {
            return;
        }

        pendingDeathCollections.remove(playerId);
        ci.cancel();
    }
}
