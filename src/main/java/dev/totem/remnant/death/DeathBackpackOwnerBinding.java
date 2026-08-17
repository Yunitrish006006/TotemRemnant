package dev.totem.remnant.death;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

/** Persists the deceased player's identity on the backpack item itself. */
public final class DeathBackpackOwnerBinding {
    private static final String KEY = "totem_remnant_death_backpack_owner";

    private DeathBackpackOwnerBinding() {
    }

    public static void write(ItemStack backpack, UUID ownerId) {
        if (backpack.isEmpty() || ownerId == null) return;
        CompoundTag tag = backpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.store(KEY, UUIDUtil.CODEC, ownerId);
        backpack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static UUID read(ItemStack backpack) {
        CompoundTag tag = backpack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.read(KEY, UUIDUtil.CODEC).orElse(null);
    }
}
