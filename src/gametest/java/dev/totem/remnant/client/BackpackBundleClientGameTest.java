package dev.totem.remnant.client;

import dev.totem.core.api.v1.client.observer.*;
import dev.totem.remnant.client.screen.RemnantBackpackObserverScreenProvider;
import dev.totem.remnant.inventory.BackpackItemInteraction;
import dev.totem.remnant.item.BackpackTooltip;
import dev.totem.remnant.registry.RemnantDataComponents;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BackpackBundleClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        String expected = System.getProperty("totem.remnant.test.expectedVanillaTweaks");
        if (expected != null) require((expected.equals("true") || expected.equals("false"))
                        && net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("totem-vanilla-tweaks")
                        == Boolean.parseBoolean(expected), "Unexpected VanillaTweaks presence");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender(); context.getInput().resizeWindow(1280, 720);
            if (Boolean.getBoolean("totem.remnant.test.hoverOnly")) {
                verifyHoveredContainers(context, world); return;
            }
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); p.getInventory().clearContent();
                var bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
                bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                        new ItemStack(Items.DIRT, 2), new ItemStack(Items.APPLE, 3), new ItemStack(Items.DIRT, 3))));
                p.getInventory().setItem(0, bag); p.inventoryMenu.broadcastFullState();
            });
            context.waitFor(client -> client.player.getInventory().getItem(0).is(RemnantItemRegistration.BACKPACK_BASIC));
            context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
            move(context, 36); context.getInput().pressMouse(2);
            context.waitFor(client -> client.player.getInventory().getItem(0).get(DataComponents.CONTAINER)
                    .itemCopies().findFirst().orElse(ItemStack.EMPTY).is(Items.APPLE));
            world.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst().getInventory().getItem(0)
                    .get(DataComponents.CONTAINER).itemCopies().filter(item -> item.is(Items.DIRT)).count() == 1);
            context.takeScreenshot("backpack-hover-middle-click-sorted");
            context.getInput().scroll(0, 1);
            context.waitFor(client -> client.player.getInventory().getItem(0).has(RemnantDataComponents.SELECTED_ITEM));
            int first = context.computeOnClient(client -> client.player.getInventory().getItem(0).get(RemnantDataComponents.SELECTED_ITEM));
            context.runOnClient(client -> verifyPreview(client.player.getInventory().getItem(0)));
            context.takeScreenshot("backpack-bundle-wheel-first-selection");
            context.getInput().scroll(0, 1);
            context.waitFor(client -> client.player.getInventory().getItem(0).has(RemnantDataComponents.SELECTED_ITEM)
                    && client.player.getInventory().getItem(0).get(RemnantDataComponents.SELECTED_ITEM) != first);
            ItemStack chosen = context.computeOnClient(client -> BackpackItemInteraction.selectedStack(client.player.getInventory().getItem(0)));
            context.runOnClient(client -> verifyPreview(client.player.getInventory().getItem(0)));
            context.takeScreenshot("backpack-bundle-wheel-selection");
            context.getInput().pressMouse(3);
            context.waitFor(client -> ItemStack.matches(client.player.containerMenu.getCarried(), chosen));
            world.getServer().waitFor(server -> ItemStack.matches(
                    server.getPlayerList().getPlayers().getFirst().containerMenu.getCarried(), chosen));
            world.getServer().runOnServer(server -> require(ItemStack.matches(
                    server.getPlayerList().getPlayers().getFirst().containerMenu.getCarried(), chosen), "Server withdrawal differs"));
            move(context, 9); context.getInput().pressMouse(1);
            context.waitFor(client -> client.player.containerMenu.getCarried().isEmpty());
            world.getServer().waitFor(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                return player.containerMenu.getCarried().isEmpty() && ItemStack.matches(player.getInventory().getItem(9), chosen);
            });
            context.getInput().pressMouse(1);
            context.waitFor(client -> ItemStack.matches(client.player.containerMenu.getCarried(), chosen));
            world.getServer().waitFor(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                return ItemStack.matches(player.containerMenu.getCarried(), chosen) && player.getInventory().getItem(9).isEmpty();
            });
            move(context, 36); context.getInput().pressMouse(1);
            context.waitFor(client -> client.player.containerMenu.getCarried().isEmpty());
            world.getServer().waitFor(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                return player.containerMenu.getCarried().isEmpty() && originalTotals(player.getInventory().getItem(0));
            });
            world.getServer().runOnServer(server -> {
                var bag = server.getPlayerList().getPlayers().getFirst().getInventory().getItem(0);
                var content = bag.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().toList();
                require(content.stream().filter(i -> i.is(Items.APPLE)).mapToInt(ItemStack::getCount).sum() == 3
                        && content.stream().filter(i -> i.is(Items.DIRT)).mapToInt(ItemStack::getCount).sum() == 5,
                        "Native deposit/withdraw changed totals");
            });
            context.takeScreenshot("backpack-bundle-deposit-restored");
            context.getInput().pressMouse(1);
            context.waitFor(client -> client.player.containerMenu.getCarried().is(RemnantItemRegistration.BACKPACK_BASIC));
            world.getServer().waitFor(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                return player.containerMenu.getCarried().is(RemnantItemRegistration.BACKPACK_BASIC)
                        && player.getInventory().getItem(0).isEmpty();
            });
            // Place in a different empty slot; another immediate click on the
            // same slot is vanilla PICKUP_ALL rather than an ordinary placement.
            move(context, 37);
            context.getInput().pressMouse(1);
            context.waitFor(client -> client.player.containerMenu.getCarried().isEmpty());
            world.getServer().waitFor(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                return player.containerMenu.getCarried().isEmpty() && originalTotals(player.getInventory().getItem(1));
            });
            context.runOnClient(client -> client.player.closeContainer()); context.waitForScreen(null);

            var handle = context.computeOnClient(client -> {
                var slots = new ArrayList<ItemStack>(); for (int i = 0; i < 46; i++) slots.add(ItemStack.EMPTY);
                var bag = client.player.getInventory().getItem(1).copy(); BackpackItemInteraction.select(bag, 0); slots.set(9, bag);
                return new RemnantBackpackObserverScreenProvider().create(new ObserverScreenContext(UUID.randomUUID(), "Target", () -> {}),
                        new ObserverScreenSnapshot("remnant_backpack", "", 1, 1, Component.literal("Backpack"),
                                slots, new int[]{1, 1}, Map.of(), new byte[0]));
            });
            context.runOnClient(client -> {
                client.setScreenAndShow(handle.screen());
                var screen = (AbstractContainerScreen<?>) handle.screen(); var slot = screen.getMenu().getSlot(9);
                var before = slot.getItem().copy(); var actions = new BackpackMouseActions(screen);
                ObserverPacketProbe.reset();
                screen.mouseScrolled(20, 20, 0, 1);
                screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(20, 20,
                        new net.minecraft.client.input.MouseButtonInfo(2, 0)), false);
                new dev.totem.remnant.client.integration.RemnantSortClientProvider().apply(screen, slot);
                actions.onMouseScrolled(0, 1, 9, slot.getItem()); actions.onStopHovering(slot);
                require(ObserverPacketProbe.sends() == 0 && ItemStack.matches(before, slot.getItem()),
                        "Observer wheel mutated stack or sent a packet");
                verifyPreview(slot.getItem());
                var updated = slot.getItem().copy(); BackpackItemInteraction.select(updated, 1);
                var slots = new ArrayList<ItemStack>(screen.getMenu().getItems()); slots.set(9, updated);
                handle.applySnapshot(new ObserverScreenSnapshot("remnant_backpack", "", 1, 2,
                        Component.literal("Backpack"), slots, new int[]{1, 1}, Map.of(), new byte[0]));
                verifyPreview(slot.getItem());
                require(BackpackItemInteraction.selected(slot.getItem()) == 1 && ObserverPacketProbe.sends() == 0,
                        "Observer tooltip did not follow remote selection without packets");
            });
            context.takeScreenshot("backpack-bundle-observer-read-only");
            context.runOnClient(client -> client.setScreenAndShow(null));
            verifyLaterPage(context, world);
            verifyHoveredContainers(context, world);
        }
    }
    private static void verifyPreview(ItemStack bag) {
        var image = bag.getItem().getTooltipImage(bag).orElseThrow();
        require(image instanceof BackpackTooltip, "Missing backpack contents tooltip");
        var preview = (BackpackTooltip) image;
        require(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(image)
                instanceof ClientBackpackTooltip, "Backpack tooltip renderer not registered");
        require(ItemStack.matches(preview.items().get(preview.selectedIndex()).create(),
                BackpackItemInteraction.selectedStack(bag)), "Highlighted item differs from right-click withdrawal");
        require(preview.selectedIndex() >= preview.firstShownIndex()
                && preview.selectedIndex() < preview.firstShownIndex() + preview.shownItems().size(),
                "Selected item is hidden outside preview page");
        require(preview.shownItems().size() <= BackpackTooltip.PAGE_SIZE, "Unbounded tooltip grid");
    }

    private static void verifyLaterPage(ClientGameTestContext context,
            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            var bag = new ItemStack(RemnantItemRegistration.BACKPACK_STANDARD);
            var items = new ArrayList<ItemStack>();
            for (int i = 0; i < 18; i++) items.add(i == 0 || i == 5 ? ItemStack.EMPTY
                    : new ItemStack(i == 17 ? Items.NETHER_STAR : Items.DIAMOND, i + 1));
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            BackpackItemInteraction.select(bag, 16);
            player.getInventory().setItem(0, bag); player.inventoryMenu.broadcastFullState();
        });
        context.waitFor(client -> client.player.getInventory().getItem(0).is(RemnantItemRegistration.BACKPACK_STANDARD));
        context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
        move(context, 36); context.getInput().scroll(0, -1);
        context.waitFor(client -> BackpackItemInteraction.selectedStack(client.player.getInventory().getItem(0)).is(Items.NETHER_STAR));
        context.runOnClient(client -> {
            var bag = client.player.getInventory().getItem(0); verifyPreview(bag);
            var preview = (BackpackTooltip) bag.getItem().getTooltipImage(bag).orElseThrow();
            require(preview.selectedIndex() == 15 && preview.firstShownIndex() == 12,
                    "Sparse storage indices were not mapped to the selected preview page");
            var stale = bag.copy(); stale.set(RemnantDataComponents.SELECTED_ITEM, 999);
            verifyPreview(stale);
            require(((BackpackTooltip) stale.getItem().getTooltipImage(stale).orElseThrow()).selectedIndex() == 0,
                    "Stale selection should highlight the same first item that withdrawal uses");
            require(new ItemStack(RemnantItemRegistration.BACKPACK_BASIC).getItem()
                    .getTooltipImage(new ItemStack(RemnantItemRegistration.BACKPACK_BASIC)).isEmpty(),
                    "Empty backpack must not show a stale selection");
        });
        context.takeScreenshot("backpack-bundle-wheel-later-page");
        String language = context.computeOnClient(client -> client.getLanguageManager().getSelected());
        var reload = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>>();
        context.runOnClient(client -> {
            client.options.languageCode = "zh_tw"; client.getLanguageManager().setSelected("zh_tw");
            reload.set(client.reloadResourcePacks());
        });
        context.waitFor(client -> reload.get() != null && reload.get().isDone() && client.gui.overlay() == null);
        selectLastAfterReload(context);
        context.takeScreenshot("backpack-bundle-wheel-later-page-zh-tw");
        context.runOnClient(client -> {
            client.options.languageCode = language; client.getLanguageManager().setSelected(language);
            reload.set(client.reloadResourcePacks());
        });
        context.waitFor(client -> reload.get().isDone() && client.gui.overlay() == null);
        selectLastAfterReload(context);
        context.getInput().pressMouse(3);
        context.waitFor(client -> client.player.containerMenu.getCarried().is(Items.NETHER_STAR)
                && client.player.containerMenu.getCarried().getCount() == 18);
        world.getServer().waitFor(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            return player.containerMenu.getCarried().is(Items.NETHER_STAR)
                    && player.containerMenu.getCarried().getCount() == 18;
        });
        // Inventory has no server menu to close; a late close packet could close the next fixture's chest.
        context.runOnClient(client -> client.setScreenAndShow(null)); context.waitForScreen(null);
    }
    private static void selectLastAfterReload(ClientGameTestContext context) {
        // Native screen/resource reinitialization ends the previous hover and clears selection.
        context.getInput().setCursorPos(0, 0); context.waitTicks(2);
        move(context, 36); context.getInput().scroll(0, 1);
        context.waitFor(client -> BackpackItemInteraction.selectedStack(client.player.getInventory().getItem(0)).is(Items.NETHER_STAR));
        context.runOnClient(client -> verifyPreview(client.player.getInventory().getItem(0)));
    }
    private static void verifyHoveredContainers(ClientGameTestContext context,
            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst(); player.getInventory().clearContent();
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            var chest = new net.minecraft.world.SimpleContainer(9);
            chest.setItem(4, unsortedBag()); chest.setItem(0, new ItemStack(Items.DIRT, 19));
            player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 7));
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, ignored) ->
                    new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1,
                            id, inventory, chest, 1), Component.literal("Hovered Backpack")));
        });
        context.waitForScreen(net.minecraft.client.gui.screens.inventory.ContainerScreen.class);
        context.waitFor(client -> client.player.containerMenu.getSlot(4).getItem().is(RemnantItemRegistration.BACKPACK_BASIC));
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            System.out.println("HOVER_SORT_BEFORE menu=" + p.containerMenu.containerId + " state=" + p.containerMenu.getStateId());
        });
        move(context, 4); context.takeScreenshot("backpack-hover-chest-before-sort");
        context.runOnClient(client -> ObserverPacketProbe.reset());
        context.getInput().pressMouse(2);
        context.waitTicks(2);
        context.runOnClient(client -> require(ObserverPacketProbe.lastHoveredSort() != null
                && ObserverPacketProbe.lastHoveredSort().slot() == 4, "Native middle click did not target chest backpack slot4"));
        var request = context.computeOnClient(client -> ObserverPacketProbe.lastHoveredSort());
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst(); var menu = p.containerMenu; var slot = menu.getSlot(4);
            System.out.println("HOVER_SORT_AFTER request=" + request.menuId() + "/" + request.stateId()
                    + " server=" + menu.containerId + "/" + menu.getStateId() + " class=" + menu.getClass().getSimpleName()
                    + " match=" + ItemStack.matches(slot.getItem(), request.expected()) + " valid=" + menu.stillValid(p)
                    + " active=" + slot.isActive() + " pickup=" + slot.mayPickup(p) + " alive=" + p.isAlive()
                    + " spectator=" + p.isSpectator() + " root=" + request.selectedRootSlot()
                    + " sorted=" + previewSorted(slot.getItem()));
        });
        context.waitFor(client -> previewSorted(client.player.containerMenu.getSlot(4).getItem()));
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            require(previewSorted(player.containerMenu.getSlot(4).getItem()), "Chest hover sort did not persist");
            require(player.containerMenu.getSlot(0).getItem().getCount() == 19
                    && player.getInventory().getItem(9).getCount() == 7, "Hover sorting changed surrounding surfaces");
        });
        context.takeScreenshot("backpack-hover-middle-click-chest-sorted");
        context.runOnClient(client -> client.player.closeContainer()); context.waitForScreen(null);
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst(); player.getInventory().clearContent();
            var root = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
            root.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(unsortedBag(), new ItemStack(Items.DIAMOND, 7))));
            player.getInventory().setItem(0, root); player.inventoryMenu.broadcastFullState();
        });
        context.waitFor(client -> client.player.getInventory().getItem(0).is(RemnantItemRegistration.DEATH_BACKPACK));
        context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
        move(context, 36);
        context.waitFor(client -> ((dev.totem.remnant.inventory.BackpackPanelMenuAccess) client.player.inventoryMenu)
                .totem$getBackpackPanel().selectedInventorySlot() == 0);
        int index = context.computeOnClient(client -> ((dev.totem.remnant.inventory.BackpackPanelMenuAccess)
                client.player.inventoryMenu).totem$getBackpackPanelSlotStart());
        move(context, index); context.getInput().pressMouse(2);
        context.waitFor(client -> previewSorted(client.player.getInventory().getItem(0)
                .get(DataComponents.CONTAINER).itemCopies().findFirst().orElse(ItemStack.EMPTY)));
        world.getServer().runOnServer(server -> {
            var root = server.getPlayerList().getPlayers().getFirst().getInventory().getItem(0);
            var contents = root.get(DataComponents.CONTAINER).itemCopies().toList();
            require(previewSorted(contents.getFirst()) && contents.get(1).getCount() == 7, "Captured backpack hover sort lost outer contents");
        });
        context.takeScreenshot("backpack-hover-middle-click-captured-sorted");
        context.runOnClient(client -> client.player.closeContainer()); context.waitForScreen(null);
    }
    private static ItemStack unsortedBag() {
        var bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                new ItemStack(Items.DIRT, 2), new ItemStack(Items.APPLE, 3), new ItemStack(Items.DIRT, 3)))); return bag;
    }
    private static boolean previewSorted(ItemStack bag) {
        if (!bag.is(RemnantItemRegistration.BACKPACK_BASIC)) return false;
        var items = bag.get(DataComponents.CONTAINER).itemCopies().filter(item -> !item.isEmpty()).toList();
        return items.size() == 2 && items.get(0).is(Items.APPLE) && items.get(0).getCount() == 3
                && items.get(1).is(Items.DIRT) && items.get(1).getCount() == 5;
    }
    private static void move(ClientGameTestContext context, int index) {
        int[] point = context.computeOnClient(client -> {
            var screen = (AbstractContainerScreen<?>) client.gui.screen(); var slot = screen.getMenu().getSlot(index);
            double sx = (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth();
            double sy = (double) client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight();
            var layout = (dev.totem.remnant.client.screen.BackpackSidebarLayout) screen;
            return new int[]{(int) Math.round((layout.remnant$left() + slot.x + 8) * sx),
                    (int) Math.round((layout.remnant$top() + slot.y + 8) * sy)};
        });
        context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(2);
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static boolean originalTotals(ItemStack bag) {
        var items = bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList();
        return bag.is(RemnantItemRegistration.BACKPACK_BASIC)
                && items.stream().filter(item -> item.is(Items.APPLE)).mapToInt(ItemStack::getCount).sum() == 3
                && items.stream().filter(item -> item.is(Items.DIRT)).mapToInt(ItemStack::getCount).sum() == 5;
    }
}
