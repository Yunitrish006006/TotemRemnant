package dev.totem.remnant.mixin.client;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.totem.remnant.client.screen.SidebarProjection;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(EnchantmentScreen.class)
abstract class SidebarEnchantingRenderMixin {
    @ModifyExpressionValue(method = {"extractBackground", "extractRenderState"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I"))
    private int remnant$targetLevel(int original) {
        return (Object)this instanceof SidebarProjection p ? p.remnant$target().experienceLevel : original;
    }
    @ModifyExpressionValue(method = {"extractBackground", "extractRenderState"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
    private boolean remnant$targetCreative(boolean original) {
        return (Object)this instanceof SidebarProjection p ? p.remnant$target().hasInfiniteMaterials() : original;
    }
}
