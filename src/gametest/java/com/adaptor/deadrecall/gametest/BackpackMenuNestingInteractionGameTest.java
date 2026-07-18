package com.adaptor.deadrecall.gametest;

import com.adaptor.deadrecall.inventory.BackpackInventory;
import com.adaptor.deadrecall.inventory.BackpackMenu;
import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.item.TieredBackpackItem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

public final class BackpackMenuNestingInteractionGameTest {
    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void shiftClickCursorDragAndNumberKeyCannotInsertPortableContainers(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack backpack = new ItemStack(ModItems.BACKPACK_BASIC);
        player.setItemInHand(InteractionHand.MAIN_HAND, backpack);

        BackpackInventory storage = new BackpackInventory(
                player,
                InteractionHand.MAIN_HAND,
                TieredBackpackItem.BackpackTier.BASIC
        );
        BackpackMenu menu = new BackpackMenu(
                MenuType.GENERIC_9x1,
                1,
                player.getInventory(),
                storage,
                1
        );
        player.containerMenu = menu;

        player.getInventory().setItem(1, new ItemStack(Items.BUNDLE));
        int bundleMenuSlot = playerMenuSlot(menu, player, 1, helper);
        require(helper, menu.quickMoveStack(player, bundleMenuSlot).isEmpty(),
                "Shift-click unexpectedly returned a moved Bundle stack");
        require(helper, player.getInventory().getItem(1).is(Items.BUNDLE),
                "Shift-click removed the Bundle from player inventory");
        require(helper, storage.isEmpty(), "Shift-click inserted Bundle into backpack storage");

        menu.setCarried(new ItemStack(Items.SHULKER_BOX));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        require(helper, menu.getCarried().is(Items.SHULKER_BOX),
                "Cursor click consumed the Shulker Box");
        require(helper, storage.isEmpty(), "Cursor click inserted Shulker Box into backpack storage");

        menu.clicked(0, 0, ContainerInput.QUICK_CRAFT, player);
        require(helper, menu.getCarried().is(Items.SHULKER_BOX),
                "Quick-craft drag consumed the Shulker Box");
        require(helper, storage.isEmpty(), "Quick-craft drag inserted Shulker Box into backpack storage");

        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 1, ContainerInput.SWAP, player);
        require(helper, player.getInventory().getItem(1).is(Items.BUNDLE),
                "Number-key swap removed the Bundle from hotbar");
        require(helper, storage.isEmpty(), "Number-key swap inserted Bundle into backpack storage");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void doubleClickDoesNotCollectLegacyPortableContainers(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack backpack = new ItemStack(ModItems.BACKPACK_BASIC);
        backpack.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.BUNDLE)))
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, backpack);

        BackpackInventory storage = new BackpackInventory(
                player,
                InteractionHand.MAIN_HAND,
                TieredBackpackItem.BackpackTier.BASIC
        );
        BackpackMenu menu = new BackpackMenu(
                MenuType.GENERIC_9x1,
                2,
                player.getInventory(),
                storage,
                1
        );
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.BUNDLE));

        menu.clicked(0, 0, ContainerInput.PICKUP_ALL, player);

        require(helper, menu.getCarried().is(Items.BUNDLE) && menu.getCarried().getCount() == 1,
                "Double-click changed the carried Bundle");
        require(helper, storage.getItem(0).is(Items.BUNDLE),
                "Double-click removed the legacy Bundle from storage");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void sameTickMultiplayerInsertionRacePreservesBothPortableContainers(GameTestHelper helper) {
        ServerPlayer first = helper.makeMockServerPlayerInLevel();
        ServerPlayer second = helper.makeMockServerPlayerInLevel();
        try {
            BackpackInventory firstStorage = openBackpack(first, 3);
            BackpackInventory secondStorage = openBackpack(second, 4);
            first.getInventory().setItem(1, new ItemStack(Items.BUNDLE));
            second.getInventory().setItem(2, new ItemStack(Items.SHULKER_BOX));

            first.containerMenu.clicked(0, 1, ContainerInput.SWAP, first);
            second.containerMenu.clicked(0, 2, ContainerInput.SWAP, second);

            require(helper, first.getInventory().getItem(1).is(Items.BUNDLE),
                    "First player's same-tick insertion lost the Bundle");
            require(helper, second.getInventory().getItem(2).is(Items.SHULKER_BOX),
                    "Second player's same-tick insertion lost the Shulker Box");
            require(helper, firstStorage.isEmpty() && secondStorage.isEmpty(),
                    "Same-tick multiplayer operations inserted a portable container into a backpack");
            helper.succeed();
        } finally {
            first.discard();
            second.discard();
        }
    }

    private static BackpackInventory openBackpack(ServerPlayer player, int containerId) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BACKPACK_BASIC));
        BackpackInventory storage = new BackpackInventory(
                player,
                InteractionHand.MAIN_HAND,
                TieredBackpackItem.BackpackTier.BASIC
        );
        player.containerMenu = new BackpackMenu(
                MenuType.GENERIC_9x1,
                containerId,
                player.getInventory(),
                storage,
                1
        );
        return storage;
    }

    private static int playerMenuSlot(
            BackpackMenu menu,
            ServerPlayer player,
            int inventorySlot,
            GameTestHelper helper
    ) {
        for (int index = 0; index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == inventorySlot) {
                return index;
            }
        }
        throw helper.assertionException("Missing player inventory slot " + inventorySlot + " in backpack menu");
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
