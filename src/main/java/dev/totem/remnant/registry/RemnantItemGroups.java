package dev.totem.remnant.registry;

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

/** Exposes every Remnant backpack in the preserved Creative-mode tab. */
public final class RemnantItemGroups {
    private static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath("deadrecall", "main")
    );

    private static boolean registered;

    private RemnantItemGroups() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        registerStandaloneTab();
        CreativeModeTabEvents.modifyOutputEvent(TAB_KEY).register(RemnantItemGroups::addItems);
        registered = true;
    }

    private static void registerStandaloneTab() {
        if (BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(TAB_KEY).isPresent()) {
            return;
        }

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY,
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.deadrecall.main"))
                        .icon(() -> new ItemStack(RemnantItemRegistration.BACKPACK_BASIC))
                        .build());
    }

    private static void addItems(FabricCreativeModeTabOutput output) {
        output.accept(RemnantItemRegistration.BACKPACK_BASIC);
        output.accept(RemnantItemRegistration.BACKPACK_STANDARD);
        output.accept(RemnantItemRegistration.BACKPACK_ADVANCED);
        output.accept(RemnantItemRegistration.BACKPACK_NETHERITE);
        output.accept(RemnantItemRegistration.UPGRADE_CRAFTING);
        output.accept(RemnantItemRegistration.UPGRADE_COMPACTION);
        output.accept(RemnantItemRegistration.UPGRADE_MATCHING_PICKUP);
        output.accept(RemnantItemRegistration.UPGRADE_CAPACITY);
        output.accept(RemnantItemRegistration.UPGRADE_SOULBOUND_CHARGE);
        output.accept(RemnantItemRegistration.UPGRADE_ENDER_ACCESS);
        output.accept(RemnantItemRegistration.UPGRADE_BLAST_PROTECTION);
        output.accept(RemnantItemRegistration.UPGRADE_FIRE_PROTECTION);
        output.accept(RemnantItemRegistration.UPGRADE_DESPAWN_PROTECTION);
        output.accept(RemnantItemRegistration.UPGRADE_VOID_PROTECTION);
        output.accept(RemnantItemRegistration.UPGRADE_PERFECT_PRESERVATION);
        output.accept(RemnantItemRegistration.DEATH_BACKPACK);
    }
}
