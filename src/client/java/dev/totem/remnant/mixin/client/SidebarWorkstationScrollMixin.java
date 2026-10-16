package dev.totem.remnant.mixin.client;
import dev.totem.remnant.client.screen.BackpackSidebarLayout;
import net.minecraft.client.gui.screens.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Native scrolling overrides must give the hovered backpack its own scroll first. */
@Mixin({MerchantScreen.class, StonecutterScreen.class, LoomScreen.class})
abstract class SidebarWorkstationScrollMixin {
    @Inject(method="mouseScrolled", at=@At("HEAD"), cancellable=true)
    private void remnant$sidebarScroll(double x,double y,double dx,double dy,CallbackInfoReturnable<Boolean> ci) {
        if (((BackpackSidebarLayout)this).remnant$scrollPanel(x,y,dx,dy)) ci.setReturnValue(true);
    }
}
