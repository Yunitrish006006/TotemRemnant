package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.registry.LegacyGameplayItemGroupRegistration;
import com.adaptor.deadrecall.registry.TotemAutomataItemGroupRegistration;
import com.adaptor.deadrecall.registry.TotemAutomataItemRegistration;
import com.adaptor.deadrecall.registry.TotemRemnantItemGroupRegistration;
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
                    .icon(() -> new ItemStack(TotemAutomataItemRegistration.COPPER_WRENCH))
                    .build()
    );

    private ModItemGroups() {
    }

    public static void registerModItemGroups() {
        CreativeModeTabEvents.modifyOutputEvent(DEADRECALL_TAB_KEY).register(ModItemGroups::addDeadRecallItems);
        Deadrecall.LOGGER.info("正在註冊模組創造模式頁籤...");
    }

    private static void addDeadRecallItems(FabricCreativeModeTabOutput output) {
        TotemAutomataItemGroupRegistration.addItems(output);
        TotemRemnantItemGroupRegistration.addItems(output);
        LegacyGameplayItemGroupRegistration.addItems(output);
    }
}
