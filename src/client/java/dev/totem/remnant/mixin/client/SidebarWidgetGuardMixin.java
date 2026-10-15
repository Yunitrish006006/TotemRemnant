package dev.totem.remnant.mixin.client;
import dev.totem.remnant.client.screen.SidebarProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Blocks direct widget callbacks in addition to normal screen event dispatch. */
@Mixin({Button.class,CycleButton.class})
abstract class SidebarWidgetGuardMixin {
    @Inject(method="onPress",at=@At("HEAD"),cancellable=true)
    private void remnant$blockCallback(CallbackInfo ci) {
        if(Minecraft.getInstance().gui.screen() instanceof SidebarProjection)ci.cancel();
    }
}
