package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.item.BackpackItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void deadrecall$protectDroppedBackpackFromDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(self.getItem(), source)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "hurtServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V")
    )
    private void deadrecall$dropBackpackContentsWhenDestroyed(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (BackpackItemHelper.isVoidDamage(source)) {
            return;
        }
        BackpackItemHelper.dropStoredItems(level, self.position(), self.getItem(), deadrecall$getDamageEjectionDirection(self, source));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void deadrecall$keepProtectedBackpackFromDespawning(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (BackpackItemHelper.shouldPreventDroppedBackpackDespawn(self.getItem())) {
            self.setUnlimitedLifetime();
        }
        if (BackpackItemHelper.shouldApplyDeathBackpackVoidMomentum(self)) {
            BackpackItemHelper.applyDeathBackpackVoidMomentum(self);
        } else if (BackpackItemHelper.shouldApplyDeathBackpackSlowFalling(self)) {
            BackpackItemHelper.applyDeathBackpackSlowFalling(self);
        } else if (BackpackItemHelper.shouldStopDeathBackpackVoidMomentum(self)) {
            BackpackItemHelper.stopDeathBackpackVoidMomentum(self);
        }
    }

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V", ordinal = 1)
    )
    private void deadrecall$dropBackpackContentsWhenDespawned(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level() instanceof ServerLevel level) {
            BackpackItemHelper.dropStoredItems(level, self.position(), self.getItem(), self.getDeltaMovement().scale(-1.0));
        }
    }

    private Vec3 deadrecall$getDamageEjectionDirection(ItemEntity itemEntity, DamageSource source) {
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null && "cactus".equals(source.getMsgId())) {
            sourcePosition = deadrecall$findNearestCactusPosition(itemEntity);
        }

        if (sourcePosition != null) {
            return itemEntity.position().subtract(sourcePosition);
        }
        return itemEntity.getDeltaMovement().scale(-1.0);
    }

    private Vec3 deadrecall$findNearestCactusPosition(ItemEntity itemEntity) {
        Vec3 itemPosition = itemEntity.position();
        Vec3 nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(itemEntity.getBoundingBox().inflate(1.0))) {
            if (!itemEntity.level().getBlockState(pos).is(Blocks.CACTUS)) {
                continue;
            }

            Vec3 cactusPosition = Vec3.atCenterOf(pos);
            double distance = cactusPosition.distanceToSqr(itemPosition);
            if (distance < nearestDistance) {
                nearest = cactusPosition;
                nearestDistance = distance;
            }
        }

        return nearest;
    }
}
