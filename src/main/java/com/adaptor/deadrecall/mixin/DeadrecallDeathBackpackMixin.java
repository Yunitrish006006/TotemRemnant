package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.death.DeathBackpackCaptureService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables the legacy nearby-ItemEntity death collector.
 *
 * <p>Direct pre-drop capture is now the only death-backpack creation path. If that transactional
 * capture fails, restored inventory is left to vanilla {@code Inventory.dropAll()} rather than
 * scanning nearby world entities and risking cross-player or unrelated-item collection.</p>
 */
@Mixin(Deadrecall.class)
public abstract class DeadrecallDeathBackpackMixin {
    @Inject(method = "rememberExistingDropsBeforeDeath", at = @At("HEAD"), cancellable = true)
    private static void deadrecall$disableLegacyPreDeathItemScan(ServerPlayer player, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "handlePlayerDeath", at = @At("HEAD"), cancellable = true)
    private void deadrecall$disableLegacyPostDeathCollector(ServerPlayer player, CallbackInfo ci) {
        DeathBackpackCaptureService.consumeCompletedCapture(player.getUUID());
        ci.cancel();
    }
}
