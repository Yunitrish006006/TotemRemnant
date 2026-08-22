package dev.totem.remnant.registry;

import dev.totem.remnant.item.DeathBackpackItem;
import dev.totem.remnant.item.TieredBackpackItem;
import dev.totem.remnant.upgrade.BackpackUpgradeItem;
import dev.totem.remnant.upgrade.BackpackUpgradeType;
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
            "totem", "remnant/backpack_basic", TieredBackpackItem.BackpackTier.BASIC);
    public static final Item BACKPACK_STANDARD = registerTiered(
            "totem", "remnant/backpack_standard", TieredBackpackItem.BackpackTier.STANDARD);
    public static final Item BACKPACK_ADVANCED = registerTiered(
            "totem", "remnant/backpack_advanced", TieredBackpackItem.BackpackTier.ADVANCED);
    public static final Item BACKPACK_NETHERITE = registerTiered(
            "totem", "remnant/backpack_netherite", TieredBackpackItem.BackpackTier.NETHERITE);
    public static final Item UPGRADE_CRAFTING = registerUpgrade(
            "remnant/upgrade_crafting", BackpackUpgradeType.CRAFTING);
    public static final Item UPGRADE_COMPACTION = registerUpgrade(
            "remnant/upgrade_compaction", BackpackUpgradeType.COMPACTION);
    public static final Item UPGRADE_MATCHING_PICKUP = registerUpgrade(
            "remnant/upgrade_matching_pickup", BackpackUpgradeType.MATCHING_PICKUP);
    public static final Item UPGRADE_CAPACITY = registerUpgrade(
            "remnant/upgrade_capacity", BackpackUpgradeType.CAPACITY);
    public static final Item UPGRADE_SOULBOUND_CHARGE = registerUpgrade(
            "remnant/upgrade_soulbound_charge", BackpackUpgradeType.SOULBOUND_CHARGE);
    public static final Item UPGRADE_ENDER_ACCESS = registerUpgrade(
            "remnant/upgrade_ender_access", BackpackUpgradeType.ENDER_ACCESS);
    /** Legacy hidden ID; existing stacks gain the combined impact-protection behavior. */
    public static final Item UPGRADE_CACTUS_PROTECTION = registerUpgrade(
            "remnant/upgrade_cactus_protection", BackpackUpgradeType.BLAST_PROTECTION);
    public static final Item UPGRADE_BLAST_PROTECTION = registerUpgrade(
            "remnant/upgrade_blast_protection", BackpackUpgradeType.BLAST_PROTECTION);
    public static final Item UPGRADE_FIRE_PROTECTION = registerUpgrade(
            "remnant/upgrade_fire_protection", BackpackUpgradeType.FIRE_PROTECTION);
    public static final Item UPGRADE_DESPAWN_PROTECTION = registerUpgrade(
            "remnant/upgrade_despawn_protection", BackpackUpgradeType.DESPAWN_PROTECTION);
    public static final Item UPGRADE_VOID_PROTECTION = registerUpgrade(
            "remnant/upgrade_void_protection", BackpackUpgradeType.VOID_PROTECTION);
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
            TieredBackpackItem.BackpackTier tier
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties properties = new Item.Properties().setId(key).stacksTo(1);
        if (tier == TieredBackpackItem.BackpackTier.NETHERITE) properties.fireResistant();
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

    private static Item registerUpgrade(String path, BackpackUpgradeType type) {
        Identifier id = Identifier.fromNamespaceAndPath("totem", path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            return BuiltInRegistries.ITEM.getValue(id);
        }
        return Registry.register(BuiltInRegistries.ITEM, id,
                new BackpackUpgradeItem(new Item.Properties().setId(key).stacksTo(16), type));
    }
}
