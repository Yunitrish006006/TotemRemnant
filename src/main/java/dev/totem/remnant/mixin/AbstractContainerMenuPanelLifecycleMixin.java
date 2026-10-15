package dev.totem.remnant.mixin;

import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuPanelLifecycleMixin {
    @Inject(method = "removed", at = @At("TAIL"))
    private void totem$finishPanelRecovery(Player player, CallbackInfo callback) {
        if (BackpackPanelMenuAccess.supports((AbstractContainerMenu)(Object)this)
                && (Object)this instanceof BackpackPanelMenuAccess access)
            access.totem$getBackpackPanel().finishRecovery(player);
    }
}
