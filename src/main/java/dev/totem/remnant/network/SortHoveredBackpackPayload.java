package dev.totem.remnant.network;

import dev.totem.remnant.inventory.BackpackActions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Sort exactly the hovered stack; the existing surface-sort protocol keeps its meaning. */
public record SortHoveredBackpackPayload(int menuId, int stateId, int slot, ItemStack expected,
        int selectedRootSlot, ItemStack expectedRoot) implements CustomPacketPayload {
    public SortHoveredBackpackPayload(int menuId, int stateId, int slot, ItemStack expected) {
        this(menuId, stateId, slot, expected, -1, ItemStack.EMPTY);
    }
    public static final Type<SortHoveredBackpackPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("totem", "remnant/sort_hovered_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SortHoveredBackpackPayload> CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.menuId); buffer.writeVarInt(value.stateId); buffer.writeVarInt(value.slot);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, value.expected);
                buffer.writeVarInt(value.selectedRootSlot); ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, value.expectedRoot);
            }, buffer -> new SortHoveredBackpackPayload(buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readVarInt(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static SortHoveredBackpackPayload forSlot(net.minecraft.world.inventory.AbstractContainerMenu menu,
            net.minecraft.world.inventory.Slot slot) {
        if (slot instanceof dev.totem.remnant.inventory.BackpackPanelSlot
                && menu instanceof dev.totem.remnant.inventory.BackpackPanelMenuAccess access) {
            var panel = access.totem$getBackpackPanel();
            return new SortHoveredBackpackPayload(menu.containerId, menu.getStateId(), slot.index, slot.getItem().copy(),
                    panel.selectedInventorySlot(), panel.selectedBackpack().copy());
        }
        return new SortHoveredBackpackPayload(menu.containerId, menu.getStateId(), slot.index, slot.getItem().copy());
    }
    public static boolean handle(ServerPlayer player, SortHoveredBackpackPayload request) {
        var menu = player.containerMenu;
        if (menu.containerId != request.menuId || menu.getStateId() != request.stateId
                || request.slot < 0 || request.slot >= menu.slots.size()) return false;
        if (menu.getSlot(request.slot) instanceof dev.totem.remnant.inventory.BackpackPanelSlot) {
            if (!(dev.totem.remnant.inventory.BackpackPanelMenuAccess.supports(menu)
                    && menu instanceof dev.totem.remnant.inventory.BackpackPanelMenuAccess access)) return false;
            var panel = access.totem$getBackpackPanel();
            if (panel.selectedInventorySlot() != request.selectedRootSlot
                    || !ItemStack.matches(panel.selectedBackpack(), request.expectedRoot)) return false;
        } else if (request.selectedRootSlot != -1 || !request.expectedRoot.isEmpty()) return false;
        return BackpackActions.sortHovered(player, request.slot, request.expected);
    }
}
