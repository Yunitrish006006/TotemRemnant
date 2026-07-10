package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record CopperGolemFuelSlotPayload(UUID golemId, Action action) implements CustomPacketPayload {
    public static final Type<CopperGolemFuelSlotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "copper_golem_fuel_slot"));

    public enum Action {
        INSERT_MAIN_HAND,
        TAKE_ALL
    }

    public static final StreamCodec<FriendlyByteBuf, CopperGolemFuelSlotPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeEnum(payload.action());
                    },
                    buf -> new CopperGolemFuelSlotPayload(buf.readUUID(), buf.readEnum(Action.class))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
