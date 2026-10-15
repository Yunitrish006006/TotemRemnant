package dev.totem.remnant.client;

import dev.totem.remnant.client.screen.BackpackScreen;
import dev.totem.remnant.client.screen.RemnantBackpackObserverScreenProvider;
import dev.totem.core.api.v1.client.observer.*;
import dev.totem.remnant.inventory.BackpackMenu;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Production input, packet, persistence and owner-provider checks for both new actions. */
@SuppressWarnings("UnstableApiUsage")
public final class BackpackActionsClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        String expectedVanillaTweaks = System.getProperty("totem.remnant.test.expectedVanillaTweaks");
        if (expectedVanillaTweaks != null) {
            require(expectedVanillaTweaks.equals("true") || expectedVanillaTweaks.equals("false"),
                    "Invalid expected VanillaTweaks presence");
            require(FabricLoader.getInstance().isModLoaded("totem-vanilla-tweaks")
                            == Boolean.parseBoolean(expectedVanillaTweaks),
                    "VanillaTweaks presence does not match this validation run");
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.getInput().resizeWindow(1280, 720);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                ItemStack bag = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
                ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET); helmet.setDamageValue(19);
                bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                        new ItemStack(Items.DIRT, 32), helmet, new ItemStack(Items.DIRT, 40), new ItemStack(Items.APPLE, 2))));
                player.getInventory().setItem(0, bag);
                player.inventoryMenu.broadcastFullState();
                bag.use(player.level(), player, InteractionHand.MAIN_HAND);
            });
            context.waitForScreen(BackpackScreen.class);
            context.runOnClient(client -> {
                BackpackScreen screen = (BackpackScreen) client.gui.screen();
                require(screen.getMenu().isDeathBackpack() && screen.equipButton() != null, "Death equip UI missing");
                screen.equipButton().onPress(new KeyEvent(257, 0, 0));
            });
            context.waitFor(client -> client.player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET));
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                require(player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == 19, "Equip lost durability");
                require(((BackpackMenu) player.containerMenu).storage().getItem(1).isEmpty(), "Equip duplicated source");
            });
            context.takeScreenshot("death-backpack-one-click-equip");

            if (FabricLoader.getInstance().isModLoaded("totem-vanilla-tweaks")) {
                clickMiddle(context, 0);
                context.waitFor(client -> ((BackpackScreen) client.gui.screen()).getMenu().getSlot(0).getItem().is(Items.APPLE));
                world.getServer().runOnServer(server -> {
                    var menu = (BackpackMenu) server.getPlayerList().getPlayers().getFirst().containerMenu;
                    require(menu.getSlot(1).getItem().getCount() == 64 && menu.getSlot(2).getItem().getCount() == 8,
                            "Real middle click did not safely merge storage");
                });
                context.takeScreenshot("death-backpack-middle-click-sorted");
            }

            ObserverScreenSnapshot snapshot = context.computeOnClient(client -> new RemnantBackpackObserverScreenProvider()
                    .capture(client.gui.screen(), 1).orElseThrow());
            require(snapshot.variant().equals("death"), "Death snapshot has wrong variant");
            context.runOnClient(client -> client.player.closeContainer());
            context.waitForScreen(null);
            AtomicInteger stops = new AtomicInteger();
            ObserverScreenHandle handle = context.computeOnClient(client -> new RemnantBackpackObserverScreenProvider().create(
                    new ObserverScreenContext(UUID.randomUUID(), "Target", stops::incrementAndGet), snapshot));
            context.runOnClient(client -> {
                client.setScreenAndShow(handle.screen());
                BackpackScreen screen = (BackpackScreen) handle.screen();
                require(screen.totem$isObserverReadOnly() && !screen.equipButton().active, "Observer equip is active");
                var updatedItems = new java.util.ArrayList<ItemStack>(snapshot.slots());
                updatedItems.set(0, new ItemStack(Items.GOLD_INGOT, 3));
                handle.applySnapshot(new ObserverScreenSnapshot(snapshot.familyId(), "", snapshot.protocolVersion(), 99,
                        snapshot.title(), updatedItems, snapshot.data(), snapshot.metadata(), snapshot.ownerPayload()));
                handle.applySnapshot(new ObserverScreenSnapshot(snapshot.familyId(), "death", snapshot.protocolVersion(), 2,
                        snapshot.title(), updatedItems, snapshot.data(), snapshot.metadata(), snapshot.ownerPayload()));
                handle.applySnapshot(snapshot);
                require(screen.getMenu().getSlot(0).getItem().is(Items.GOLD_INGOT)
                                && screen.getMenu().getSlot(0).getItem().getCount() == 3,
                        "Death snapshot variant/monotonic update policy failed");
                ObserverPacketProbe.reset();
                screen.equipButton().onPress(new KeyEvent(257, 0, 0));
                screen.mouseClicked(new MouseButtonEvent(20, 20, new MouseButtonInfo(2, 0)), false);
                require(ObserverPacketProbe.sends() == 0, "Readonly actions sent packets");
                handle.applyCursor(new ObserverRemoteCursor(2, 20, 20, 176, 166, new ItemStack(Items.DIAMOND, 2)));
                handle.applyCursor(new ObserverRemoteCursor(1, 0, 0, 176, 166, ItemStack.EMPTY));
                require(screen.getMenu().getCarried().getCount() == 2, "Cursor sequence regressed");
            });
            context.takeScreenshot("death-backpack-observer-read-only");
            context.runOnClient(client -> {
                // Screenshots yield frames; ordinary world packets between frames are
                // unrelated to this synchronous read-only keyboard action.
                ObserverPacketProbe.reset();
                handle.screen().keyPressed(new KeyEvent(256, 0, 0));
                require(stops.get() == 1, "Observer close did not request stop exactly once");
                require(ObserverPacketProbe.sends() == 0, "Observer close attempted mutation");
                client.setScreenAndShow(null);
            });
            if (FabricLoader.getInstance().isModLoaded("totem-vanilla-tweaks")) {
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    ItemStack bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
                    bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIRT, 32),
                            new ItemStack(Items.APPLE, 1), new ItemStack(Items.DIRT, 40))));
                    player.getInventory().setItem(0, bag); player.inventoryMenu.broadcastFullState();
                });
                context.waitFor(client -> client.player.getInventory().getItem(0).is(RemnantItemRegistration.BACKPACK_BASIC));
                context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
                context.waitTicks(10);
                int start = context.computeOnClient(client -> ((BackpackPanelMenuAccess) client.player.inventoryMenu).totem$getBackpackPanelSlotStart());
                clickMiddle(context, start);
                context.waitFor(client -> client.player.inventoryMenu.getSlot(start).getItem().is(Items.APPLE));
                world.getServer().runOnServer(server -> {
                    var menu = server.getPlayerList().getPlayers().getFirst().inventoryMenu;
                    require(menu.getSlot(start + 1).getItem().getCount() == 64, "E panel middle click routed incorrectly");
                });
                context.takeScreenshot("backpack-panel-middle-click-sorted");
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.getInventory().setItem(9, new ItemStack(Items.DIRT, 20));
                    player.inventoryMenu.broadcastChanges();
                });
                context.waitFor(client -> client.player.getInventory().getItem(9).getCount() == 20);
                clickMiddle(context, playerMenuSlot(context, 9));
                context.waitFor(client -> storedDirt(client.player.getInventory().getItem(0)) == 92);
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    require(storedDirt(player.getInventory().getItem(0)) == 92 && player.getInventory().getItem(9).isEmpty(),
                            "Player matching deposit failed or duplicated source");
                });
                context.takeScreenshot("player-matching-backpack-deposit");
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    require(((BackpackPanelMenuAccess) player.inventoryMenu).totem$getBackpackPanel().selectedInventorySlot() == 0,
                            "Close barrier needs the selected fixture backpack");
                });
                context.runOnClient(client -> {
                    client.player.closeContainer();
                    ((BackpackPanelMenuAccess) client.player.inventoryMenu).totem$selectBackpackSlot(-1);
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            new dev.totem.remnant.network.SelectBackpackPanelPayload(-1));
                });
                context.waitForScreen(null);
                // InventoryMenu already has id zero while the E screen is open, so
                // merely observing id zero cannot acknowledge its close packet.
                // This later selection packet uses the same ordered connection.
                world.getServer().waitFor(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    return player.containerMenu == player.inventoryMenu
                            && ((BackpackPanelMenuAccess) player.inventoryMenu).totem$getBackpackPanel().selectedInventorySlot() == -1;
                });
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    var chest = new net.minecraft.world.SimpleContainer(9);
                    ItemStack bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
                    bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIRT, 4))));
                    chest.setItem(0, bag); chest.setItem(1, new ItemStack(Items.DIRT, 20));
                    player.getInventory().setItem(9, new ItemStack(Items.APPLE, 7));
                    player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, ignored) ->
                            new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inventory, chest, 1), Component.literal("Matching Deposit")));
                });
                context.waitForScreen(net.minecraft.client.gui.screens.inventory.ContainerScreen.class);
                context.runOnClient(client -> ObserverPacketProbe.reset());
                clickMiddle(context, 1);
                context.waitTicks(3);
                var request = context.computeOnClient(client -> ObserverPacketProbe.lastSort());
                context.takeScreenshot("chest-matching-input-diagnostic");
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    require(storedDirt(player.containerMenu.getSlot(0).getItem()) == 24,
                            "Chest input did not deposit; request=" + (request == null ? "none" : request.menuId()
                                    + "/" + request.stateId() + "/" + request.hoveredSlot())
                                    + "; server=" + player.containerMenu.containerId + "/" + player.containerMenu.getStateId()
                                    + "; dirt=" + storedDirt(player.containerMenu.getSlot(0).getItem()));
                });
                context.waitFor(client -> storedDirt(client.player.containerMenu.getSlot(0).getItem()) == 24);
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    require(storedDirt(player.containerMenu.getSlot(0).getItem()) == 24
                                    && player.containerMenu.getSlot(1).getItem().isEmpty()
                                    && player.getInventory().getItem(9).getCount() == 7,
                            "Chest matching action crossed surface or lost items");
                    player.getInventory().setItem(9, new ItemStack(Items.DIRT));
                    player.containerMenu.broadcastChanges();
                });
                context.waitFor(client -> client.player.getInventory().getItem(9).is(Items.DIRT));
                clickMiddle(context, playerMenuSlot(context, 9));
                context.waitFor(client -> storedDirt(client.player.getInventory().getItem(0)) == 93);
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    require(storedDirt(player.containerMenu.getSlot(0).getItem()) == 24, "Player-side click mutated chest backpack");
                });
                context.takeScreenshot("chest-matching-backpack-deposit");
                context.runOnClient(client -> client.player.closeContainer());
            }
        }
    }
    private static void clickMiddle(ClientGameTestContext context, int slot) {
        int[] pos = context.computeOnClient(client -> {
            var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) client.gui.screen();
            var s = screen.getMenu().getSlot(slot);
            int left = (screen.width - 176) / 2;
            int height = screen.getMenu() instanceof net.minecraft.world.inventory.ChestMenu chestMenu
                    ? 114 + chestMenu.getRowCount() * 18 : 166;
            if (screen instanceof BackpackScreen backpackScreen) {
                height = 114 + Math.min(backpackScreen.getMenu().getRowCount(), 6) * 18;
                if (!backpackScreen.getMenu().isDeathBackpack())
                    left -= (BackpackMenu.CRAFTING_PANEL_X + BackpackMenu.CRAFTING_PANEL_WIDTH - 176) / 2;
            }
            int top = (screen.height - height) / 2;
            double xScale = (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth();
            double yScale = (double) client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight();
            return new int[]{(int) Math.round((left + s.x + 8) * xScale), (int) Math.round((top + s.y + 8) * yScale)};
        });
        context.getInput().setCursorPos(pos[0], pos[1]); context.waitTicks(2); context.getInput().pressMouse(2);
    }
    private static int playerMenuSlot(ClientGameTestContext context, int inventorySlot) {
        return context.computeOnClient(client -> client.player.containerMenu.slots.stream()
                .filter(slot -> slot.container == client.player.getInventory() && slot.getContainerSlot() == inventorySlot)
                .findFirst().orElseThrow().index);
    }
    private static int storedDirt(ItemStack bag) {
        return bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream()
                .filter(item -> item.is(Items.DIRT)).mapToInt(ItemStack::getCount).sum();
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
