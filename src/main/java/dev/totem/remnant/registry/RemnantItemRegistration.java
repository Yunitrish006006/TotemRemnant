package dev.totem.remnant.registry;

import dev.totem.remnant.item.DeathBackpackItem;
import dev.totem.remnant.item.TieredBackpackItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Remnant item owner retaining legacy deadrecall resource identifiers. */
public final class RemnantItemRegistration {
    public static final Item BACKPACK_BASIC = registerTiered("backpack_basic", TieredBackpackItem.BackpackTier.BASIC, false);
    public static final Item BACKPACK_STANDARD = registerTiered("backpack_standard", TieredBackpackItem.BackpackTier.STANDARD, false);
    public static final Item BACKPACK_ADVANCED = registerTiered("backpack_advanced", TieredBackpackItem.BackpackTier.ADVANCED, false);
    public static final Item BACKPACK_NETHERITE = registerTiered("backpack_netherite", TieredBackpackItem.BackpackTier.NETHERITE, true);
    private static final Identifier DEATH_BACKPACK_ID = Identifier.fromNamespaceAndPath("deadrecall", "death_backpack");
    private static final ResourceKey<Item> DEATH_BACKPACK_KEY = ResourceKey.create(Registries.ITEM, DEATH_BACKPACK_ID);
    public static final Item DEATH_BACKPACK = BuiltInRegistries.ITEM.getOptional(DEATH_BACKPACK_KEY)
            .orElseGet(() -> Registry.register(BuiltInRegistries.ITEM,
                    DEATH_BACKPACK_ID,
                    new DeathBackpackItem(new Item.Properties()
                            .setId(DEATH_BACKPACK_KEY)
                            .stacksTo(1)
                            .fireResistant())));
    private RemnantItemRegistration() { }
    public static void register() { }

    private static Item registerTiered(String path, TieredBackpackItem.BackpackTier tier, boolean fireResistant) {
        Identifier id = Identifier.fromNamespaceAndPath("deadrecall", path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties properties = new Item.Properties().setId(key).stacksTo(1);
        if (fireResistant) properties.fireResistant();
        return BuiltInRegistries.ITEM.getOptional(key).orElseGet(() ->
                Registry.register(BuiltInRegistries.ITEM, id, new TieredBackpackItem(properties, tier)));
    }
}
