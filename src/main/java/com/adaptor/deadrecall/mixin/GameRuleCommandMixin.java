package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DiscordBridge;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.world.level.gamerules.GameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRuleCommand.class)
public abstract class GameRuleCommandMixin {
    @Inject(method = "setRule", at = @At("RETURN"))
    private static <T> void deadrecall$notifyGameRuleChanged(
            CommandContext<CommandSourceStack> context,
            GameRule<T> rule,
            CallbackInfoReturnable<Integer> cir
    ) {
        CommandSourceStack source = context.getSource();
        T value = context.getArgument("value", rule.valueClass());
        DiscordBridge.sendGameruleChanged(
                DiscordMixinFormatting.actor(source),
                rule.id(),
                rule.serialize(value)
        );
    }
}
