package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DiscordBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(PardonCommand.class)
public abstract class PardonCommandMixin {
    @Inject(method = "pardonPlayers", at = @At("RETURN"))
    private static void deadrecall$notifyPardonPlayers(
            CommandSourceStack source,
            Collection<NameAndId> targets,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (cir.getReturnValueI() > 0) {
            DiscordBridge.sendAdminAction(DiscordMixinFormatting.actor(source), "pardon", DiscordMixinFormatting.names(targets));
        }
    }
}
