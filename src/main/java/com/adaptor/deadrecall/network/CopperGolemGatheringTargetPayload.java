package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record CopperGolemGatheringTargetPayload(
        UUID golemId,
        String value,
        boolean tag,
        TargetSet targetSet,
        Action action,
        int revision) implements CustomPacketPayload {
    public static final Type<CopperGolemGatheringTargetPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "copper_golem_gathering_target"));

    public enum Action {
        REMOVE
    }

    public enum TargetSet {
        MANUAL,
        ALLOWED,
        DENIED
    }

    public static final StreamCodec<FriendlyByteBuf, CopperGolemGatheringTargetPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeUtf(payload.value(), 256);
                        buf.writeBoolean(payload.tag());
                        buf.writeEnum(payload.targetSet());
                        buf.writeEnum(payload.action());
                        buf.writeInt(payload.revision());
                    },
                    buf -> new CopperGolemGatheringTargetPayload(
                            buf.readUUID(),
                            buf.readUtf(256),
                            buf.readBoolean(),
                            buf.readEnum(TargetSet.class),
                            buf.readEnum(Action.class),
                            buf.readInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
