package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.death.DeathBackpackCaptureService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerDeathBackpackMixin {
    @Inject(
            method = "dropEquipment",
            at = @At("HEAD")
    )
    private void deadrecall$captureInventoryBeforeVanillaDeathDrops(ServerLevel level, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer serverPlayer) {
            DeathBackpackCaptureService.captureBeforeVanillaDrop(serverPlayer, level);
        }
    }
}
