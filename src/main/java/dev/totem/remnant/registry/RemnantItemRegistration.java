package dev.totem.remnant.registry;

import dev.totem.remnant.item.DeathBackpackItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Remnant item owner retaining legacy deadrecall resource identifiers. */
public final class RemnantItemRegistration {
    private static final Identifier DEATH_BACKPACK_ID = Identifier.fromNamespaceAndPath("deadrecall", "death_backpack");
    public static final Item DEATH_BACKPACK = Registry.register(BuiltInRegistries.ITEM,
            DEATH_BACKPACK_ID,
            new DeathBackpackItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, DEATH_BACKPACK_ID))
                    .stacksTo(1)
                    .fireResistant()));
    private RemnantItemRegistration() { }
    public static void register() { }
}
