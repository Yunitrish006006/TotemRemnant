package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TestCopperGolemLlmConnectionPayload(String apiUrl, String apiKey, String model) implements CustomPacketPayload {
    public static final Type<TestCopperGolemLlmConnectionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "test_copper_golem_llm_connection"));

    public static final StreamCodec<FriendlyByteBuf, TestCopperGolemLlmConnectionPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.apiUrl(), 2048);
                        buf.writeUtf(payload.apiKey(), 512);
                        buf.writeUtf(payload.model(), 256);
                    },
                    buf -> new TestCopperGolemLlmConnectionPayload(
                            buf.readUtf(2048),
                            buf.readUtf(512),
                            buf.readUtf(256)
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
