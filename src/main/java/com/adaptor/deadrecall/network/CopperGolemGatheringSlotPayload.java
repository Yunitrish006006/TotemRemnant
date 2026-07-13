package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record CopperGolemGatheringSlotPayload(UUID golemId, Slot slot, Action action, int revision) implements CustomPacketPayload {
    public static final Type<CopperGolemGatheringSlotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "copper_golem_gathering_slot"));

    public enum Slot {
        TOOL,
        STORAGE
    }

    public enum Action {
        INSERT_MAIN_HAND,
        TAKE_ALL
    }

    public static final StreamCodec<FriendlyByteBuf, CopperGolemGatheringSlotPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeEnum(payload.slot());
                        buf.writeEnum(payload.action());
                        buf.writeInt(payload.revision());
                    },
                    buf -> new CopperGolemGatheringSlotPayload(buf.readUUID(), buf.readEnum(Slot.class), buf.readEnum(Action.class), buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
