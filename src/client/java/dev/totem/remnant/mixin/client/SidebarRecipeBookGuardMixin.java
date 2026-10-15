package dev.totem.remnant.mixin.client;
import dev.totem.remnant.client.screen.SidebarProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RecipeBookComponent.class)
abstract class SidebarRecipeBookGuardMixin {
    private static boolean remnant$readonly() { return Minecraft.getInstance().gui.screen() instanceof SidebarProjection; }
    @Inject(method = {"toggleVisibility", "setVisible", "toggleFiltering", "sendUpdateSettings", "tick", "recipesUpdated", "checkSearchStringUpdate"}, at = @At("HEAD"), cancellable = true)
    private void remnant$blockMutation(CallbackInfo ci) { if (remnant$readonly()) ci.cancel(); }
    @Inject(method = {"mouseClicked", "mouseDragged", "keyPressed", "keyReleased", "charTyped", "preeditUpdated", "tryPlaceRecipe"}, at = @At("HEAD"), cancellable = true)
    private void remnant$blockInput(CallbackInfoReturnable<Boolean> ci) { if (remnant$readonly()) ci.setReturnValue(true); }
}
