package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record RequestCopperGolemVisualizationPayload(UUID golemId) implements CustomPacketPayload {
    public static final Type<RequestCopperGolemVisualizationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "request_copper_golem_visualization"));

    public static final StreamCodec<FriendlyByteBuf, RequestCopperGolemVisualizationPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeUUID(payload.golemId()),
                    buf -> new RequestCopperGolemVisualizationPayload(buf.readUUID())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
