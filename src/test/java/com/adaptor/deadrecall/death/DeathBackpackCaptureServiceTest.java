package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.item.DeathBackpackItem;
import com.adaptor.deadrecall.item.TieredBackpackItem;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathBackpackCaptureServiceTest {
    private static final Item TEST_DEATH_BACKPACK = new DeathBackpackItem(
            properties("test_death_backpack").stacksTo(1)
    );
    private static final Item TEST_TIERED_BACKPACK = new TieredBackpackItem(
            properties("test_tiered_backpack").stacksTo(1),
            TieredBackpackItem.BackpackTier.BASIC
    );

    @BeforeAll
    static void bootStrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Bootstrap.validate();
    }

    @Test
    void capturesNormalItems() {
        assertTrue(DeathBackpackCaptureService.isCapturable(stack(vanillaItem("diamond"), 12, 64)));
    }

    @Test
    void ignoresEmptyStacks() {
        assertFalse(DeathBackpackCaptureService.isCapturable(ItemStack.EMPTY));
    }

    @Test
    void excludesDeathBackpacksFromNesting() {
        assertFalse(DeathBackpackCaptureService.isCapturable(stack(TEST_DEATH_BACKPACK, 1, 1)));
    }

    @Test
    void excludesTieredBackpacksFromNesting() {
        assertFalse(DeathBackpackCaptureService.isCapturable(stack(TEST_TIERED_BACKPACK, 1, 1)));
    }

    private static Item.Properties properties(String path) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath("deadrecall_test", path)
        );
        return new Item.Properties().setId(key);
    }

    private static ItemStack stack(Item item, int count, int maxStackSize) {
        return new ItemStack(Holder.direct(item, DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, maxStackSize)
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
