package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.Deadrecall;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerPublishMixin {
    @Inject(method = "publishServer", at = @At("RETURN"))
    private void deadrecall$notifyPublishedServer(MinecraftServer.MultiplayerScope scope, GameType gameMode, boolean cheats, int port, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            Deadrecall.notifyServerOpened((MinecraftServer) (Object) this, false);
        }
    }

    @Inject(method = "unpublishServer", at = @At("RETURN"))
    private void deadrecall$notifyUnpublishedServer(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            Deadrecall.notifyServerClosed((MinecraftServer) (Object) this, true);
        }
    }
}
