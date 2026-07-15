package com.adaptor.deadrecall.item.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class CopperGolemData {
    static final int DATA_VERSION = 2;

    static final String TAG_BOUND_CONTAINER_DIM = "deadrecall_bound_container_dim";
    static final String TAG_BOUND_CONTAINER_X = "deadrecall_bound_container_x";
    static final String TAG_BOUND_CONTAINER_Y = "deadrecall_bound_container_y";
    static final String TAG_BOUND_CONTAINER_Z = "deadrecall_bound_container_z";
    static final String TAG_BOUND_CONTAINERS = "deadrecall_bound_containers";
    static final String TAG_SOURCE_COPPER_CONTAINER_DIM = "deadrecall_source_copper_container_dim";
    static final String TAG_SOURCE_COPPER_CONTAINER_X = "deadrecall_source_copper_container_x";
    static final String TAG_SOURCE_COPPER_CONTAINER_Y = "deadrecall_source_copper_container_y";
    static final String TAG_SOURCE_COPPER_CONTAINER_Z = "deadrecall_source_copper_container_z";
    static final String TAG_BINDING_DIM = "dimension";
    static final String TAG_BINDING_X = "x";
    static final String TAG_BINDING_Y = "y";
    static final String TAG_BINDING_Z = "z";
    static final String TAG_DATA_VERSION = "deadrecall_data_version";
    static final String TAG_REVISION = "deadrecall_revision";
    static final String TAG_MODE = "deadrecall_mode";
    static final String TAG_TRANSPORT_ENABLED = "deadrecall_transport_enabled";
    static final String TAG_ACTIVITY = "deadrecall_activity";
    static final String TAG_FUEL_STACK = "deadrecall_fuel_stack";
    static final String TAG_FUEL_TICKS = "deadrecall_fuel_ticks";
    static final String TAG_SORTING_BLOCKED = "deadrecall_sorting_blocked";
    static final String TAG_BLOCKED_SOURCE_CONTAINER_DIM = "deadrecall_blocked_source_container_dim";
    static final String TAG_BLOCKED_SOURCE_CONTAINER_X = "deadrecall_blocked_source_container_x";
    static final String TAG_BLOCKED_SOURCE_CONTAINER_Y = "deadrecall_blocked_source_container_y";
    static final String TAG_BLOCKED_SOURCE_CONTAINER_Z = "deadrecall_blocked_source_container_z";
    static final String TAG_BLOCKED_SOURCE_HASH = "deadrecall_blocked_source_hash";
    static final String TAG_BLOCKED_BINDINGS_HASH = "deadrecall_blocked_bindings_hash";
    static final String TAG_BLOCKED_TARGETS_HASH = "deadrecall_blocked_targets_hash";

    private CopperGolemData() {
    }

    static CompoundTag readStackTag(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customData.copyTag();
    }

    static void writeStackTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    static CompoundTag readEntityTag(Entity entity) {
        CustomData customData = entity.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    static void writeEntityTag(Entity entity, CompoundTag tag) {
        entity.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    static void migrate(CopperGolem golem) {
        CompoundTag tag = readEntityTag(golem);
        if (migrate(tag)) {
            writeEntityTag(golem, tag);
        }
    }

    static boolean migrate(CompoundTag tag) {
        boolean changed = false;
        if (tag.getIntOr(TAG_DATA_VERSION, 0) < DATA_VERSION) {
            tag.putInt(TAG_DATA_VERSION, DATA_VERSION);
            changed = true;
        }
        if (!tag.contains(TAG_MODE)) {
            tag.putString(TAG_MODE, CopperGolemMode.SORTING.id());
            changed = true;
        }
        if (!tag.contains(TAG_REVISION)) {
            tag.putInt(TAG_REVISION, 0);
            changed = true;
        }
        if (migrateLegacySortingBindings(tag)) {
            changed = true;
        }
        return changed;
    }

    static boolean migrateLegacySortingBindings(CompoundTag tag) {
        boolean hasLegacyBinding = tag.contains(TAG_BOUND_CONTAINER_DIM)
                || tag.contains(TAG_BOUND_CONTAINER_X)
                || tag.contains(TAG_BOUND_CONTAINER_Y)
                || tag.contains(TAG_BOUND_CONTAINER_Z);
        if (!hasLegacyBinding) {
            return false;
        }

        List<CopperGolemWrenchHandler.Binding> bindings = new ArrayList<>(readBindingList(tag, TAG_BOUND_CONTAINERS));
        readBinding(tag, TAG_BOUND_CONTAINER_DIM, TAG_BOUND_CONTAINER_X, TAG_BOUND_CONTAINER_Y, TAG_BOUND_CONTAINER_Z)
                .filter(binding -> !bindings.contains(binding))
                .ifPresent(bindings::add);
        writeBindings(tag, bindings);
        return true;
    }

    static int revision(CopperGolem golem) {
        return readEntityTag(golem).getIntOr(TAG_REVISION, 0);
    }

    static void bumpRevision(CompoundTag tag) {
        tag.putInt(TAG_REVISION, tag.getIntOr(TAG_REVISION, 0) + 1);
        tag.putInt(TAG_DATA_VERSION, DATA_VERSION);
    }

    static CopperGolemMode mode(CopperGolem golem) {
        migrate(golem);
        return CopperGolemMode.fromId(readEntityTag(golem).getStringOr(TAG_MODE, CopperGolemMode.SORTING.id()));
    }

    static boolean running(CopperGolem golem) {
        return readEntityTag(golem).getBooleanOr(TAG_TRANSPORT_ENABLED, true);
    }

    static ItemStack fuelStack(CopperGolem golem) {
        return readItemStack(readEntityTag(golem), TAG_FUEL_STACK);
    }

    static int fuelTicks(CopperGolem golem) {
        return readEntityTag(golem).getIntOr(TAG_FUEL_TICKS, 0);
    }

    static CopperGolemActivity activity(CopperGolem golem) {
        return CopperGolemActivity.fromId(readEntityTag(golem).getStringOr(TAG_ACTIVITY, ""));
    }

    static List<CopperGolemWrenchHandler.Binding> readBindings(CompoundTag tag) {
        List<CopperGolemWrenchHandler.Binding> bindings = new ArrayList<>(readBindingList(tag, TAG_BOUND_CONTAINERS));

        readBinding(tag, TAG_BOUND_CONTAINER_DIM, TAG_BOUND_CONTAINER_X, TAG_BOUND_CONTAINER_Y, TAG_BOUND_CONTAINER_Z)
                .filter(binding -> !bindings.contains(binding))
                .ifPresent(bindings::add);
        return List.copyOf(bindings);
    }

    static List<CopperGolemWrenchHandler.Binding> readBindingList(CompoundTag tag, String listKey) {
        List<CopperGolemWrenchHandler.Binding> bindings = new ArrayList<>();
        tag.getList(listKey).ifPresent(list -> {
            for (CompoundTag bindingTag : list.compoundStream().toList()) {
                readBinding(bindingTag, TAG_BINDING_DIM, TAG_BINDING_X, TAG_BINDING_Y, TAG_BINDING_Z)
                        .ifPresent(bindings::add);
            }
        });
        return bindings;
    }

    static Optional<CopperGolemWrenchHandler.Binding> readBinding(CompoundTag tag, String dimKey, String xKey, String yKey, String zKey) {
        if (!tag.contains(dimKey) || !tag.contains(xKey) || !tag.contains(yKey) || !tag.contains(zKey)) {
            return Optional.empty();
        }

        Identifier dimensionId = Identifier.tryParse(tag.getStringOr(dimKey, ""));
        if (dimensionId == null) {
            return Optional.empty();
        }

        return Optional.of(new CopperGolemWrenchHandler.Binding(
                net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimensionId),
                new BlockPos(tag.getIntOr(xKey, 0), tag.getIntOr(yKey, 0), tag.getIntOr(zKey, 0))
        ));
    }

    static void writeBindings(CompoundTag tag, List<CopperGolemWrenchHandler.Binding> bindings) {
        writeBindingList(tag, TAG_BOUND_CONTAINERS, bindings);
        tag.remove(TAG_BOUND_CONTAINER_DIM);
        tag.remove(TAG_BOUND_CONTAINER_X);
        tag.remove(TAG_BOUND_CONTAINER_Y);
        tag.remove(TAG_BOUND_CONTAINER_Z);
    }

    static void writeBindingList(CompoundTag tag, String listKey, List<CopperGolemWrenchHandler.Binding> bindings) {
        ListTag list = new ListTag();
        for (CopperGolemWrenchHandler.Binding binding : bindings) {
            CompoundTag bindingTag = new CompoundTag();
            writeBinding(bindingTag, binding, TAG_BINDING_DIM, TAG_BINDING_X, TAG_BINDING_Y, TAG_BINDING_Z);
            list.add(bindingTag);
        }

        tag.put(listKey, list);
    }

    static void writeBinding(CompoundTag tag, CopperGolemWrenchHandler.Binding binding, String dimKey, String xKey, String yKey, String zKey) {
        tag.putString(dimKey, binding.dimension().identifier().toString());
        tag.putInt(xKey, binding.containerPos().getX());
        tag.putInt(yKey, binding.containerPos().getY());
        tag.putInt(zKey, binding.containerPos().getZ());
    }

    static Optional<BlockPos> readBlockPos(CompoundTag tag, String xKey, String yKey, String zKey) {
        if (!tag.contains(xKey) || !tag.contains(yKey) || !tag.contains(zKey)) {
            return Optional.empty();
        }

        return Optional.of(new BlockPos(tag.getIntOr(xKey, 0), tag.getIntOr(yKey, 0), tag.getIntOr(zKey, 0)));
    }

    static void writeBlockPos(CompoundTag tag, BlockPos pos, String xKey, String yKey, String zKey) {
        tag.putInt(xKey, pos.getX());
        tag.putInt(yKey, pos.getY());
        tag.putInt(zKey, pos.getZ());
    }

    static ItemStack readItemStack(CompoundTag tag, String key) {
        return tag.read(key, ItemStack.OPTIONAL_CODEC)
                .orElse(ItemStack.EMPTY)
                .copy();
    }

    static void writeItemStack(CompoundTag tag, String key, ItemStack stack) {
        if (stack.isEmpty()) {
            tag.remove(key);
        } else {
            tag.store(key, ItemStack.OPTIONAL_CODEC, stack.copy());
        }
    }

    static List<String> readStringList(CompoundTag tag, String key) {
        List<String> values = new ArrayList<>();
        tag.getList(key).ifPresent(list -> {
            for (int i = 0; i < list.size(); i++) {
                String value = list.getStringOr(i, "");
                if (!value.isBlank() && !values.contains(value)) {
                    values.add(value);
                }
            }
        });
        return List.copyOf(values);
    }

    static void writeStringList(CompoundTag tag, String key, List<String> values, int valueLimit) {
        ListTag list = new ListTag();
        for (String value : values) {
            if (list.size() >= valueLimit) {
                break;
            }
            if (!value.isBlank()) {
                list.add(StringTag.valueOf(value));
            }
        }

        if (list.isEmpty()) {
            tag.remove(key);
        } else {
            tag.put(key, list);
        }
    }

    static void putOrRemoveString(CompoundTag tag, String key, String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            tag.remove(key);
        } else {
            tag.putString(key, normalized);
        }
    }

    static boolean removeSortingBlockedTags(CompoundTag tag) {
        boolean hadBlockedState = tag.getBooleanOr(TAG_SORTING_BLOCKED, false)
                || tag.contains(TAG_BLOCKED_SOURCE_CONTAINER_DIM)
                || tag.contains(TAG_BLOCKED_SOURCE_HASH)
                || tag.contains(TAG_BLOCKED_BINDINGS_HASH)
                || tag.contains(TAG_BLOCKED_TARGETS_HASH);

        tag.remove(TAG_SORTING_BLOCKED);
        tag.remove(TAG_BLOCKED_SOURCE_CONTAINER_DIM);
        tag.remove(TAG_BLOCKED_SOURCE_CONTAINER_X);
        tag.remove(TAG_BLOCKED_SOURCE_CONTAINER_Y);
        tag.remove(TAG_BLOCKED_SOURCE_CONTAINER_Z);
        tag.remove(TAG_BLOCKED_SOURCE_HASH);
        tag.remove(TAG_BLOCKED_BINDINGS_HASH);
        tag.remove(TAG_BLOCKED_TARGETS_HASH);
        return hadBlockedState;
    }
}
