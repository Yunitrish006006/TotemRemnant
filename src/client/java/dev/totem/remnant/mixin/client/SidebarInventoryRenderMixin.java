package dev.totem.remnant.mixin.client;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.totem.remnant.client.screen.SidebarProjection;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
@Mixin(InventoryScreen.class)
abstract class SidebarInventoryRenderMixin {
    @WrapOperation(method = {"init", "containerTick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
    private boolean remnant$keepNativeInventory(LocalPlayer player, Operation<Boolean> original) {
        return !((Object)this instanceof SidebarProjection) && original.call(player);
    }
    @ModifyArg(method = "extractBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"), index = 9)
    private LivingEntity remnant$targetAvatar(LivingEntity original) {
        return (Object)this instanceof SidebarProjection projection ? projection.remnant$target() : original;
    }
}
