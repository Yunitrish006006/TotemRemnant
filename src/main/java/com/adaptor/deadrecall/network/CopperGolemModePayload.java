package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record CopperGolemModePayload(UUID golemId, String mode, int revision) implements CustomPacketPayload {
    public static final Type<CopperGolemModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "copper_golem_mode"));

    public static final StreamCodec<FriendlyByteBuf, CopperGolemModePayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeUtf(payload.mode(), 32);
                        buf.writeInt(payload.revision());
                    },
                    buf -> new CopperGolemModePayload(buf.readUUID(), buf.readUtf(32), buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
