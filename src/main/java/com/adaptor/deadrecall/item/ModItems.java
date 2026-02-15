package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.Deadrecall;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    // 註冊不同等級的背包
    public static final Item BACKPACK_BASIC = registerItem("backpack_basic",
        new TieredBackpackItem(new Item.Settings().maxCount(1), TieredBackpackItem.BackpackTier.BASIC));

    public static final Item BACKPACK_STANDARD = registerItem("backpack_standard",
        new TieredBackpackItem(new Item.Settings().maxCount(1), TieredBackpackItem.BackpackTier.STANDARD));

    public static final Item BACKPACK_ADVANCED = registerItem("backpack_advanced",
        new TieredBackpackItem(new Item.Settings().maxCount(1), TieredBackpackItem.BackpackTier.ADVANCED));

    public static final Item BACKPACK_NETHERITE = registerItem("backpack_netherite",
        new TieredBackpackItem(new Item.Settings().maxCount(1).fireproof(), TieredBackpackItem.BackpackTier.NETHERITE));

    // 死亡背包 - 特殊的死亡掉落物品收集器
    public static final Item DEATH_BACKPACK = registerItem("death_backpack",
        new DeathBackpackItem(new Item.Settings().maxCount(1).fireproof()));

    // 保留舊的背包物品以兼容性（指向標準背包）
    @Deprecated
    public static final Item BACKPACK = BACKPACK_STANDARD;

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of("deadrecall", name), item);
    }

    public static void registerModItems() {
        Deadrecall.LOGGER.info("正在註冊模組物品...");

        // 將所有背包添加到工具物品組
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(BACKPACK_BASIC);
            content.add(BACKPACK_STANDARD);
            content.add(BACKPACK_ADVANCED);
            content.add(BACKPACK_NETHERITE);
        });

        // 同時添加到功能性物品組，讓創造模式更容易找到
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> {
            content.add(BACKPACK_BASIC);
            content.add(BACKPACK_STANDARD);
            content.add(BACKPACK_ADVANCED);
            content.add(BACKPACK_NETHERITE);
        });
    }
}
