package com.adaptor.deadrecall.item;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class ConcretePowderItemHardeningTest {
    private static final List<String> COLORS = List.of(
            "white", "light_gray", "gray", "black",
            "brown", "red", "orange", "yellow",
            "lime", "green", "cyan", "light_blue",
            "blue", "purple", "magenta", "pink"
    );

    @BeforeAll
    static void bootStrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Bootstrap.validate();
    }

    @Test
    void mapsAllVanillaConcretePowderColors() {
        for (String color : COLORS) {
            Item powder = item(color + "_concrete_powder");
            Item concrete = item(color + "_concrete");
            assertSame(concrete, ConcretePowderItemHardening.hardenedItem(powder));
        }
    }

    @Test
    void leavesUnsupportedItemsUntouched() {
        ItemStack stone = stack(item("stone"), 5);

        assertSame(stone, ConcretePowderItemHardening.harden(stone));
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(Holder.direct(item, DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 64)
                .build()), count);
    }

    private static Item item(String path) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:" + path));
        if (item == null) {
            throw new AssertionError("Missing vanilla item minecraft:" + path);
        }
        return item;
    }
}
