package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.Deadrecall;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroups {
    private static final Identifier DEADRECALL_TAB_ID = Identifier.fromNamespaceAndPath("deadrecall", "main");

    public static final ResourceKey<CreativeModeTab> DEADRECALL_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, DEADRECALL_TAB_ID);

    public static final CreativeModeTab DEADRECALL_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            DEADRECALL_TAB_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.deadrecall.main"))
                    .icon(() -> new ItemStack(ModItems.COPPER_WRENCH))
                    .build()
    );

    private ModItemGroups() {
    }

    public static void registerModItemGroups() {
        CreativeModeTabEvents.modifyOutputEvent(DEADRECALL_TAB_KEY).register(ModItemGroups::addDeadRecallItems);
        Deadrecall.LOGGER.info("正在註冊模組創造模式頁籤...");
    }

    private static void addDeadRecallItems(FabricCreativeModeTabOutput output) {
        add(output, ModItems.COPPER_WRENCH);
        add(output, ModItems.BACKPACK_BASIC);
        add(output, ModItems.BACKPACK_STANDARD);
        add(output, ModItems.BACKPACK_ADVANCED);
        add(output, ModItems.BACKPACK_NETHERITE);
        add(output, ModItems.DEATH_BACKPACK);
        add(output, ModItems.SALTPETER);
        add(output, ModItems.PIG_MANURE);
        add(output, ModItems.WOOD_ASH);
        add(output, ModItems.COCOA_POWDER);
        add(output, ModItems.HOT_COCOA);
        add(output, ModItems.CHERRY_BREW);
        add(output, ModItems.STONE_BOWL);
        add(output, ModItems.SULFUR_BOWL);
    }

    private static void add(FabricCreativeModeTabOutput output, Item item) {
        ItemStack stack = new ItemStack(item);
        output.getDisplayStacks().add(stack);
        output.getSearchTabStacks().add(stack);
    }
}
