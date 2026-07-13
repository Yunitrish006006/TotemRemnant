package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DiscordBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(WhitelistCommand.class)
public abstract class WhitelistCommandMixin {
    @Inject(method = "addPlayers", at = @At("RETURN"))
    private static void deadrecall$notifyWhitelistAdd(
            CommandSourceStack source,
            Collection<NameAndId> targets,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (cir.getReturnValueI() > 0) {
            DiscordBridge.sendAdminAction(DiscordMixinFormatting.actor(source), "whitelist add", DiscordMixinFormatting.names(targets));
        }
    }

    @Inject(method = "removePlayers", at = @At("RETURN"))
    private static void deadrecall$notifyWhitelistRemove(
            CommandSourceStack source,
            Collection<NameAndId> targets,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (cir.getReturnValueI() > 0) {
            DiscordBridge.sendAdminAction(DiscordMixinFormatting.actor(source), "whitelist remove", DiscordMixinFormatting.names(targets));
        }
    }

    @Inject(method = "enableWhitelist", at = @At("RETURN"))
    private static void deadrecall$notifyWhitelistEnabled(
            CommandSourceStack source,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (cir.getReturnValueI() > 0) {
            DiscordBridge.sendAdminAction(DiscordMixinFormatting.actor(source), "whitelist", "enabled");
        }
    }

    @Inject(method = "disableWhitelist", at = @At("RETURN"))
    private static void deadrecall$notifyWhitelistDisabled(
            CommandSourceStack source,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (cir.getReturnValueI() > 0) {
            DiscordBridge.sendAdminAction(DiscordMixinFormatting.actor(source), "whitelist", "disabled");
        }
    }
}
