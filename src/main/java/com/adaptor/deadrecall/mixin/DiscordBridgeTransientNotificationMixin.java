package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.DiscordBridge;
import com.adaptor.deadrecall.discord.DiscordTransientBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes allowlisted operational notifications through the temporary-message contract. */
@Mixin(DiscordBridge.class)
public abstract class DiscordBridgeTransientNotificationMixin {
    @Inject(method = "sendPlayerJoined", at = @At("HEAD"), cancellable = true)
    private static void deadrecall$sendTemporaryPlayerJoined(String playerName, CallbackInfo ci) {
        String name = normalize(playerName);
        DiscordTransientBridge.sendEvent("player_join", name, "加入伺服器");
        ci.cancel();
    }

    @Inject(method = "sendPlayerFirstJoined", at = @At("HEAD"), cancellable = true)
    private static void deadrecall$sendTemporaryPlayerFirstJoined(String playerName, CallbackInfo ci) {
        String name = normalize(playerName);
        DiscordTransientBridge.sendEvent("player_first_join", name, "第一次加入伺服器");
        ci.cancel();
    }

    @Inject(method = "sendDeathBackpackCreated", at = @At("HEAD"), cancellable = true)
    private static void deadrecall$sendTemporaryDeathBackpackCreated(String playerName, CallbackInfo ci) {
        String name = normalize(playerName);
        DiscordTransientBridge.sendEvent(
                "death_backpack_created",
                name,
                name.isEmpty() ? "" : name + " 的死亡背包已建立"
        );
        ci.cancel();
    }

    @Inject(method = "sendDeathBackpackRecovered", at = @At("HEAD"), cancellable = true)
    private static void deadrecall$sendTemporaryDeathBackpackRecovered(String playerName, CallbackInfo ci) {
        String name = normalize(playerName);
        DiscordTransientBridge.sendEvent(
                "death_backpack_recovered",
                name,
                name.isEmpty() ? "" : name + " 的死亡背包已回收"
        );
        ci.cancel();
    }

    @Inject(method = "sendServerStatus", at = @At("HEAD"), cancellable = true)
    private static void deadrecall$sendTemporaryServerStatus(
            String status,
            int playersOnline,
            int playersMax,
            String version,
            double tps,
            CallbackInfo ci) {
        DiscordTransientBridge.sendServerStatus(status, playersOnline, playersMax, version, tps, false);
        ci.cancel();
    }

    @Inject(method = "sendServerStatusImmediately", at = @At("HEAD"), cancellable = true)
    private static void deadrecall$sendTemporaryServerStatusImmediately(
            String status,
            int playersOnline,
            int playersMax,
            String version,
            double tps,
            CallbackInfo ci) {
        DiscordTransientBridge.sendServerStatus(status, playersOnline, playersMax, version, tps, true);
        ci.cancel();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
