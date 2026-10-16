package dev.totem.remnant.mixin.client;

import dev.totem.remnant.client.BackpackMouseActions;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class BackpackItemMouseActionMixin {
    @Shadow protected abstract void addItemSlotMouseAction(ItemSlotMouseAction action);
    @Shadow private net.minecraft.world.inventory.Slot getHoveredSlot(double x, double y) { throw new AssertionError(); }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void totem$standaloneSort(net.minecraft.client.input.MouseButtonEvent event, boolean doubled,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        // VanillaTweaks owns its configurable sort binding when present.
        if (event.button() == 2 && !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("totem-vanilla-tweaks")
                && new dev.totem.remnant.client.integration.RemnantSortClientProvider().apply(
                        (AbstractContainerScreen<?>) (Object) this, getHoveredSlot(event.x(), event.y()),
                        event.x(), event.y())) ci.setReturnValue(true);
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void totem$addBackpackActions(CallbackInfo ci) {
        addItemSlotMouseAction(new BackpackMouseActions((AbstractContainerScreen<?>) (Object) this));
    }
}
