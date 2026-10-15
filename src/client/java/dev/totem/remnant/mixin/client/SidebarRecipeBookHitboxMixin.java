package dev.totem.remnant.mixin.client;
import dev.totem.remnant.client.screen.BackpackSidebarLayout;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Recipe-book Screens override the base outside test without calling super. */
@Mixin(AbstractRecipeBookScreen.class)
abstract class SidebarRecipeBookHitboxMixin {
    @Inject(method="hasClickedOutside",at=@At("HEAD"),cancellable=true)
    private void remnant$includeSidebar(double x,double y,int left,int top,CallbackInfoReturnable<Boolean> ci) {
        if((Object)this instanceof BackpackSidebarLayout layout && layout.remnant$contains(x,y))ci.setReturnValue(false);
    }
}
