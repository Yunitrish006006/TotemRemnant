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
    @Inject(method = "init", at = @At("TAIL"))
    private void totem$addBackpackActions(CallbackInfo ci) {
        addItemSlotMouseAction(new BackpackMouseActions((AbstractContainerScreen<?>) (Object) this));
    }
}
