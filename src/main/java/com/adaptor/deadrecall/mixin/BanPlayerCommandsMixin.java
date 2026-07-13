package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DiscordBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(BanPlayerCommands.class)
public abstract class BanPlayerCommandsMixin {
    @Inject(method = "banPlayers", at = @At("RETURN"))
    private static void deadrecall$notifyBanPlayers(
            CommandSourceStack source,
            Collection<NameAndId> targets,
            Component reason,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (cir.getReturnValueI() > 0) {
            DiscordBridge.sendAdminAction(DiscordMixinFormatting.actor(source), "ban", DiscordMixinFormatting.names(targets));
        }
    }
}
