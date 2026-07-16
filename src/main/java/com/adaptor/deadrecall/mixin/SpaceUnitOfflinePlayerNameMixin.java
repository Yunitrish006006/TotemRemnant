package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.network.SpaceUnitFriendsPayload;
import com.adaptor.deadrecall.space.SpaceUnitHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves names for offline Space Unit friends from the server profile cache.
 *
 * <p>The original fallback intentionally shortens the UUID. That is useful only when the server
 * has never seen a profile; known offline players should retain their last cached Minecraft name.</p>
 */
@Mixin(SpaceUnitHandler.class)
public abstract class SpaceUnitOfflinePlayerNameMixin {
    @Inject(method = "friendEntry", at = @At("HEAD"), cancellable = true)
    private static void deadrecall$useCachedNameForOfflineFriend(
            MinecraftServer server,
            UUID playerId,
            String status,
            CallbackInfoReturnable<SpaceUnitFriendsPayload.Entry> cir
    ) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        String name = online != null
                ? online.getName().getString()
                : deadrecall$cachedPlayerName(server, playerId).orElseGet(() -> deadrecall$shortPlayerId(playerId));
        cir.setReturnValue(new SpaceUnitFriendsPayload.Entry(playerId, name, online != null, status));
    }

    @Inject(method = "playerDisplayName", at = @At("HEAD"), cancellable = true)
    private static void deadrecall$useCachedPlayerDisplayName(
            MinecraftServer server,
            UUID playerId,
            CallbackInfoReturnable<String> cir
    ) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            cir.setReturnValue(online.getName().getString());
            return;
        }
        deadrecall$cachedPlayerName(server, playerId).ifPresent(cir::setReturnValue);
    }

    private static Optional<String> deadrecall$cachedPlayerName(MinecraftServer server, UUID playerId) {
        return server.getProfileCache().get(playerId).flatMap(profile -> {
            String name = deadrecall$invokeNameAccessor(profile, "name");
            if (name == null || name.isBlank()) {
                name = deadrecall$invokeNameAccessor(profile, "getName");
            }
            return name == null || name.isBlank() ? Optional.empty() : Optional.of(name);
        });
    }

    private static String deadrecall$invokeNameAccessor(Object profile, String methodName) {
        try {
            Method method = profile.getClass().getMethod(methodName);
            Object value = method.invoke(profile);
            return value instanceof String string ? string : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String deadrecall$shortPlayerId(UUID playerId) {
        String id = playerId.toString();
        return id.length() <= 8 ? id : id.substring(0, 8);
    }
}
