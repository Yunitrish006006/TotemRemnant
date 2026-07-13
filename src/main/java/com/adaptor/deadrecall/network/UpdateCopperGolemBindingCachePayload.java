package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record UpdateCopperGolemBindingCachePayload(
        UUID golemId,
        String dimension,
        int x,
        int y,
        int z,
        String value,
        boolean tag,
        boolean allowed,
        int revision) implements CustomPacketPayload {
    public static final Type<UpdateCopperGolemBindingCachePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "update_copper_golem_binding_cache"));

    public static final StreamCodec<FriendlyByteBuf, UpdateCopperGolemBindingCachePayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeUtf(payload.dimension(), 128);
                        buf.writeInt(payload.x());
                        buf.writeInt(payload.y());
                        buf.writeInt(payload.z());
                        buf.writeUtf(payload.value(), 256);
                        buf.writeBoolean(payload.tag());
                        buf.writeBoolean(payload.allowed());
                        buf.writeInt(payload.revision());
                    },
                    buf -> new UpdateCopperGolemBindingCachePayload(
                            buf.readUUID(),
                            buf.readUtf(128),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readUtf(256),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
