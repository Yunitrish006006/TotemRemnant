package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record UpdateCopperGolemGatheringLlmPayload(UUID golemId, boolean enabled, String prompt, int revision)
        implements CustomPacketPayload {
    public static final Type<UpdateCopperGolemGatheringLlmPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "update_copper_golem_gathering_llm"));

    public static final StreamCodec<FriendlyByteBuf, UpdateCopperGolemGatheringLlmPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeBoolean(payload.enabled());
                        buf.writeUtf(payload.prompt(), 2048);
                        buf.writeInt(payload.revision());
                    },
                    buf -> new UpdateCopperGolemGatheringLlmPayload(
                            buf.readUUID(),
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
