package dev.totem.remnant.mixin.client;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.totem.remnant.client.screen.SidebarProjection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Collection;
@Mixin(EffectsInInventory.class)
abstract class SidebarEffectsRenderMixin {
    @Shadow @Final private AbstractContainerScreen<?> screen;
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getActiveEffects()Ljava/util/Collection;"))
    private Collection<MobEffectInstance> remnant$targetEffects(LocalPlayer player, Operation<Collection<MobEffectInstance>> original) {
        return screen instanceof SidebarProjection p ? p.remnant$target().getActiveEffects() : original.call(player);
    }
}
