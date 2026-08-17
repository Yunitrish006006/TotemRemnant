package dev.totem.remnant.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Selects one bounded player-inventory backpack for the real side-panel slots. */
public record SelectBackpackPanelPayload(int inventorySlot) implements CustomPacketPayload {
    public static final Type<SelectBackpackPanelPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("totem", "remnant/select_backpack_panel")
    );
    public static final StreamCodec<FriendlyByteBuf, SelectBackpackPanelPayload> CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeVarInt(payload.inventorySlot()),
                    buffer -> new SelectBackpackPanelPayload(buffer.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
