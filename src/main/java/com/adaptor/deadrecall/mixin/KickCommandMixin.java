package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DiscordBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(KickCommand.class)
public abstract class KickCommandMixin {
    @Inject(method = "kickPlayers", at = @At("RETURN"))
    private static void deadrecall$notifyKickPlayers(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            Component reason,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (cir.getReturnValueI() > 0) {
            DiscordBridge.sendAdminAction(DiscordMixinFormatting.actor(source), "kick", DiscordMixinFormatting.playerNames(targets));
        }
    }
}
