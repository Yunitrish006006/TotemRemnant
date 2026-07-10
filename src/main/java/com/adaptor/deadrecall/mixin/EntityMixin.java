package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.item.BackpackItemHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "checkBelowWorld", at = @At("HEAD"), cancellable = true)
    private void deadrecall$protectDeathBackpackFromVoidDiscard(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ItemEntity itemEntity
                && BackpackItemHelper.shouldApplyDeathBackpackVoidMomentum(itemEntity)) {
            BackpackItemHelper.applyDeathBackpackVoidMomentum(itemEntity);
            ci.cancel();
        }
    }
}
