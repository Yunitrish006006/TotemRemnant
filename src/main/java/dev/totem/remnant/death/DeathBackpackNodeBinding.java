package dev.totem.remnant.death;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

/** Stable on-item node binding with one-way legacy-key migration. */
public final class DeathBackpackNodeBinding {
    private static final String KEY = "totem_remnant_space_death_node_id";
    private static final String LEGACY_KEY = "deadrecall_space_death_node_id";

    private DeathBackpackNodeBinding() { }

    public static void write(ItemStack backpack, UUID nodeId) {
        if (backpack.isEmpty() || nodeId == null) return;
        CompoundTag tag = backpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.store(KEY, UUIDUtil.CODEC, nodeId);
        tag.remove(LEGACY_KEY);
        backpack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static UUID read(ItemStack backpack) {
        CompoundTag tag = backpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID nodeId = tag.read(KEY, UUIDUtil.CODEC).orElse(null);
        if (nodeId == null) {
            nodeId = tag.read(LEGACY_KEY, UUIDUtil.CODEC).orElse(null);
        }
        if (nodeId != null && tag.contains(LEGACY_KEY)) {
            tag.store(KEY, UUIDUtil.CODEC, nodeId);
            tag.remove(LEGACY_KEY);
            backpack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return nodeId;
    }
}
