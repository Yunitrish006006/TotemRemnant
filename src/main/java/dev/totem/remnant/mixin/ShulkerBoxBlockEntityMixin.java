package dev.totem.remnant.mixin;

import dev.totem.remnant.inventory.PortableContainerDiagnostics;
import dev.totem.remnant.inventory.PortableContainerPolicy;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rejects Remnant backpacks from sided Shulker Box automation. */
@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin {
    @Inject(method = "canPlaceItemThroughFace", at = @At("HEAD"), cancellable = true)
    private void totemRemnant$rejectBackpacksFromAutomation(
            int slot,
            ItemStack stack,
            Direction direction,
            CallbackInfoReturnable<Boolean> callback
    ) {
        ShulkerBoxBlockEntity shulker = (ShulkerBoxBlockEntity) (Object) this;
        boolean allowed = shulker.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel
                ? PortableContainerPolicy.mayInsertIntoPortableContainer(serverLevel, stack)
                : PortableContainerPolicy.mayInsertIntoPortableContainer(stack);
        if (!allowed) {
            PortableContainerDiagnostics.logRejectedAutomation(
                    shulker.getLevel(),
                    shulker.getBlockPos(),
                    stack,
                    "vanilla_sided_transfer"
            );
            callback.setReturnValue(false);
        }
    }
}
