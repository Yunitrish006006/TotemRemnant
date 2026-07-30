package dev.totem.remnant.registry;

import dev.totem.remnant.item.DeathBackpackItem;
import dev.totem.remnant.item.TieredBackpackItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Remnant item owner exposing canonical IDs while retaining legacy save compatibility. */
public final class RemnantItemRegistration {
    public static final Item BACKPACK_BASIC = registerTiered(
            "totem", "remnant/backpack_basic", TieredBackpackItem.BackpackTier.BASIC, false);
    public static final Item BACKPACK_STANDARD = registerTiered(
            "totem", "remnant/backpack_standard", TieredBackpackItem.BackpackTier.STANDARD, false);
    public static final Item BACKPACK_ADVANCED = registerTiered(
            "totem", "remnant/backpack_advanced", TieredBackpackItem.BackpackTier.ADVANCED, false);
    public static final Item BACKPACK_NETHERITE = registerTiered(
            "totem", "remnant/backpack_netherite", TieredBackpackItem.BackpackTier.NETHERITE, true);
    public static final Item DEATH_BACKPACK = registerDeathBackpack("totem", "remnant/death_backpack");

    public static final Item LEGACY_BACKPACK_BASIC = registerTiered(
            "deadrecall", "backpack_basic", TieredBackpackItem.BackpackTier.BASIC, false);
    public static final Item LEGACY_BACKPACK_STANDARD = registerTiered(
            "deadrecall", "backpack_standard", TieredBackpackItem.BackpackTier.STANDARD, false);
    public static final Item LEGACY_BACKPACK_ADVANCED = registerTiered(
            "deadrecall", "backpack_advanced", TieredBackpackItem.BackpackTier.ADVANCED, false);
    public static final Item LEGACY_BACKPACK_NETHERITE = registerTiered(
            "deadrecall", "backpack_netherite", TieredBackpackItem.BackpackTier.NETHERITE, true);
    public static final Item LEGACY_DEATH_BACKPACK = registerDeathBackpack("deadrecall", "death_backpack");

    private RemnantItemRegistration() { }
    public static void register() { }

    /**
     * Converts a legacy backpack to its canonical item while retaining its full component patch.
     * Non-legacy stacks are returned unchanged so callers can use identity to detect migration.
     */
    public static ItemStack migrateLegacy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return stack;
        }
        Item canonical = canonicalItem(stack.getItem());
        return canonical == null ? stack : stack.transmuteCopy(canonical, stack.getCount());
    }

    public static Item canonicalItem(Item item) {
        if (item == LEGACY_BACKPACK_BASIC) return BACKPACK_BASIC;
        if (item == LEGACY_BACKPACK_STANDARD) return BACKPACK_STANDARD;
        if (item == LEGACY_BACKPACK_ADVANCED) return BACKPACK_ADVANCED;
        if (item == LEGACY_BACKPACK_NETHERITE) return BACKPACK_NETHERITE;
        if (item == LEGACY_DEATH_BACKPACK) return DEATH_BACKPACK;
        return null;
    }

    public static boolean isLegacy(ItemStack stack) {
        return stack != null && !stack.isEmpty() && canonicalItem(stack.getItem()) != null;
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
        return BuiltInRegistries.ITEM.getOptional(key).orElseGet(() ->
                Registry.register(BuiltInRegistries.ITEM, id, new TieredBackpackItem(properties, tier)));
    }

    private static Item registerDeathBackpack(String namespace, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return BuiltInRegistries.ITEM.getOptional(key).orElseGet(() ->
                Registry.register(BuiltInRegistries.ITEM, id, new DeathBackpackItem(
                        new Item.Properties().setId(key).stacksTo(1).fireResistant())));
    }
}
