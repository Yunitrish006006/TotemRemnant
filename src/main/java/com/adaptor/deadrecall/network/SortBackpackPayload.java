package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客戶端 → 伺服器：整理目前開著的容器區域
 */
public record SortBackpackPayload(Target target) implements CustomPacketPayload {

    public enum Target {
        CONTAINER,
        PLAYER
    }

    public static final Type<SortBackpackPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "sort_backpack"));

    public static final StreamCodec<FriendlyByteBuf, SortBackpackPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeEnum(payload.target()),
                    buf -> new SortBackpackPayload(buf.readEnum(Target.class))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
