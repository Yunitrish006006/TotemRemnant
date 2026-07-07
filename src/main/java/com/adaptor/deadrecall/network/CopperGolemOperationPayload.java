package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record CopperGolemOperationPayload(UUID golemId, boolean running) implements CustomPacketPayload {
    public static final Type<CopperGolemOperationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "copper_golem_operation"));

    public static final StreamCodec<FriendlyByteBuf, CopperGolemOperationPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeBoolean(payload.running());
                    },
                    buf -> new CopperGolemOperationPayload(buf.readUUID(), buf.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
