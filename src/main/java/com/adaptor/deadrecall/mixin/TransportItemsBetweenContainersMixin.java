package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.item.copper.CopperGolemWrenchHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {
    @Shadow
    private TransportItemTarget target;

    @Shadow
    protected abstract void clearMemoriesAfterMatchingTargetFound(PathfinderMob mob);

    @Shadow
    protected abstract void stopTargetingCurrentTarget(PathfinderMob mob);

    @Inject(method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;)Z", at = @At("HEAD"), cancellable = true)
    private void deadrecall$requireBindingBeforeTransport(ServerLevel level, PathfinderMob mob, CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof CopperGolem golem
                && (!CopperGolemWrenchHandler.hasBinding(golem)
                || !CopperGolemWrenchHandler.isTransportEnabled(golem)
                || CopperGolemWrenchHandler.isSortingBlocked(golem))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getTransportTarget", at = @At("HEAD"), cancellable = true)
    private void deadrecall$useBoundCopperGolemTarget(ServerLevel level, PathfinderMob mob, CallbackInfoReturnable<Optional<TransportItemTarget>> cir) {
        if (!(mob instanceof CopperGolem golem) || mob.getMainHandItem().isEmpty()) {
            return;
        }

        if (!CopperGolemWrenchHandler.hasBinding(golem)) {
            return;
        }

        if (!CopperGolemWrenchHandler.isTransportEnabled(golem)) {
            cir.setReturnValue(Optional.empty());
            return;
        }

        Optional<TransportItemTarget> target = CopperGolemWrenchHandler.findNextDestinationTarget(golem, level, mob.getMainHandItem());
        if (target.isPresent()) {
            cir.setReturnValue(target);
            return;
        }

        if (CopperGolemWrenchHandler.returnCarriedItemToSource(golem, level)) {
            return;
        }

        cir.setReturnValue(Optional.empty());
    }

    @Inject(method = "isWantedBlock", at = @At("HEAD"), cancellable = true)
    private void deadrecall$acceptBoundCopperGolemTargetState(PathfinderMob mob, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof CopperGolem golem
                && !mob.getMainHandItem().isEmpty()
                && CopperGolemWrenchHandler.hasBinding(golem)
                && CopperGolemWrenchHandler.isTransportEnabled(golem)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "pickUpItems", at = @At("HEAD"), cancellable = true)
    private void deadrecall$pickUpSortableItem(PathfinderMob mob, Container container, CallbackInfo ci) {
        if (!(mob instanceof CopperGolem golem)
                || !CopperGolemWrenchHandler.hasBinding(golem)
                || !CopperGolemWrenchHandler.isTransportEnabled(golem)
                || !(mob.level() instanceof ServerLevel level)
                || target == null) {
            return;
        }

        ItemStack picked = CopperGolemWrenchHandler.pickUpNextItem(golem, level, container, target.pos());
        if (picked.isEmpty()) {
            stopTargetingCurrentTarget(mob);
            ci.cancel();
            return;
        }

        mob.setItemSlot(EquipmentSlot.MAINHAND, picked);
        mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        container.setChanged();
        clearMemoriesAfterMatchingTargetFound(mob);
        ci.cancel();
    }

    @Inject(method = "putDownItem", at = @At("TAIL"))
    private void deadrecall$clearSourceAfterPuttingDownItem(PathfinderMob mob, Container container, CallbackInfo ci) {
        if (mob instanceof CopperGolem golem && mob.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            CopperGolemWrenchHandler.clearRememberedSource(golem);
        }
    }
}
