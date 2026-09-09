package dev.totem.remnant.mixin;

import dev.totem.remnant.item.BackpackItemHelper;
import dev.totem.remnant.death.DeathBackpackOwnerBinding;
import dev.totem.remnant.registry.RemnantGameRules;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @org.spongepowered.asm.mixin.Unique private net.minecraft.core.GlobalPos totemremnant$lastDeathPosition;

    @Inject(method = "tick", at = @At("TAIL"))
    private void totemremnant$reportDeathBackpackPosition(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || self.isRemoved()
                || !self.getItem().is(RemnantItemRegistration.DEATH_BACKPACK)) return;
        var nodeId = dev.totem.remnant.death.DeathBackpackNodeBinding.read(self.getItem());
        var ownerId = DeathBackpackOwnerBinding.read(self.getItem());
        if (nodeId == null || ownerId == null) return;
        var position = net.minecraft.core.GlobalPos.of(level.dimension(), self.blockPosition());
        var provider = dev.totem.core.api.v1.death.DeathBackpackNodeLifecycle.current();
        if (provider.isPresent() && !position.equals(totemremnant$lastDeathPosition)) {
            provider.get().moved(level, nodeId, self.getUUID(), ownerId, self.blockPosition());
            totemremnant$lastDeathPosition = position;
        }
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void totemremnant$restrictDeathBackpackPickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)
                || !self.getItem().is(RemnantItemRegistration.DEATH_BACKPACK)
                || !RemnantGameRules.deathBackpackOwnerPickupOnly(level)) return;
        java.util.UUID ownerId = DeathBackpackOwnerBinding.read(self.getItem());
        if (ownerId != null && !ownerId.equals(player.getUUID())) ci.cancel();
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "playerTouch", at = @At(
            value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean totemremnant$recoverPickedUpBackpack(net.minecraft.world.entity.player.Inventory inventory,
            net.minecraft.world.item.ItemStack stack,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original, Player player) {
        var node = stack.is(RemnantItemRegistration.DEATH_BACKPACK)
                ? dev.totem.remnant.death.DeathBackpackNodeBinding.read(stack) : null;
        int before = totemremnant$boundCount(inventory, node);
        boolean accepted = original.call(inventory, stack);
        if (node != null && accepted && stack.isEmpty() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && totemremnant$boundCount(inventory, node) > before) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                var inserted = inventory.getItem(slot);
                if (node.equals(dev.totem.remnant.death.DeathBackpackNodeBinding.read(inserted))) {
                    dev.totem.remnant.death.DeathBackpackRecoveryService.recoverBoundNode(serverPlayer, inserted);
                    break;
                }
            }
        }
        return accepted;
    }

    @org.spongepowered.asm.mixin.Unique
    private static int totemremnant$boundCount(net.minecraft.world.entity.player.Inventory inventory, java.util.UUID node) {
        if (node == null) return 0;
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (node.equals(dev.totem.remnant.death.DeathBackpackNodeBinding.read(stack))) count += stack.getCount();
        }
        return count;
    }

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
        if (BackpackItemHelper.shouldApplyBackpackVoidMomentum(self)) BackpackItemHelper.applyBackpackVoidMomentum(self);
        else if (BackpackItemHelper.shouldApplyBackpackSlowFalling(self)) BackpackItemHelper.applyBackpackSlowFalling(self);
        else if (BackpackItemHelper.shouldStopBackpackVoidMomentum(self)) BackpackItemHelper.stopBackpackVoidMomentum(self);
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
