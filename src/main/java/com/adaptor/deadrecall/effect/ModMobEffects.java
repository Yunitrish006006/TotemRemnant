package com.adaptor.deadrecall.effect;

import com.adaptor.deadrecall.Deadrecall;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;

public final class ModMobEffects {
    public static final Holder.Reference<MobEffect> STINKY = register("stinky", new StinkyMobEffect());
    public static final Holder.Reference<MobEffect> CHERRY_BLOOM = register("cherry_bloom", new CherryBloomMobEffect());

    private ModMobEffects() {
    }

    private static Holder.Reference<MobEffect> register(String name, MobEffect effect) {
        Identifier id = Identifier.fromNamespaceAndPath("deadrecall", name);
        ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, id);
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, key, effect);
    }

    public static void registerModEffects() {
        Deadrecall.LOGGER.info("正在註冊模組狀態效果...");
    }
}
