package dev.totem.remnant.mixin;

import dev.totem.remnant.death.DeathBackpackCaptureService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owns the death hook so standalone Remnant captures player inventory before vanilla drops. */
@Mixin(Player.class)
public abstract class PlayerDeathBackpackMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void totemremnant$captureInventoryBeforeVanillaDeathDrops(ServerLevel level, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer player) DeathBackpackCaptureService.captureBeforeVanillaDrop(player, level);
    }
}
