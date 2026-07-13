package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DiscordBridge;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "op(Lnet/minecraft/server/players/NameAndId;Ljava/util/Optional;Ljava/util/Optional;)V", at = @At("RETURN"))
    private void deadrecall$notifyOp(
            NameAndId profile,
            Optional<LevelBasedPermissionSet> permissionLevel,
            Optional<Boolean> bypassPlayerLimit,
            CallbackInfo ci
    ) {
        DiscordBridge.sendAdminAction("server", "op", profile.name());
    }

    @Inject(method = "deop", at = @At("RETURN"))
    private void deadrecall$notifyDeop(NameAndId profile, CallbackInfo ci) {
        DiscordBridge.sendAdminAction("server", "deop", profile.name());
    }
}
