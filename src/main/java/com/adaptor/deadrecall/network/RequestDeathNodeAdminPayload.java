package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestDeathNodeAdminPayload() implements CustomPacketPayload {
    public static final Type<RequestDeathNodeAdminPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "request_death_node_admin"));

    public static final StreamCodec<FriendlyByteBuf, RequestDeathNodeAdminPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> new RequestDeathNodeAdminPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
