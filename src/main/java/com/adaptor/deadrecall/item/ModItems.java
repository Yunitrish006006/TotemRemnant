package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.Deadrecall;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {
    // 註冊不同等級的背包
    public static final Item BACKPACK_BASIC = registerItem("backpack_basic",
        props -> new TieredBackpackItem(props.stacksTo(1), TieredBackpackItem.BackpackTier.BASIC));

    public static final Item BACKPACK_STANDARD = registerItem("backpack_standard",
        props -> new TieredBackpackItem(props.stacksTo(1), TieredBackpackItem.BackpackTier.STANDARD));

    public static final Item BACKPACK_ADVANCED = registerItem("backpack_advanced",
        props -> new TieredBackpackItem(props.stacksTo(1), TieredBackpackItem.BackpackTier.ADVANCED));

    public static final Item BACKPACK_NETHERITE = registerItem("backpack_netherite",
        props -> new TieredBackpackItem(props.stacksTo(1).fireResistant(), TieredBackpackItem.BackpackTier.NETHERITE));

    // 死亡背包 - 特殊的死亡掉落物品收集器
    public static final Item DEATH_BACKPACK = registerItem("death_backpack",
        props -> new DeathBackpackItem(props.stacksTo(1).fireResistant()));

    // 舊版物品 ID 相容（deadrecall:backpack）
    @Deprecated
    public static final Item BACKPACK = registerItem("backpack",
        props -> new TieredBackpackItem(props.stacksTo(1), TieredBackpackItem.BackpackTier.STANDARD));

    private static Item registerItem(String name, Function<Item.Properties, Item> itemFactory) {
        Identifier id = Identifier.fromNamespaceAndPath("deadrecall", name);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Item.Properties props = new Item.Properties().setId(itemKey);
        Item item = itemFactory.apply(props);
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void registerModItems() {
        Deadrecall.LOGGER.info("正在註冊模組物品...");
    }
}
