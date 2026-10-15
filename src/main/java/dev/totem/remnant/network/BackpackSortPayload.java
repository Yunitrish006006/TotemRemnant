package dev.totem.remnant.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** A bounded sort request bound to the currently displayed menu and panel selection. */
public record BackpackSortPayload(int menuId, int stateId, int hoveredSlot, int selectedSlot,
                                  ItemStack expectedBackpack) implements CustomPacketPayload {
    public static final Type<BackpackSortPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("totem", "remnant/sort_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackSortPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.menuId); buf.writeVarInt(p.stateId); buf.writeVarInt(p.hoveredSlot);
                buf.writeVarInt(p.selectedSlot); ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.expectedBackpack); },
            buf -> new BackpackSortPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
