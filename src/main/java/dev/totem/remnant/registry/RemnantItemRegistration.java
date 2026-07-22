package dev.totem.remnant.registry;

import dev.totem.remnant.item.DeathBackpackItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Remnant item owner retaining legacy deadrecall resource identifiers. */
public final class RemnantItemRegistration {
    public static final Item DEATH_BACKPACK = Registry.register(BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath("deadrecall", "death_backpack"),
            new DeathBackpackItem(new Item.Properties().stacksTo(1).fireResistant()));
    private RemnantItemRegistration() { }
    public static void register() { }
}
