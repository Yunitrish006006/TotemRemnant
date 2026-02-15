package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DeathLocationManager;
import com.adaptor.deadrecall.DiscordBridge;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeathRecord(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity)(Object)this;
        BlockPos pos = serverPlayer.getBlockPos();
        World world = serverPlayer.getWorld();

        // 记录死亡座标
        DeathLocationManager.setDeathLocation(serverPlayer, pos, world);

        // 发送死亡消息到 Discord
        String playerName = serverPlayer.getName().getString();
        String deathMessage = source.getDeathMessage(serverPlayer).getString();
        String location = String.format("座標: X=%d, Y=%d, Z=%d", pos.getX(), pos.getY(), pos.getZ());
        String fullMessage = String.format("💀 %s\n📍 %s", deathMessage, location);

        DiscordBridge.sendChatMessage(playerName, fullMessage);
    }
}

