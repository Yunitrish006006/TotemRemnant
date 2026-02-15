package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.Deadrecall;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    // 註冊背包物品
    public static final Item BACKPACK = registerItem("backpack",
        new BackpackItem(new Item.Settings().maxCount(1)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of("deadrecall", name), item);
    }

    public static void registerModItems() {
        Deadrecall.LOGGER.info("正在註冊模組物品...");

        // 將背包添加到工具物品組
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(BACKPACK);
        });
    }
}

