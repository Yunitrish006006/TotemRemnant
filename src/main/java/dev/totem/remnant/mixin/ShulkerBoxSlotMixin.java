package dev.totem.remnant.mixin;

import dev.totem.remnant.inventory.PortableContainerPolicy;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rejects Remnant backpacks from direct Shulker Box menu insertion. */
@Mixin(ShulkerBoxSlot.class)
public abstract class ShulkerBoxSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void totemRemnant$rejectBackpacks(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (!PortableContainerPolicy.mayInsertIntoPortableContainer(stack)) {
            callback.setReturnValue(false);
        }
    }
}
