package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.discord.DiscordEventNotifications;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.DifficultyCommand;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DifficultyCommand.class)
public abstract class DifficultyCommandMixin {
    @Inject(method = "setDifficulty", at = @At("RETURN"))
    private static void deadrecall$notifyDifficultyChanged(
            CommandSourceStack source,
            Difficulty difficulty,
            CallbackInfoReturnable<Integer> cir
    ) {
        DiscordEventNotifications.difficultyChanged(
                DiscordMixinFormatting.actor(source),
                difficulty.getSerializedName()
        );
    }
}
