package com.adaptor.deadrecall.item.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

final class SortingModeController {
    private static final int SORTING_BLOCKED_JUMP_INTERVAL_TICKS = 10;

    private SortingModeController() {
    }

    static boolean isBindingInLevel(CopperGolem golem, Level level) {
        for (CopperGolemWrenchHandler.Binding binding : CopperGolemWrenchHandler.getBindings(golem)) {
            if (binding.dimension().equals(level.dimension())) {
                return true;
            }
        }
        return false;
    }

    static boolean isBoundContainer(CopperGolem golem, Level level, BlockPos pos) {
        for (CopperGolemWrenchHandler.Binding binding : CopperGolemWrenchHandler.getBindings(golem)) {
            if (binding.dimension().equals(level.dimension()) && binding.containerPos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    static Optional<TransportItemTarget> findNextDestinationTarget(CopperGolem golem, ServerLevel level, ItemStack carried) {
        if (carried.isEmpty()) {
            return Optional.empty();
        }

        List<CopperGolemWrenchHandler.Binding> triedDestinations = CopperGolemWrenchHandler.getTriedDestinations(golem);
        Optional<CopperGolemWrenchHandler.Source> source = CopperGolemWrenchHandler.getRememberedSource(golem);
        for (CopperGolemWrenchHandler.Binding binding : CopperGolemWrenchHandler.getBindings(golem)) {
            if (!binding.dimension().equals(level.dimension())
                    || triedDestinations.contains(binding)
                    || source.filter(value -> value.dimension().equals(binding.dimension()) && value.containerPos().equals(binding.containerPos())).isPresent()) {
                continue;
            }

            TransportItemTarget target = CopperGolemWrenchHandler.tryCreateBoundTarget(level, binding.containerPos());
            if (target == null || !CopperGolemWrenchHandler.canSortInto(golem, level, binding, target.container(), carried)) {
                CopperGolemWrenchHandler.rememberTriedDestination(golem, binding);
                continue;
            }

            CopperGolemWrenchHandler.rememberTriedDestination(golem, binding);
            return Optional.of(target);
        }

        return Optional.empty();
    }

    static ItemStack pickUpNextItem(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos) {
        if (!CopperGolemWrenchHandler.hasFuelAvailable(golem, level)) {
            return ItemStack.EMPTY;
        }

        if (CopperGolemWrenchHandler.hasItems(source)
                && !CopperGolemWrenchHandler.hasAnySortableItem(golem, level, source, sourcePos)) {
            CopperGolemWrenchHandler.markSortingBlocked(golem, level, sourcePos, source);
            return ItemStack.EMPTY;
        }

        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack picked = source.removeItem(slot, Math.min(stack.getCount(), CopperGolemWrenchHandler.transportStorageMaxStackSize()));
            if (!picked.isEmpty()) {
                CopperGolemWrenchHandler.rememberSource(golem, level, sourcePos, slot);
                CopperGolemWrenchHandler.consumeFuelForTransport(golem, level);
            }
            return picked;
        }

        return ItemStack.EMPTY;
    }

    static boolean returnCarriedItemToSource(CopperGolem golem, ServerLevel level) {
        ItemStack carried = golem.getMainHandItem();
        if (carried.isEmpty()) {
            clearRememberedSource(golem);
            return true;
        }

        Optional<CopperGolemWrenchHandler.Source> source = CopperGolemWrenchHandler.getRememberedSource(golem);
        if (source.isEmpty() || !source.get().dimension().equals(level.dimension())) {
            return false;
        }

        TransportItemTarget target = TransportItemTarget.tryCreatePossibleTarget(source.get().containerPos(), level);
        if (target == null) {
            return false;
        }

        ItemStack remaining = CopperGolemWrenchHandler.moveCarriedStackToSourceBack(target.container(), carried, source.get().slot());
        target.container().setChanged();
        golem.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, remaining);
        if (remaining.isEmpty()) {
            clearRememberedSource(golem);
            return true;
        }

        return false;
    }

    static Optional<ItemStack> putCarriedItemIntoDestination(CopperGolem golem, ServerLevel level, BlockPos targetPos, Container container) {
        ItemStack carried = golem.getMainHandItem();
        if (carried.isEmpty()) {
            return Optional.empty();
        }

        Optional<CopperGolemWrenchHandler.Binding> binding = CopperGolemWrenchHandler.getBindings(golem).stream()
                .filter(value -> value.dimension().equals(level.dimension()) && value.containerPos().equals(targetPos))
                .findFirst();
        if (binding.isEmpty()) {
            return Optional.empty();
        }

        CopperGolemWrenchHandler.BindingLlmConfig config = CopperGolemWrenchHandler.getBindingLlmConfig(golem, binding.get());
        Optional<Boolean> cachedDecision = CopperGolemWrenchHandler.getCachedLlmDecision(
                config,
                CopperGolemLlmService.itemId(carried),
                CopperGolemLlmService.itemTags(carried));
        if (cachedDecision.isPresent() && !cachedDecision.get()) {
            return Optional.of(carried);
        }

        ItemStack remaining = carried.copy();
        CopperGolemWrenchHandler.NestedBackpackTarget nestedTarget =
                CopperGolemWrenchHandler.findNestedBackpackTarget(golem, binding.get(), container, carried);
        if (nestedTarget != null) {
            remaining = CopperGolemWrenchHandler.insertIntoBackpack(nestedTarget.backpackStack(), remaining);
            container.setItem(nestedTarget.containerSlot(), nestedTarget.backpackStack());
        }

        if (!remaining.isEmpty()) {
            remaining = CopperGolemWrenchHandler.insertIntoDestinationContainer(golem, binding.get(), container, remaining);
        }

        if (remaining.getCount() < carried.getCount()) {
            container.setChanged();
        }
        return Optional.of(remaining);
    }

    static void clearRememberedSource(CopperGolem golem) {
        CopperGolemWrenchHandler.clearRememberedSourceData(golem);
    }

    static void tickBlocked(CopperGolem golem, ServerLevel level) {
        if (golem.tickCount % SORTING_BLOCKED_JUMP_INTERVAL_TICKS == 0
                && CopperGolemWrenchHandler.shouldClearSortingBlocked(golem, level)) {
            CopperGolemWrenchHandler.clearSortingBlocked(golem);
            return;
        }

        golem.getNavigation().stop();
        golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        golem.setDeltaMovement(0.0, golem.getDeltaMovement().y, 0.0);

        if (golem.onGround() && golem.tickCount % SORTING_BLOCKED_JUMP_INTERVAL_TICKS == 0) {
            golem.jumpFromGround();
            golem.setJumping(true);
        }
    }
}
