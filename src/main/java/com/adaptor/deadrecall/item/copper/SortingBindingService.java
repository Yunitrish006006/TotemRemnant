package com.adaptor.deadrecall.item.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

final class SortingBindingService {
    private SortingBindingService() {
    }

    static Optional<CopperGolemWrenchHandler.Binding> getBinding(CopperGolem golem) {
        List<CopperGolemWrenchHandler.Binding> bindings = getBindings(golem);
        return bindings.isEmpty() ? Optional.empty() : Optional.of(bindings.getFirst());
    }

    static List<CopperGolemWrenchHandler.Binding> getBindings(CopperGolem golem) {
        return CopperGolemData.readBindings(CopperGolemData.readEntityTag(golem));
    }

    static boolean hasBindings(CopperGolem golem) {
        return !getBindings(golem).isEmpty();
    }

    static Optional<CopperGolemWrenchHandler.Binding> getSourceContainer(CopperGolem golem) {
        return readSourceContainer(CopperGolemData.readEntityTag(golem));
    }

    static boolean hasSourceContainer(CopperGolem golem) {
        return getSourceContainer(golem).isPresent();
    }

    static boolean isSourceContainer(CopperGolem golem, Level level, BlockPos pos) {
        return getSourceContainer(golem)
                .filter(binding -> binding.dimension().equals(level.dimension()) && binding.containerPos().equals(pos))
                .isPresent();
    }

    static Optional<CopperGolemWrenchHandler.Binding> readSourceContainer(CompoundTag tag) {
        return CopperGolemData.readBinding(
                tag,
                CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_DIM,
                CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_X,
                CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_Y,
                CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_Z);
    }

    static void writeBindings(CompoundTag tag, List<CopperGolemWrenchHandler.Binding> bindings) {
        CopperGolemData.writeBindings(tag, bindings);
    }

    static void writeSourceContainer(CompoundTag tag, CopperGolemWrenchHandler.Binding binding) {
        CopperGolemData.writeBinding(
                tag,
                binding,
                CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_DIM,
                CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_X,
                CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_Y,
                CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_Z);
    }

    static void clearSourceContainer(CompoundTag tag) {
        tag.remove(CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_DIM);
        tag.remove(CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_X);
        tag.remove(CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_Y);
        tag.remove(CopperGolemData.TAG_SOURCE_COPPER_CONTAINER_Z);
    }
}
