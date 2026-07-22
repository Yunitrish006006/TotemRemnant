package com.adaptor.deadrecall.api.death;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.UUID;

/** Stable Remnant-owned binding for an optional external death-node adapter. */
public final class DeathBackpackNodeBinding {
    private static final String KEY = "deadrecall_space_death_node_id";
    private DeathBackpackNodeBinding() { }
    public static void write(ItemStack backpack, UUID nodeId) {
        if (backpack.isEmpty() || nodeId == null) return;
        CompoundTag tag = backpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.store(KEY, UUIDUtil.CODEC, nodeId);
        backpack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    public static UUID read(ItemStack backpack) {
        CompoundTag tag = backpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.read(KEY, UUIDUtil.CODEC).orElse(null);
    }
}
