package com.adaptor.deadrecall.mixin.client;

import com.adaptor.deadrecall.client.DeadrecallClient;
import com.adaptor.deadrecall.network.SortBackpackPayload;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void deadrecall$handleSortKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (DeadrecallClient.sortBackpackKey.matches(event)) {
            SortBackpackPayload.Target target = DeadrecallClient.resolveSortTarget(
                    (AbstractContainerScreen<?>) (Object) this,
                    this.hoveredSlot,
                    Minecraft.getInstance()
            );
            if (target != null) {
                DeadrecallClient.requestContainerSort(net.minecraft.client.Minecraft.getInstance(), target);
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void deadrecall$handleSortMouse(MouseButtonEvent event, boolean over, CallbackInfoReturnable<Boolean> cir) {
        if (DeadrecallClient.sortBackpackKey.matchesMouse(event)) {
            SortBackpackPayload.Target target = DeadrecallClient.resolveSortTarget(
                    (AbstractContainerScreen<?>) (Object) this,
                    this.hoveredSlot,
                    Minecraft.getInstance()
            );
            if (target != null) {
                DeadrecallClient.requestContainerSort(net.minecraft.client.Minecraft.getInstance(), target);
                cir.setReturnValue(true);
            }
        }
    }
}
