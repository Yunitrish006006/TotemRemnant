package dev.totem.remnant.client;

import dev.totem.core.api.v1.client.observer.ObserverReadOnlyScreen;
import dev.totem.remnant.inventory.BackpackItemInteraction;
import dev.totem.remnant.item.TieredBackpackItem;
import dev.totem.remnant.network.SelectBackpackItemPayload;
import dev.totem.remnant.registry.RemnantDataComponents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Uses vanilla fractional-wheel accumulation and slot hover/click lifecycle. */
public final class BackpackMouseActions implements ItemSlotMouseAction {
    private final AbstractContainerScreen<?> screen;
    private final ScrollWheelHandler wheel = new ScrollWheelHandler();
    public BackpackMouseActions(AbstractContainerScreen<?> screen) { this.screen = screen; }
    @Override public boolean matches(Slot slot) { return slot.getItem().getItem() instanceof TieredBackpackItem; }
    @Override public boolean onMouseScrolled(double x, double y, int slot, ItemStack bag) {
        if (readOnly()) return true;
        var choices = BackpackItemInteraction.choices(bag);
        if (choices.isEmpty()) return false;
        var delta = wheel.onMouseScroll(x, y);
        int steps = delta.y == 0 ? -delta.x : delta.y;
        if (steps != 0) {
            int current = choices.indexOf(bag.getOrDefault(RemnantDataComponents.SELECTED_ITEM, -1));
            int next = ScrollWheelHandler.getNextScrollWheelSelection(steps, current, choices.size());
            select(bag, slot, choices.get(next));
        }
        return true;
    }
    @Override public void onStopHovering(Slot slot) { clear(slot); }
    @Override public void onSlotClicked(Slot slot, ContainerInput input) {
        if (input == ContainerInput.QUICK_MOVE || input == ContainerInput.SWAP) clear(slot);
    }
    private void clear(Slot slot) {
        if (slot.getItem().has(RemnantDataComponents.SELECTED_ITEM)) select(slot.getItem(), slot.index, -1);
    }
    private void select(ItemStack bag, int slot, int next) {
        if (readOnly() || !ClientPlayNetworking.canSend(SelectBackpackItemPayload.TYPE)) return;
        var menu = screen.getMenu();
        if (slot < 0 || slot >= menu.slots.size() || menu.getSlot(slot).getItem() != bag) return;
        var request = new SelectBackpackItemPayload(menu.containerId, menu.getStateId(), slot, next, bag.copy());
        if (BackpackItemInteraction.select(bag, next)) ClientPlayNetworking.send(request);
    }
    private boolean readOnly() {
        return screen instanceof ObserverReadOnlyScreen view && view.totem$isObserverReadOnly();
    }
}
