package dev.totem.remnant.registry;

import dev.totem.remnant.item.DeathBackpackItem;
import dev.totem.remnant.item.TieredBackpackItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Remnant item owner exposing canonical IDs. Legacy aliases are owned by DeadRecall. */
public final class RemnantItemRegistration {
    private static final Identifier BACKPACK_BASIC_ID =
            Identifier.fromNamespaceAndPath("totem", "remnant/backpack_basic");
    public static final Item BACKPACK_BASIC = registerTiered(
            "totem", "remnant/backpack_basic", TieredBackpackItem.BackpackTier.BASIC, false);
    public static final Item BACKPACK_STANDARD = registerTiered(
            "totem", "remnant/backpack_standard", TieredBackpackItem.BackpackTier.STANDARD, false);
    public static final Item BACKPACK_ADVANCED = registerTiered(
            "totem", "remnant/backpack_advanced", TieredBackpackItem.BackpackTier.ADVANCED, false);
    public static final Item BACKPACK_NETHERITE = registerTiered(
            "totem", "remnant/backpack_netherite", TieredBackpackItem.BackpackTier.NETHERITE, true);
    public static final Item DEATH_BACKPACK = registerDeathBackpack("totem", "remnant/death_backpack");

    private RemnantItemRegistration() { }
    public static void register() {
        Identifier actual = BuiltInRegistries.ITEM.getKey(BACKPACK_BASIC);
        if (!BACKPACK_BASIC_ID.equals(actual)) {
            throw new IllegalStateException(
                    "Remnant canonical item registration failed: expected "
                            + BACKPACK_BASIC_ID + ", got " + actual
            );
        }
    }

    private static Item registerTiered(
            String namespace,
            String path,
            TieredBackpackItem.BackpackTier tier,
            boolean fireResistant
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties properties = new Item.Properties().setId(key).stacksTo(1);
        if (fireResistant) properties.fireResistant();
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            return BuiltInRegistries.ITEM.getValue(id);
        }
        return Registry.register(
                BuiltInRegistries.ITEM,
                id,
                new TieredBackpackItem(properties, tier)
        );
    }

    private static Item registerDeathBackpack(String namespace, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            return BuiltInRegistries.ITEM.getValue(id);
        }
        return Registry.register(
                BuiltInRegistries.ITEM,
                id,
                new DeathBackpackItem(
                        new Item.Properties().setId(key).stacksTo(1).fireResistant()
                )
        );
    }
}
