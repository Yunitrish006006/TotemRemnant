package dev.totem.remnant.inventory;

import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

/** Exercises the same real InventoryMenu slots addressed by vanilla click packets. */
public final class BackpackPanelInteractionGameTest {
    @GameTest(maxTicks = 20)
    public void panelSlotsPickupPlaceQuickMoveAndRejectStaleSelection(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack firstBackpack = backpackWith(new ItemStack(Items.DIAMOND, 3));
            ItemStack secondBackpack = backpackWith(new ItemStack(Items.EMERALD, 2));
            player.getInventory().setItem(0, firstBackpack);
            player.getInventory().setItem(1, secondBackpack);

            if (!(player.inventoryMenu instanceof BackpackPanelMenuAccess access)) {
                helper.fail("InventoryMenu did not expose Remnant backpack panel slots");
                return;
            }
            access.totem$selectBackpackSlot(0);
            int panelStart = access.totem$getBackpackPanelSlotStart();

            player.inventoryMenu.clicked(panelStart, 0, ContainerInput.PICKUP, player);
            if (!player.inventoryMenu.getCarried().is(Items.DIAMOND)
                    || player.inventoryMenu.getCarried().getCount() != 3
                    || !panelItem(firstBackpack, 0).isEmpty()) {
                helper.fail("Left-click did not move the real backpack stack to the carried slot");
                return;
            }

            player.inventoryMenu.clicked(panelStart + 1, 0, ContainerInput.PICKUP, player);
            if (!player.inventoryMenu.getCarried().isEmpty()
                    || !panelItem(firstBackpack, 1).is(Items.DIAMOND)) {
                helper.fail("Left-click did not place the carried stack into the backpack component");
                return;
            }

            ItemStack quickMoved = player.inventoryMenu.quickMoveStack(player, panelStart + 1);
            if (!quickMoved.is(Items.DIAMOND)
                    || !panelItem(firstBackpack, 1).isEmpty()
                    || !player.getInventory().contains(new ItemStack(Items.DIAMOND))) {
                helper.fail("Shift-click did not move the backpack stack into player inventory");
                return;
            }

            access.totem$selectBackpackSlot(1);
            player.inventoryMenu.clicked(panelStart, 0, ContainerInput.PICKUP, player);
            if (!player.inventoryMenu.getCarried().is(Items.EMERALD)
                    || !panelItem(secondBackpack, 0).isEmpty()
                    || !panelItem(firstBackpack, 0).isEmpty()) {
                helper.fail("Selecting another backpack mutated the wrong container component");
                return;
            }

            player.inventoryMenu.setCarried(new ItemStack(Items.STONE));
            player.getInventory().setItem(1, ItemStack.EMPTY);
            player.inventoryMenu.clicked(panelStart, 0, ContainerInput.PICKUP, player);
            if (!player.inventoryMenu.getCarried().is(Items.STONE)) {
                helper.fail("A stale selected inventory slot accepted a panel mutation");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(maxTicks = 20)
    public void panelRejectsPortableContainerNesting(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = backpackWith(ItemStack.EMPTY);
            player.getInventory().setItem(0, backpack);
            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) player.inventoryMenu;
            access.totem$selectBackpackSlot(0);
            player.inventoryMenu.setCarried(new ItemStack(Items.SHULKER_BOX));
            player.inventoryMenu.clicked(
                    access.totem$getBackpackPanelSlotStart(),
                    0,
                    ContainerInput.PICKUP,
                    player
            );
            if (!player.inventoryMenu.getCarried().is(Items.SHULKER_BOX)
                    || !panelItem(backpack, 0).isEmpty()) {
                helper.fail("Interactive panel bypassed portable-container nesting protection");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(maxTicks = 20)
    public void panelSupportsNativeSplitSwapThrowDragAndDoubleClick(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = backpackWithItems(
                    new ItemStack(Items.IRON_INGOT, 5),
                    new ItemStack(Items.IRON_INGOT, 4),
                    new ItemStack(Items.COBBLESTONE, 64),
                    new ItemStack(Items.REDSTONE, 2),
                    new ItemStack(Items.REDSTONE, 2)
            );
            player.getInventory().setItem(0, backpack);
            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) player.inventoryMenu;
            access.totem$selectBackpackSlot(0);
            int panelStart = access.totem$getBackpackPanelSlotStart();

            player.inventoryMenu.clicked(panelStart, 1, ContainerInput.PICKUP, player);
            if (player.inventoryMenu.getCarried().getCount() != 3
                    || panelItem(backpack, 0).getCount() != 2) {
                helper.fail("Right-click did not split a backpack panel stack");
                return;
            }
            player.inventoryMenu.clicked(panelStart + 5, 1, ContainerInput.PICKUP, player);
            if (player.inventoryMenu.getCarried().getCount() != 2
                    || panelItem(backpack, 5).getCount() != 1) {
                helper.fail("Right-click did not place one carried item into the panel");
                return;
            }

            player.inventoryMenu.setCarried(new ItemStack(Items.COBBLESTONE));
            player.inventoryMenu.clicked(panelStart + 2, 0, ContainerInput.PICKUP, player);
            if (player.inventoryMenu.getCarried().getCount() != 1
                    || panelItem(backpack, 2).getCount() != 64) {
                helper.fail("A full panel stack accepted an extra carried item");
                return;
            }

            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.getInventory().setItem(2, new ItemStack(Items.GOLD_INGOT, 7));
            player.inventoryMenu.clicked(panelStart + 1, 2, ContainerInput.SWAP, player);
            if (!panelItem(backpack, 1).is(Items.GOLD_INGOT)
                    || panelItem(backpack, 1).getCount() != 7
                    || !player.getInventory().getItem(2).is(Items.IRON_INGOT)
                    || player.getInventory().getItem(2).getCount() != 4) {
                helper.fail("Number-key swap did not exchange a panel and hotbar stack");
                return;
            }

            player.inventoryMenu.clicked(panelStart + 1, 0, ContainerInput.THROW, player);
            if (panelItem(backpack, 1).getCount() != 6) {
                helper.fail("Throw input did not remove one item from the panel stack");
                return;
            }

            player.inventoryMenu.setCarried(new ItemStack(Items.REDSTONE, 4));
            player.inventoryMenu.clicked(-999,
                    net.minecraft.world.inventory.AbstractContainerMenu.getQuickcraftMask(0, 0),
                    ContainerInput.QUICK_CRAFT, player);
            player.inventoryMenu.clicked(panelStart + 6,
                    net.minecraft.world.inventory.AbstractContainerMenu.getQuickcraftMask(1, 0),
                    ContainerInput.QUICK_CRAFT, player);
            player.inventoryMenu.clicked(panelStart + 7,
                    net.minecraft.world.inventory.AbstractContainerMenu.getQuickcraftMask(1, 0),
                    ContainerInput.QUICK_CRAFT, player);
            player.inventoryMenu.clicked(-999,
                    net.minecraft.world.inventory.AbstractContainerMenu.getQuickcraftMask(2, 0),
                    ContainerInput.QUICK_CRAFT, player);
            if (!player.inventoryMenu.getCarried().isEmpty()
                    || panelItem(backpack, 6).getCount() != 2
                    || panelItem(backpack, 7).getCount() != 2) {
                helper.fail("Vanilla quick-craft drag did not distribute items across panel slots");
                return;
            }

            player.inventoryMenu.setCarried(new ItemStack(Items.REDSTONE));
            player.inventoryMenu.clicked(panelStart + 3, 0, ContainerInput.PICKUP_ALL, player);
            if (player.inventoryMenu.getCarried().getCount() != 9
                    || !panelItem(backpack, 3).isEmpty()
                    || !panelItem(backpack, 4).isEmpty()
                    || !panelItem(backpack, 6).isEmpty()
                    || !panelItem(backpack, 7).isEmpty()) {
                helper.fail("Double-click pickup-all did not collect matching panel stacks");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    private static ItemStack backpackWith(ItemStack firstItem) {
        ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        if (!firstItem.isEmpty()) {
            backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(firstItem)));
        }
        return backpack;
    }

    private static ItemStack backpackWithItems(ItemStack... items) {
        ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        NonNullList<ItemStack> contents = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int slot = 0; slot < Math.min(items.length, contents.size()); slot++) {
            contents.set(slot, items[slot]);
        }
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return backpack;
    }

    private static ItemStack panelItem(ItemStack backpack, int slot) {
        NonNullList<ItemStack> contents = NonNullList.withSize(9, ItemStack.EMPTY);
        backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(contents);
        return contents.get(slot);
    }
}
