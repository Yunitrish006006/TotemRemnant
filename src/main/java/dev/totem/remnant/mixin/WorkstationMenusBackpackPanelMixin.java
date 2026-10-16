package dev.totem.remnant.mixin;

import dev.totem.remnant.inventory.BackpackPanelAttachment;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native lifecycle adapter; all sidebar operations live in the shared attachment. */
@Mixin({StonecutterMenu.class,GrindstoneMenu.class,LoomMenu.class,CartographyTableMenu.class})
abstract class WorkstationMenusBackpackPanelMixin implements BackpackPanelMenuAccess {
    @Unique private BackpackPanelAttachment totem$panelAttachment;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void totem$attachPanel(CallbackInfo callback) {
        if (totem$panelAttachment == null)
            totem$panelAttachment = BackpackPanelAttachment.attach((AbstractContainerMenu) (Object) this);
    }

    @Override public BackpackPanelAttachment totem$getBackpackPanelAttachment() { return totem$panelAttachment; }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void totem$quickMovePanel(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> callback) {
        if (totem$panelAttachment != null && totem$panelAttachment.contains(slotIndex))
            callback.setReturnValue(totem$panelAttachment.quickMove(player, slotIndex));
    }
}
