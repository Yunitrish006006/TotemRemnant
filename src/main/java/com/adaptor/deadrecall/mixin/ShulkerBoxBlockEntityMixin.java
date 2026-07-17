package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.inventory.PortableContainerDiagnostics;
import com.adaptor.deadrecall.inventory.PortableContainerPolicy;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the shared nesting policy to sided automation such as hoppers, hopper minecarts,
 * droppers and dispensers targeting a Shulker Box.
 */
@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin {
    @Inject(method = "canPlaceItemThroughFace", at = @At("HEAD"), cancellable = true)
    private void deadrecall$rejectBackpacksFromAutomation(
            int slot,
            ItemStack stack,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!PortableContainerPolicy.mayInsertIntoPortableContainer(stack)) {
            ShulkerBoxBlockEntity shulker = (ShulkerBoxBlockEntity) (Object) this;
            PortableContainerDiagnostics.logRejectedAutomation(
                    shulker.getLevel(),
                    shulker.getBlockPos(),
                    stack,
                    "vanilla_sided_transfer"
            );
            cir.setReturnValue(false);
        }
    }
}
