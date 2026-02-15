package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DeathLocationManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(PlayerEntity.class)
// 此檔案已被 ServerPlayerEntityMixin 取代，無需再混入 PlayerEntity
public abstract class PlayerEntityMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeathRecord(DamageSource source, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity serverPlayer) {
            BlockPos pos = serverPlayer.getBlockPos();
            World world = serverPlayer.getWorld();
            DeathLocationManager.setDeathLocation(serverPlayer, pos, world);
        }
    }
}
