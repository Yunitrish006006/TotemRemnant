package dev.totem.remnant.mixin;

import dev.totem.remnant.item.BackpackItemHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "checkBelowWorld", at = @At("HEAD"), cancellable = true)
    private void totemremnant$protectDeathBackpackFromVoidDiscard(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ItemEntity item && BackpackItemHelper.shouldApplyDeathBackpackVoidMomentum(item)) {
            BackpackItemHelper.applyDeathBackpackVoidMomentum(item);
            ci.cancel();
        }
    }
}
