package dev.totem.remnant.network;

import dev.totem.remnant.inventory.BackpackItemInteraction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Selection changes are bound to the exact menu, slot and pre-prediction stack. */
public record SelectBackpackItemPayload(int menuId, int stateId, int slot, int selected, ItemStack expected)
        implements CustomPacketPayload {
    public static final Type<SelectBackpackItemPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("totem", "remnant/select_backpack_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectBackpackItemPayload> CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.menuId); buffer.writeVarInt(value.stateId);
                buffer.writeVarInt(value.slot); buffer.writeVarInt(value.selected);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, value.expected);
            }, buffer -> new SelectBackpackItemPayload(buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static boolean handle(ServerPlayer player, SelectBackpackItemPayload request) {
        if (!player.isAlive() || player.isSpectator()) return false;
        var menu = player.containerMenu;
        if (menu.containerId != request.menuId || menu.getStateId() != request.stateId || !menu.stillValid(player)
                || request.slot < 0 || request.slot >= menu.slots.size()) return false;
        var slot = menu.getSlot(request.slot);
        return slot.isActive() && slot.allowModification(player) && ItemStack.matches(slot.getItem(), request.expected)
                && BackpackItemInteraction.select(slot.getItem(), request.selected);
    }
}
