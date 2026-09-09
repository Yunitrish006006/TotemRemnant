package dev.totem.remnant.inventory;

import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
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
    public void vanillaInventoryClicksRemainConservativeWithPanel(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = backpackWith(new ItemStack(Items.OAK_LOG, 32));
            player.getInventory().setItem(0, backpack);
            player.getInventory().setItem(1, new ItemStack(Items.OAK_LOG, 32));

            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) player.inventoryMenu;
            access.totem$selectBackpackSlot(0);
            int panelStart = access.totem$getBackpackPanelSlotStart();
            var originalPanelSlot = player.inventoryMenu.getSlot(panelStart);
            access.totem$layoutBackpackSlots(180, 18, 9);
            access.totem$layoutBackpackSlots(198, 36, 9);
            if (player.inventoryMenu.getSlot(panelStart) != originalPanelSlot) {
                helper.fail("Backpack panel layout replaced a registered Slot instance");
                return;
            }

            int oakMenuSlot = findPlayerInventoryMenuSlot(player, 1);
            player.inventoryMenu.clicked(oakMenuSlot, 0, ContainerInput.PICKUP, player);
            if (!player.inventoryMenu.getCarried().is(Items.OAK_LOG)
                    || player.inventoryMenu.getCarried().getCount() != 32
                    || !player.getInventory().getItem(1).isEmpty()
                    || total(player, Items.OAK_LOG) != 32) {
                helper.fail("Vanilla left-click duplicated or lost a player-inventory stack");
                return;
            }

            player.inventoryMenu.clicked(oakMenuSlot, 0, ContainerInput.PICKUP_ALL, player);
            if (!player.inventoryMenu.getCarried().is(Items.OAK_LOG)
                    || player.inventoryMenu.getCarried().getCount() != 32
                    || !player.getInventory().getItem(1).isEmpty()
                    || !panelItem(backpack, 0).is(Items.OAK_LOG)
                    || panelItem(backpack, 0).getCount() != 32) {
                helper.fail("Vanilla double-click drained the adjacent backpack panel");
                return;
            }

            player.inventoryMenu.clicked(oakMenuSlot, 0, ContainerInput.PICKUP, player);
            if (!player.inventoryMenu.getCarried().isEmpty()
                    || player.getInventory().getItem(1).getCount() != 32
                    || total(player, Items.OAK_LOG) != 32
                    || panelItem(backpack, 0).getCount() != 32) {
                helper.fail("Returning a vanilla carried stack changed its total count");
                return;
            }

            player.inventoryMenu.clicked(oakMenuSlot, 1, ContainerInput.PICKUP, player);
            if (player.inventoryMenu.getCarried().getCount() != 16
                    || player.getInventory().getItem(1).getCount() != 16
                    || total(player, Items.OAK_LOG) != 32) {
                helper.fail("Vanilla right-click split duplicated or lost items");
                return;
            }

            player.inventoryMenu.clicked(oakMenuSlot, 0, ContainerInput.PICKUP, player);
            if (!player.inventoryMenu.getCarried().isEmpty()
                    || player.getInventory().getItem(1).getCount() != 32
                    || total(player, Items.OAK_LOG) != 32) {
                helper.fail("Merging a split stack back changed its total count");
                return;
            }

            ItemStack quickMoved = player.inventoryMenu.quickMoveStack(player, oakMenuSlot);
            if (!quickMoved.is(Items.OAK_LOG)
                    || total(player, Items.OAK_LOG) != 32
                    || !panelItem(backpack, 0).is(Items.OAK_LOG)
                    || panelItem(backpack, 0).getCount() != 32) {
                helper.fail("Vanilla Shift-click was hijacked by the backpack side panel");
                return;
            }

            // The inventory's vanilla 2x2 crafting slots are menu indices 1..4.
            // Fill them using the same right-click path a player uses in the E screen.
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.getInventory().setItem(2, new ItemStack(Items.OAK_PLANKS, 4));
            int plankMenuSlot = findPlayerInventoryMenuSlot(player, 2);
            player.inventoryMenu.clicked(plankMenuSlot, 0, ContainerInput.PICKUP, player);
            for (int craftSlot = 1; craftSlot <= 4; craftSlot++) {
                player.inventoryMenu.clicked(craftSlot, 1, ContainerInput.PICKUP, player);
            }
            if (!player.inventoryMenu.getCarried().isEmpty()
                    || !player.inventoryMenu.getSlot(0).getItem().is(Items.CRAFTING_TABLE)) {
                helper.fail("Vanilla 2x2 crafting inputs did not resolve exactly one crafting table");
                return;
            }
            for (int craftSlot = 1; craftSlot <= 4; craftSlot++) {
                if (!player.inventoryMenu.getSlot(craftSlot).getItem().is(Items.OAK_PLANKS)
                        || player.inventoryMenu.getSlot(craftSlot).getItem().getCount() != 1) {
                    helper.fail("Vanilla 2x2 right-click insertion changed an input count");
                    return;
                }
            }

            player.inventoryMenu.clicked(0, 0, ContainerInput.PICKUP, player);
            if (!player.inventoryMenu.getCarried().is(Items.CRAFTING_TABLE)
                    || player.inventoryMenu.getCarried().getCount() != 1
                    || total(player, Items.CRAFTING_TABLE) != 1) {
                helper.fail("Taking the vanilla 2x2 crafting result duplicated the output");
                return;
            }
            for (int craftSlot = 1; craftSlot <= 4; craftSlot++) {
                if (player.inventoryMenu.getSlot(craftSlot).hasItem()) {
                    helper.fail("Taking the vanilla 2x2 result did not consume each input exactly once");
                    return;
                }
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
            // THROW creates a real item entity; keep it inside this test's fixture
            // instead of the shared default mock-player position used by other tests.
            BlockPos position = helper.absolutePos(new BlockPos(1, 2, 1));
            player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
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

            // The first physical click empties its origin; vanilla PICKUP_ALL
            // intentionally does nothing when the clicked slot still holds an item.
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.inventoryMenu.clicked(panelStart + 3, 0, ContainerInput.PICKUP, player);
            player.inventoryMenu.clicked(panelStart + 3, 0, ContainerInput.PICKUP_ALL, player);
            if (player.inventoryMenu.getCarried().getCount() != 8
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

    @GameTest(maxTicks = 20)
    public void doubleClickCollectionIsSymmetricAndDoesNotRememberPreviousSurface(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = backpackWithItems(new ItemStack(Items.REDSTONE, 5), new ItemStack(Items.REDSTONE, 7));
            player.getInventory().setItem(0, backpack);
            player.getInventory().setItem(2, new ItemStack(Items.REDSTONE, 11));
            player.getInventory().setItem(3, new ItemStack(Items.REDSTONE, 13));
            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) player.inventoryMenu;
            access.totem$selectBackpackSlot(0);
            int panel = access.totem$getBackpackPanelSlotStart();
            int inventory = findPlayerInventoryMenuSlot(player, 2);

            doubleClick(player, panel);
            if (player.inventoryMenu.getCarried().getCount() != 12
                    || !panelItem(backpack, 0).isEmpty() || !panelItem(backpack, 1).isEmpty()
                    || player.getInventory().getItem(2).getCount() != 11
                    || player.getInventory().getItem(3).getCount() != 13) {
                helper.fail("Panel double-click crossed into player inventory or failed to collect its own surface");
                return;
            }
            player.inventoryMenu.clicked(panel, 0, ContainerInput.PICKUP, player);

            doubleClick(player, inventory);
            if (player.inventoryMenu.getCarried().getCount() != 24
                    || !player.getInventory().getItem(2).isEmpty()
                    || !player.getInventory().getItem(3).isEmpty()
                    || panelItem(backpack, 0).getCount() != 12) {
                helper.fail("Inventory double-click inherited the preceding panel origin");
                return;
            }
            player.inventoryMenu.clicked(inventory, 0, ContainerInput.PICKUP, player);

            doubleClick(player, panel);
            if (player.inventoryMenu.getCarried().getCount() != 12
                    || player.getInventory().getItem(2).getCount() != 24
                    || !panelItem(backpack, 0).isEmpty()) {
                helper.fail("Panel double-click inherited the preceding inventory origin");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(maxTicks = 20)
    public void doubleClickKeepsVanillaMaximumAndConservesExcess(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = backpackWithItems(new ItemStack(Items.REDSTONE, 40), new ItemStack(Items.REDSTONE, 40));
            player.getInventory().setItem(0, backpack);
            player.getInventory().setItem(2, new ItemStack(Items.REDSTONE, 16));
            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) player.inventoryMenu;
            access.totem$selectBackpackSlot(0);
            doubleClick(player, access.totem$getBackpackPanelSlotStart());
            if (player.inventoryMenu.getCarried().getCount() != 64
                    || !panelItem(backpack, 0).isEmpty()
                    || panelItem(backpack, 1).getCount() != 16
                    || player.getInventory().getItem(2).getCount() != 16) {
                helper.fail("Panel double-click exceeded the native maximum or lost the uncollected remainder");
                return;
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(maxTicks = 20)
    public void doubleClickInactiveOrOutsideOriginCannotCollect(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack backpack = backpackWith(new ItemStack(Items.REDSTONE, 5));
            player.getInventory().setItem(0, backpack);
            player.getInventory().setItem(2, new ItemStack(Items.REDSTONE, 7));
            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) player.inventoryMenu;
            access.totem$selectBackpackSlot(0);
            int inactive = access.totem$getBackpackPanelSlotStart() + 9;
            if (player.inventoryMenu.getSlot(inactive).isActive()) {
                helper.fail("Expected a slot outside the basic backpack's nine-slot capacity");
                return;
            }
            player.inventoryMenu.setCarried(new ItemStack(Items.REDSTONE));
            for (int origin : new int[]{inactive, -999, -1}) {
                player.inventoryMenu.clicked(origin, 0, ContainerInput.PICKUP_ALL, player);
                if (player.inventoryMenu.getCarried().getCount() != 1
                        || panelItem(backpack, 0).getCount() != 5
                        || player.getInventory().getItem(2).getCount() != 7) {
                    helper.fail("Inactive/outside double-click origin collected items: " + origin);
                    return;
                }
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    private static void doubleClick(ServerPlayer player, int slot) {
        player.inventoryMenu.clicked(slot, 0, ContainerInput.PICKUP, player);
        player.inventoryMenu.clicked(slot, 0, ContainerInput.PICKUP_ALL, player);
    }

    private static int findPlayerInventoryMenuSlot(ServerPlayer player, int inventorySlot) {
        for (int menuSlot = 0; menuSlot < player.inventoryMenu.slots.size(); menuSlot++) {
            var slot = player.inventoryMenu.getSlot(menuSlot);
            if (slot.container == player.getInventory()
                    && slot.getContainerSlot() == inventorySlot) {
                return menuSlot;
            }
        }
        throw new IllegalStateException("Could not find player inventory slot " + inventorySlot);
    }

    private static int total(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        ItemStack carried = player.inventoryMenu.getCarried();
        if (carried.is(item)) total += carried.getCount();
        return total;
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
