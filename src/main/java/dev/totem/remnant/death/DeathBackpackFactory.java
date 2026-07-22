package dev.totem.remnant.death;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Remnant-owned seam that creates the death-backpack stack for capture transactions. */
@FunctionalInterface
public interface DeathBackpackFactory {
    ItemStack create(List<ItemStack> contents);

    static void register(DeathBackpackFactory factory) { Holder.factory = factory; }
    static DeathBackpackFactory current() { return Holder.factory; }

    final class Holder {
        private static volatile DeathBackpackFactory factory;
        private Holder() { }
    }
}
