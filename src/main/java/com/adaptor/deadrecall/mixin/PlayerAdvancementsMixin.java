package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DiscordBridge;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void deadrecall$notifyAdvancementAwarded(
            AdvancementHolder advancement,
            String criterionName,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ()
                || player == null
                || !(Boolean) player.level().getGameRules().get(GameRules.SHOW_ADVANCEMENT_MESSAGES)) {
            return;
        }

        AdvancementProgress progress = ((PlayerAdvancements) (Object) this).getOrStartProgress(advancement);
        if (!progress.isDone()) {
            return;
        }

        advancement.value().display()
                .filter(DisplayInfo::shouldAnnounceChat)
                .ifPresent(display -> DiscordBridge.sendAdvancement(
                        player.getName().getString(),
                        display.getTitle().getString(),
                        display.getType().getSerializedName()
                ));
    }
}
