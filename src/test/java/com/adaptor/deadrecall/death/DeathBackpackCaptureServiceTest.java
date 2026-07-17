package com.adaptor.deadrecall.death;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void excludesBundleAndShulkerBoxFromDeathBackpackNesting() {
        assertFalse(DeathBackpackCaptureService.isCapturable(stack(vanillaItem("bundle"), 1)));
        assertFalse(DeathBackpackCaptureService.isCapturable(stack(vanillaItem("shulker_box"), 1)));
        assertFalse(DeathBackpackCaptureService.isCapturable(stack(vanillaItem("blue_shulker_box"), 1)));
    }

    @Test
    void classificationDoesNotRewriteNamedPortableContainerComponents() {
        Component name = Component.literal("Legacy named shulker");
        ItemStack shulker = stack(vanillaItem("red_shulker_box"), 1, name);

        assertFalse(DeathBackpackCaptureService.isCapturable(shulker));
        assertEquals(1, shulker.getCount());
        assertEquals(name, shulker.get(DataComponents.CUSTOM_NAME));
    }

    private static ItemStack stack(Item item, int count) {
        return stack(item, count, null);
    }

    private static ItemStack stack(Item item, int count, Component customName) {
        DataComponentMap.Builder components = DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 64);
        if (customName != null) {
            components.set(DataComponents.CUSTOM_NAME, customName);
        }
        return new ItemStack(Holder.direct(item, components.build()), count);
    }

    private static Item vanillaItem(String path) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:" + path));
        if (item == null) {
            throw new AssertionError("Missing vanilla item minecraft:" + path);
        }
        return item;
    }
}
