package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.inventory.PortableContainerPolicy;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the shared nesting policy to direct player interaction with Shulker Box menus.
 */
@Mixin(ShulkerBoxSlot.class)
public abstract class ShulkerBoxSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void deadrecall$rejectBackpacks(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!PortableContainerPolicy.mayInsertIntoPortableContainer(stack)) {
            cir.setReturnValue(false);
        }
    }
}
