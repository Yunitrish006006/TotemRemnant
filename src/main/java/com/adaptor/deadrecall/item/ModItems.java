package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.registry.LegacyGameplayItemRegistration;
import com.adaptor.deadrecall.registry.TotemAutomataItemRegistration;
import com.adaptor.deadrecall.registry.TotemRemnantItemRegistration;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final Item BACKPACK_BASIC = TotemRemnantItemRegistration.BACKPACK_BASIC;
    public static final Item BACKPACK_STANDARD = TotemRemnantItemRegistration.BACKPACK_STANDARD;
    public static final Item BACKPACK_ADVANCED = TotemRemnantItemRegistration.BACKPACK_ADVANCED;
    public static final Item BACKPACK_NETHERITE = TotemRemnantItemRegistration.BACKPACK_NETHERITE;
    public static final Item DEATH_BACKPACK = TotemRemnantItemRegistration.DEATH_BACKPACK;

    public static final Item SALTPETER = LegacyGameplayItemRegistration.SALTPETER;
    public static final Item PIG_MANURE = LegacyGameplayItemRegistration.PIG_MANURE;
    public static final Item WOOD_ASH = LegacyGameplayItemRegistration.WOOD_ASH;
    public static final Item COCOA_POWDER = LegacyGameplayItemRegistration.COCOA_POWDER;
    public static final Item HOT_COCOA = LegacyGameplayItemRegistration.HOT_COCOA;
    public static final Item CHERRY_BREW = LegacyGameplayItemRegistration.CHERRY_BREW;
    public static final Item STONE_BOWL = LegacyGameplayItemRegistration.STONE_BOWL;
    public static final Item SULFUR_BOWL = LegacyGameplayItemRegistration.SULFUR_BOWL;

    public static final Item COPPER_WRENCH = TotemAutomataItemRegistration.COPPER_WRENCH;

    public static void registerModItems() {
        Deadrecall.LOGGER.info("正在註冊模組物品...");
    }
}
