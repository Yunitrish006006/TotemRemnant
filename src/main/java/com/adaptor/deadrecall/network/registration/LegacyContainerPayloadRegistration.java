package com.adaptor.deadrecall.network.registration;

import com.adaptor.deadrecall.network.SortBackpackPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

final class LegacyContainerPayloadRegistration {
    private LegacyContainerPayloadRegistration() {
    }

    static void registerServerboundTypes() {
        PayloadTypeRegistry.serverboundPlay().register(
                SortBackpackPayload.TYPE, SortBackpackPayload.CODEC);
    }

    static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(SortBackpackPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        LegacyContainerSortService.sortOpenContainer(context.player(), payload.target())));
    }
}
