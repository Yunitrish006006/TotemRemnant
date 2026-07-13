package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record UpdateCopperGolemBindingLlmPayload(UUID golemId, String dimension, int x, int y, int z, boolean enabled, String prompt, int revision)
        implements CustomPacketPayload {
    public static final Type<UpdateCopperGolemBindingLlmPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "update_copper_golem_binding_llm"));

    public static final StreamCodec<FriendlyByteBuf, UpdateCopperGolemBindingLlmPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeUtf(payload.dimension(), 128);
                        buf.writeInt(payload.x());
                        buf.writeInt(payload.y());
                        buf.writeInt(payload.z());
                        buf.writeBoolean(payload.enabled());
                        buf.writeUtf(payload.prompt(), 2048);
                        buf.writeInt(payload.revision());
                    },
                    buf -> new UpdateCopperGolemBindingLlmPayload(
                            buf.readUUID(),
                            buf.readUtf(128),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readUtf(2048),
                            buf.readInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
