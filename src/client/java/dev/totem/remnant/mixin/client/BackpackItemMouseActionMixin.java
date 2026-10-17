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
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "mouseClicked")
    private boolean totem$sortAtClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubled,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        var screen = (AbstractContainerScreen<?>) (Object) this;
        return dev.totem.remnant.client.integration.RemnantSortClientProvider.withClick(screen, event, () -> {
            // VanillaTweaks still owns its configurable binding; its provider receives this event's position.
            if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE
                    && !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("totem-vanilla-tweaks")
                    && new dev.totem.remnant.client.integration.RemnantSortClientProvider().apply(
                            screen, getHoveredSlot(event.x(), event.y()), event.x(), event.y())) return true;
            return original.call(event, doubled);
        });
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void totem$addBackpackActions(CallbackInfo ci) {
        addItemSlotMouseAction(new BackpackMouseActions((AbstractContainerScreen<?>) (Object) this));
    }
}
