package dev.totem.remnant.client;

import dev.totem.core.api.v1.client.observer.*;
import dev.totem.remnant.client.screen.RemnantBackpackObserverScreenProvider;
import dev.totem.remnant.inventory.BackpackItemInteraction;
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
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); p.getInventory().clearContent();
                var bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
                bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                        new ItemStack(Items.APPLE, 3), new ItemStack(Items.DIRT, 5))));
                p.getInventory().setItem(0, bag); p.inventoryMenu.broadcastFullState();
            });
            context.waitFor(client -> client.player.getInventory().getItem(0).is(RemnantItemRegistration.BACKPACK_BASIC));
            context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
            move(context, 36); context.getInput().scroll(0, 1);
            context.waitFor(client -> client.player.getInventory().getItem(0).has(RemnantDataComponents.SELECTED_ITEM));
            int first = context.computeOnClient(client -> client.player.getInventory().getItem(0).get(RemnantDataComponents.SELECTED_ITEM));
            context.getInput().scroll(0, 1);
            context.waitFor(client -> client.player.getInventory().getItem(0).has(RemnantDataComponents.SELECTED_ITEM)
                    && client.player.getInventory().getItem(0).get(RemnantDataComponents.SELECTED_ITEM) != first);
            ItemStack chosen = context.computeOnClient(client -> BackpackItemInteraction.selectedStack(client.player.getInventory().getItem(0)));
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
                actions.onMouseScrolled(0, 1, 9, slot.getItem()); actions.onStopHovering(slot);
                require(ObserverPacketProbe.sends() == 0 && ItemStack.matches(before, slot.getItem()),
                        "Observer wheel mutated stack or sent a packet");
            });
            context.takeScreenshot("backpack-bundle-observer-read-only");
            context.runOnClient(client -> client.setScreenAndShow(null));
        }
    }
    private static void move(ClientGameTestContext context, int index) {
        int[] point = context.computeOnClient(client -> {
            var screen = (AbstractContainerScreen<?>) client.gui.screen(); var slot = screen.getMenu().getSlot(index);
            double sx = (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth();
            double sy = (double) client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight();
            return new int[]{(int) Math.round(((screen.width - 176) / 2 + slot.x + 8) * sx),
                    (int) Math.round(((screen.height - 166) / 2 + slot.y + 8) * sy)};
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
