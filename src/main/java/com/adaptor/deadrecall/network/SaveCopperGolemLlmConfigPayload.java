package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record SaveCopperGolemLlmConfigPayload(UUID golemId, String apiUrl, String apiKey, String model, int revision) implements CustomPacketPayload {
    public static final Type<SaveCopperGolemLlmConfigPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "save_copper_golem_llm_config"));

    public static final StreamCodec<FriendlyByteBuf, SaveCopperGolemLlmConfigPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeUtf(payload.apiUrl(), 2048);
                        buf.writeUtf(payload.apiKey(), 512);
                        buf.writeUtf(payload.model(), 256);
                        buf.writeInt(payload.revision());
                    },
                    buf -> new SaveCopperGolemLlmConfigPayload(
                            buf.readUUID(),
                            buf.readUtf(2048),
                            buf.readUtf(512),
                            buf.readUtf(256),
                            buf.readInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
