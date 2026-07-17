package com.adaptor.deadrecall.gametest;

import com.adaptor.deadrecall.inventory.BackpackInventory;
import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.item.TieredBackpackItem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

public final class BackpackLegacyNestingGameTest {
    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void legacyNestedContainerLoadsAndExtractsButCannotBeReinserted(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack legacyBundle = new ItemStack(Items.BUNDLE);
        Component legacyName = Component.literal("Legacy travel bundle");
        legacyBundle.set(DataComponents.CUSTOM_NAME, legacyName);

        ItemStack backpack = new ItemStack(ModItems.BACKPACK_BASIC);
        backpack.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(legacyBundle.copy()))
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, backpack);

        BackpackInventory inventory = new BackpackInventory(
                player,
                InteractionHand.MAIN_HAND,
                TieredBackpackItem.BackpackTier.BASIC
        );

        ItemStack loaded = inventory.getItem(0);
        require(helper, loaded.is(Items.BUNDLE), "Legacy Bundle was filtered during backpack load");
        require(helper, legacyName.equals(loaded.get(DataComponents.CUSTOM_NAME)),
                "Legacy Bundle components changed during load");
        require(helper, !inventory.canPlaceItem(0, loaded),
                "Legacy Bundle was allowed to be reinserted");
        require(helper, !inventory.canPlaceItem(0, new ItemStack(Items.SHULKER_BOX)),
                "Shulker Box was allowed to be inserted");
        require(helper, !inventory.canPlaceItem(0, new ItemStack(ModItems.DEATH_BACKPACK)),
                "Death backpack was allowed to be nested");
        require(helper, inventory.canPlaceItem(0, new ItemStack(Items.DIRT)),
                "Ordinary item was rejected by backpack policy");

        ItemStack extracted = inventory.removeItemNoUpdate(0);
        require(helper, extracted.is(Items.BUNDLE), "Legacy Bundle could not be extracted");
        require(helper, legacyName.equals(extracted.get(DataComponents.CUSTOM_NAME)),
                "Extracted Bundle lost its custom name");
        require(helper, backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                        .nonEmptyItemCopyStream()
                        .findAny()
                        .isEmpty(),
                "Extracted legacy Bundle remained duplicated in backpack data");
        helper.succeed();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
