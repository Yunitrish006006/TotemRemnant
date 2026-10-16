package dev.totem.remnant.mixin.client;
import dev.totem.remnant.client.screen.SidebarProjection;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Suppress name responders during detached projection init and native slot updates. */
@Mixin(AnvilScreen.class)
abstract class SidebarAnvilInputGuardMixin {
    @Inject(method="onNameChanged", at=@At("HEAD"), cancellable=true)
    private void remnant$readonlyName(String name, CallbackInfo ci) {
        if ((Object)this instanceof SidebarProjection) ci.cancel();
    }
}
