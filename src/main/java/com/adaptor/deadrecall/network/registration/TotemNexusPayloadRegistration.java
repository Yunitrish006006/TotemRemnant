package com.adaptor.deadrecall.network.registration;

import com.adaptor.deadrecall.network.CalibrateSpaceUnitPayload;
import com.adaptor.deadrecall.network.ConfirmSpaceUnitRegistrationPayload;
import com.adaptor.deadrecall.network.RemoveSpaceUnitFriendPayload;
import com.adaptor.deadrecall.network.RenameSpaceUnitPayload;
import com.adaptor.deadrecall.network.RequestSpaceUnitFriendsPayload;
import com.adaptor.deadrecall.network.RequestSpaceUnitMapPayload;
import com.adaptor.deadrecall.network.SpaceUnitFriendsPayload;
import com.adaptor.deadrecall.network.SpaceUnitMapPayload;
import com.adaptor.deadrecall.network.SpaceUnitRegistrationPreviewPayload;
import com.adaptor.deadrecall.network.StartSpaceUnitTeleportPayload;
import com.adaptor.deadrecall.network.ToggleSpaceUnitFavoritePayload;
import com.adaptor.deadrecall.network.UpdateSpaceUnitAccessPayload;
import com.adaptor.deadrecall.network.UpdateSpaceUnitVisibilityPayload;
import com.adaptor.deadrecall.space.SpaceUnitHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

final class TotemNexusPayloadRegistration {
    private TotemNexusPayloadRegistration() {
    }

    static void registerServerboundTypes() {
        PayloadTypeRegistry.serverboundPlay().register(
                RequestSpaceUnitMapPayload.TYPE, RequestSpaceUnitMapPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                RequestSpaceUnitFriendsPayload.TYPE, RequestSpaceUnitFriendsPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                RemoveSpaceUnitFriendPayload.TYPE, RemoveSpaceUnitFriendPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                StartSpaceUnitTeleportPayload.TYPE, StartSpaceUnitTeleportPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                ToggleSpaceUnitFavoritePayload.TYPE, ToggleSpaceUnitFavoritePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                CalibrateSpaceUnitPayload.TYPE, CalibrateSpaceUnitPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                UpdateSpaceUnitVisibilityPayload.TYPE, UpdateSpaceUnitVisibilityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                RenameSpaceUnitPayload.TYPE, RenameSpaceUnitPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                UpdateSpaceUnitAccessPayload.TYPE, UpdateSpaceUnitAccessPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                ConfirmSpaceUnitRegistrationPayload.TYPE, ConfirmSpaceUnitRegistrationPayload.CODEC);
    }

    static void registerClientboundTypes() {
        PayloadTypeRegistry.clientboundPlay().register(
                SpaceUnitMapPayload.TYPE, SpaceUnitMapPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                SpaceUnitFriendsPayload.TYPE, SpaceUnitFriendsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                SpaceUnitRegistrationPreviewPayload.TYPE, SpaceUnitRegistrationPreviewPayload.CODEC);
    }

    static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(RequestSpaceUnitMapPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        SpaceUnitHandler.sendSpaceUnitMap(context.player(), payload.sourceType(), payload.sourceUnitId())));

        ServerPlayNetworking.registerGlobalReceiver(RequestSpaceUnitFriendsPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        SpaceUnitHandler.sendFriendList(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(RemoveSpaceUnitFriendPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        SpaceUnitHandler.removeFriend(context.player(), payload.friendId())));

        ServerPlayNetworking.registerGlobalReceiver(StartSpaceUnitTeleportPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        SpaceUnitHandler.startTeleport(context.player(), payload.sourceType(), payload.sourceUnitId(), payload.targetUnitId())));

        ServerPlayNetworking.registerGlobalReceiver(ToggleSpaceUnitFavoritePayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        SpaceUnitHandler.setFavorite(
                                context.player(),
                                payload.sourceType(),
                                payload.sourceUnitId(),
                                payload.targetUnitId(),
                                payload.favorite()
                        )));

        ServerPlayNetworking.registerGlobalReceiver(CalibrateSpaceUnitPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        SpaceUnitHandler.calibrateLodestone(
                                context.player(),
                                payload.sourceType(),
                                payload.sourceUnitId(),
                                payload.targetUnitId()
                        )));

        ServerPlayNetworking.registerGlobalReceiver(UpdateSpaceUnitVisibilityPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        SpaceUnitHandler.setLodestoneVisibility(
                                context.player(),
                                payload.sourceType(),
                                payload.sourceUnitId(),
                                payload.targetUnitId(),
                                payload.visibility()
                        )));

        ServerPlayNetworking.registerGlobalReceiver(RenameSpaceUnitPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        SpaceUnitHandler.setLodestoneName(
                                context.player(),
                                payload.sourceType(),
                                payload.sourceUnitId(),
                                payload.targetUnitId(),
                                payload.name()
                        )));

        ServerPlayNetworking.registerGlobalReceiver(UpdateSpaceUnitAccessPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        SpaceUnitHandler.setLodestoneAccess(
                                context.player(),
                                payload.sourceType(),
                                payload.sourceUnitId(),
                                payload.targetUnitId(),
                                payload.role(),
                                payload.playerName(),
                                payload.enabled()
                        )));

        ServerPlayNetworking.registerGlobalReceiver(ConfirmSpaceUnitRegistrationPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        SpaceUnitHandler.confirmLodestoneRegistration(
                                context.player(),
                                payload.dimension(),
                                payload.x(),
                                payload.y(),
                                payload.z()
                        )));
    }
}
