package com.adaptor.deadrecall.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class DeadRecallItemGroupSupport {
    private DeadRecallItemGroupSupport() {
    }

    static void add(FabricCreativeModeTabOutput output, Item item) {
        ItemStack stack = new ItemStack(item);
        output.getDisplayStacks().add(stack);
        output.getSearchTabStacks().add(stack);
    }
}
