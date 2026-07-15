package com.adaptor.deadrecall.death;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathBackpackCaptureServiceTest {
    @BeforeAll
    static void bootStrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Bootstrap.validate();
    }

    @Test
    void capturesNormalItems() {
        assertTrue(DeathBackpackCaptureService.isCapturable(stack(vanillaItem("diamond"), 12)));
    }

    @Test
    void ignoresEmptyStacks() {
        assertFalse(DeathBackpackCaptureService.isCapturable(ItemStack.EMPTY));
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(Holder.direct(item, DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 64)
                .build()), count);
    }

    private static Item vanillaItem(String path) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:" + path));
        if (item == null) {
            throw new AssertionError("Missing vanilla item minecraft:" + path);
        }
        return item;
    }
}
