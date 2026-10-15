package dev.totem.remnant.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

public final class RemnantDataComponents {
    public static final DataComponentType<Integer> SELECTED_ITEM = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath("totem", "remnant_selected_item"),
            DataComponentType.<Integer>builder().persistent(Codec.intRange(-1, 71))
                    .networkSynchronized(ByteBufCodecs.VAR_INT).build());
    private RemnantDataComponents() {}
    public static void register() {}
}
