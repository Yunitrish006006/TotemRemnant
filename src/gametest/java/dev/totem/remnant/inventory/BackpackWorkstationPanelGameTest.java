package dev.totem.remnant.inventory;

import dev.totem.remnant.network.BackpackPanelPayloadRegistration;
import dev.totem.remnant.network.BackpackSortPayload;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;

public final class BackpackWorkstationPanelGameTest {
    @GameTest(maxTicks = 20)
    public void adapterRegistryRejectsSubclassesAndPreservesAllChestBoundaries(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            require(BackpackSidebarAdapters.variants().equals(java.util.Set.of("inventory", "crafting", "enchanting", "chest")),
                    "Registered variants drifted from the native Observer contract");
            require(BackpackPanelMenuAccess.supports(player.inventoryMenu), "Inventory adapter was not attached");
            for (int rows = 1; rows <= 6; rows++) {
                var menu = new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, 20 + rows,
                        player.getInventory(), new net.minecraft.world.SimpleContainer(rows * 9), rows);
                var access = (BackpackPanelMenuAccess) menu;
                int nativeCount = rows * 9 + 36;
                require(BackpackSidebarAdapters.find(menu) == BackpackSidebarAdapters.CHEST
                        && access.totem$getBackpackPanelSlotStart() == nativeCount
                        && menu.slots.size() == nativeCount + 256, "Chest constructor attached at the wrong boundary");
                var original = menu.getSlot(nativeCount);
                require(BackpackPanelAttachment.attach(menu) == null && menu.slots.size() == nativeCount + 256
                        && menu.getSlot(nativeCount) == original, "Repeated attachment changed physical slots");
            }
            var custom = new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, 30,
                    player.getInventory(), new net.minecraft.world.SimpleContainer(9), 1) {};
            require(BackpackSidebarAdapters.find(custom) == null && !BackpackPanelMenuAccess.supports(custom)
                    && ((BackpackPanelMenuAccess) custom).totem$getBackpackPanel() == null
                    && custom.slots.size() == 45, "Custom menu inherited an unregistered adapter");
            helper.succeed();
        } finally { player.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void capturedBackpackSortPreservesNestedContentsAndRejectsNewNesting(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack inner = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            inner.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.EMERALD, 11))));
            for (boolean sidebar : List.of(false, true)) {
                ItemStack root = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
                root.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                        new ItemStack(Items.DIRT, 40), inner.copy(), new ItemStack(Items.DIRT, 32))));
                player.setItemInHand(InteractionHand.MAIN_HAND, root);
                AbstractContainerMenu menu;
                int start;
                if (sidebar) {
                    menu = ChestMenu.threeRows(12, player.getInventory());
                    var access = (BackpackPanelMenuAccess) menu;
                    access.totem$selectBackpackSlot(player.getInventory().getSelectedSlot());
                    start = access.totem$getBackpackPanelSlotStart();
                } else {
                    menu = BackpackMenu.deathServerSide(11, player.getInventory(),
                            new DeathBackpackInventory(player, InteractionHand.MAIN_HAND, 9), 1);
                    start = 0;
                }
                player.containerMenu = menu;
                require(BackpackActions.sort(player, false, sidebar), "Captured backpack blocked death sort");
                var sorted = contents(root);
                require(sorted.size() == 3 && sorted.stream().filter(i -> i.is(Items.DIRT)).mapToInt(ItemStack::getCount).sum() == 72
                        && sorted.stream().anyMatch(i -> ItemStack.matches(i, inner)), "Sort changed captured stack or its contents");
                var persisted = root.get(DataComponents.CONTAINER);
                require(BackpackActions.sort(player, false, sidebar) && persisted.equals(root.get(DataComponents.CONTAINER)),
                        "Repeated death sort changed contents");
                require(!menu.getSlot(start + 8).container.canPlaceItem(menu.getSlot(start + 8).getContainerSlot(), inner),
                        "Sort relaxed normal nesting policy");
                menu.setCarried(inner.copy());
                menu.clicked(start + 8, 0, ContainerInput.PICKUP, player);
                require(ItemStack.matches(menu.getCarried(), inner) && menu.getSlot(start + 8).getItem().isEmpty(),
                        "Manual insertion admitted a new nested backpack");
                menu.setCarried(ItemStack.EMPTY);
                menu.removed(player);
                require(player.getMainHandItem() == root && persisted.equals(root.get(DataComponents.CONTAINER)),
                        "Closing nonempty captured backpack lost contents");
            }
            helper.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void nativeFunctionalSlotsAndPanelTransfersKeepTheirIndices(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.LAPIS_LAZULI, 7))));
            player.getInventory().setItem(0, bag);
            List<AbstractContainerMenu> menus = List.of(new CraftingMenu(1, player.getInventory()),
                    new EnchantmentMenu(2, player.getInventory()), ChestMenu.threeRows(3, player.getInventory()));
            int[] expected = {46, 38, 63};
            for (int i = 0; i < menus.size(); i++) {
                AbstractContainerMenu menu = menus.get(i);
                player.containerMenu = menu;
                BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) menu;
                require(access.totem$getBackpackPanelSlotStart() == expected[i], "Native slot indices changed or duplicate panel appended");
                require(menu.slots.size() == expected[i] + 256, "Panel does not expose all bounded death storage");
                access.totem$selectBackpackSlot(0);
                int panel = access.totem$getBackpackPanelSlotStart();
                menu.clicked(panel, 0, ContainerInput.PICKUP, player);
                require(menu.getCarried().getCount() == 7 && contents(bag).isEmpty(), "Panel withdrawal lost items");
                int functional = menu instanceof EnchantmentMenu ? 1 : menu instanceof CraftingMenu ? 1 : 0;
                menu.clicked(functional, 0, ContainerInput.PICKUP, player);
                require(menu.getCarried().isEmpty() && menu.getSlot(functional).getItem().getCount() == 7,
                        "Manual material placement did not use the native functional slot");
                menu.clicked(functional, 0, ContainerInput.PICKUP, player);
                menu.clicked(panel, 0, ContainerInput.PICKUP, player);
                require(menu.getCarried().isEmpty() && contents(bag).getFirst().getCount() == 7, "Returning material lost items");
                require(menu.quickMoveStack(player, panel).getCount() == 7 && contents(bag).isEmpty(), "Panel shift-click failed");
                bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.LAPIS_LAZULI, 7))));
                access.totem$getBackpackPanel().refreshAfterExternalCommit();
            }
            helper.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void deathPanelDeepSlotsSortAndCloseWithoutEatingHiddenContents(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack bag = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
            var stored = new ArrayList<ItemStack>();
            for (int i = 0; i < 256; i++) stored.add(ItemStack.EMPTY);
            stored.set(200, new ItemStack(Items.DIAMOND, 3));
            stored.set(255, new ItemStack(Items.DIAMOND, 5));
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stored));
            player.getInventory().setItem(0, bag);
            ChestMenu menu = ChestMenu.threeRows(4, player.getInventory());
            player.containerMenu = menu;
            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) menu;
            access.totem$selectBackpackSlot(0);
            int start = access.totem$getBackpackPanelSlotStart();
            var physical = menu.getSlot(start + 255);
            access.totem$layoutBackpackWindow(180, 18, 9, 26, 3);
            require(menu.getSlot(start + 255) == physical, "Scrolling remapped a physical slot");
            require(BackpackPanelPayloadRegistration.handleSort(player, new BackpackSortPayload(4, menu.getStateId(),
                    start + 255, 0, bag.copy())), "Death panel middle-click request was rejected");
            require(contents(bag).getFirst().getCount() == 8 && menu.getSlot(0).getItem().isEmpty(),
                    "Panel sort was routed to chest or lost deep items");
            menu.removed(player);
            require(player.getInventory().getItem(0) == bag, "Closing a nonempty death bag deleted it");
            menu.clicked(start, 0, ContainerInput.PICKUP, player);
            require(menu.getCarried().getCount() == 8 && contents(bag).isEmpty(), "Sorted death contents inaccessible");
            menu.clicked(0, 0, ContainerInput.PICKUP, player);
            require(menu.getSlot(0).getItem().getCount() == 8, "Death item transfer lost items");
            menu.removed(player);
            require(player.getInventory().getItem(0).isEmpty(), "Empty death bag recovery did not complete at close");
            helper.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void chestSortingCannotDrainSelectedBackpackPanel(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 9))));
            player.getInventory().setItem(0, bag);
            ChestMenu menu = ChestMenu.threeRows(5, player.getInventory());
            player.containerMenu = menu;
            ((BackpackPanelMenuAccess) menu).totem$selectBackpackSlot(0);
            menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 4));
            require(BackpackActions.sort(player, false), "Chest sort failed with adjacent panel");
            require(menu.getSlot(0).getItem().getCount() == 4 && contents(bag).getFirst().getCount() == 9,
                    "Chest sorting drained or counted sidebar contents");
            require(menu.quickMoveStack(player, 0).getCount() == 4, "Native chest shift-click failed");
            require(contents(bag).getFirst().getCount() == 9 && menu.getSlot(0).getItem().isEmpty(),
                    "Native chest shift-click inserted into sidebar instead of player inventory");
            helper.succeed();
        } finally { player.containerMenu = player.inventoryMenu; player.discard(); }
    }

    private static List<ItemStack> contents(ItemStack bag) {
        return bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList();
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
