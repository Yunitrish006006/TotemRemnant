package dev.totem.remnant.mixin;

import dev.totem.remnant.item.BackpackItemHelper;
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
    private void totemremnant$protectDroppedBackpack(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(self.getItem(), source)) cir.setReturnValue(false);
    }
    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void totemremnant$ejectDestroyedContents(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!BackpackItemHelper.isVoidDamage(source)) BackpackItemHelper.dropStoredItems(level, self.position(), self.getItem(), ejectionDirection(self, source));
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void totemremnant$maintainDroppedBackpack(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (BackpackItemHelper.shouldPreventDroppedBackpackDespawn(self.getItem())) self.setUnlimitedLifetime();
        if (BackpackItemHelper.shouldApplyDeathBackpackVoidMomentum(self)) BackpackItemHelper.applyDeathBackpackVoidMomentum(self);
        else if (BackpackItemHelper.shouldApplyDeathBackpackSlowFalling(self)) BackpackItemHelper.applyDeathBackpackSlowFalling(self);
        else if (BackpackItemHelper.shouldStopDeathBackpackVoidMomentum(self)) BackpackItemHelper.stopDeathBackpackVoidMomentum(self);
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V", ordinal = 1))
    private void totemremnant$ejectDespawnedContents(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level() instanceof ServerLevel level) BackpackItemHelper.dropStoredItems(level, self.position(), self.getItem(), self.getDeltaMovement().scale(-1.0));
    }
    private static Vec3 ejectionDirection(ItemEntity entity, DamageSource source) {
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null && "cactus".equals(source.getMsgId())) sourcePosition = nearestCactus(entity);
        return sourcePosition == null ? entity.getDeltaMovement().scale(-1.0) : entity.position().subtract(sourcePosition);
    }
    private static Vec3 nearestCactus(ItemEntity entity) {
        Vec3 result = null; double distance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(entity.getBoundingBox().inflate(1.0))) {
            if (!entity.level().getBlockState(pos).is(Blocks.CACTUS)) continue;
            Vec3 candidate = Vec3.atCenterOf(pos); double candidateDistance = candidate.distanceToSqr(entity.position());
            if (candidateDistance < distance) { result = candidate; distance = candidateDistance; }
        }
        return result;
    }
}
