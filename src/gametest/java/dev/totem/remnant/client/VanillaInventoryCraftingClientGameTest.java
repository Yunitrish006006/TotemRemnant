package dev.totem.remnant.client;

import dev.totem.remnant.registry.RemnantItemRegistration;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Reproduces ordinary survival inventory crafting through physical mouse input. */
@SuppressWarnings("UnstableApiUsage")
public final class VanillaInventoryCraftingClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientLevel().waitForChunksRender();
            for (boolean backpack : new boolean[]{false, true}) {
                for (int column = 0; column < 2; column++) {
                    craftSticks(context, world, backpack, column, false);
                }
            }
            craftSticks(context, world, true, 1, true);
            collectMatchingStacks(context, world, true);
            collectMatchingStacks(context, world, false);
        }
    }

    private static void collectMatchingStacks(ClientGameTestContext context,
                                              TestSingleplayerContext world, boolean panelOrigin) {
        context.runOnClient(client -> client.setScreenAndShow(null));
        context.waitTicks(5);
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.closeContainer();
            player.getInventory().clearContent();
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                    new ItemStack(Items.REDSTONE, 2), new ItemStack(Items.REDSTONE, 6))));
            player.getInventory().setItem(0, backpack);
            player.getInventory().setItem(1, new ItemStack(Items.REDSTONE, 3));
            player.getInventory().setItem(2, new ItemStack(Items.REDSTONE, 9));
            ((BackpackPanelMenuAccess) player.inventoryMenu).totem$selectBackpackSlot(0);
            player.inventoryMenu.broadcastFullState();
        });
        context.waitFor(client -> client.player.getInventory().getItem(1).getCount() == 3
                && client.player.inventoryMenu.getCarried().isEmpty());
        context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(20);
        int panelStart = context.computeOnClient(client ->
                ((BackpackPanelMenuAccess) client.player.inventoryMenu).totem$getBackpackPanelSlotStart());
        int[] origin = point(context, panelOrigin ? panelStart : 37);
        context.getInput().setCursorPos(origin[0], origin[1]);
        context.getInput().pressMouse(0);
        context.getInput().pressMouse(0);
        context.waitTicks(10);
        context.runOnClient(client -> assertCollection(client.player.inventoryMenu, panelOrigin, "client"));
        world.getServer().runOnServer(server -> assertCollection(
                server.getPlayerList().getPlayers().getFirst().inventoryMenu, panelOrigin, "server"));
        context.getInput().setCursorPos(10, 10);
        context.waitTicks(2);
        context.takeScreenshot("double-click-origin-" + (panelOrigin ? "backpack" : "inventory"));
        System.out.println("REMNANT_DOUBLE_CLICK origin=" + (panelOrigin ? "backpack" : "inventory")
                + " client/server collection and opposite-surface preservation passed");
    }

    private static void assertCollection(InventoryMenu menu, boolean panelOrigin, String side) {
        int panelStart = ((BackpackPanelMenuAccess) menu).totem$getBackpackPanelSlotStart();
        int expected = panelOrigin ? 8 : 12;
        if (!menu.getCarried().is(Items.REDSTONE) || menu.getCarried().getCount() != expected
                || menu.getSlot(panelStart).getItem().getCount() != (panelOrigin ? 0 : 2)
                || menu.getSlot(panelStart + 1).getItem().getCount() != (panelOrigin ? 0 : 6)
                || menu.getSlot(37).getItem().getCount() != (panelOrigin ? 3 : 0)
                || menu.getSlot(38).getItem().getCount() != (panelOrigin ? 9 : 0)) {
            throw new AssertionError("Double-click collection crossed surfaces or lost items: "
                    + side + ", panelOrigin=" + panelOrigin + ", carried=" + item(menu.getCarried()));
        }
    }

    private static void craftSticks(ClientGameTestContext context, TestSingleplayerContext world,
                                    boolean backpack, int column, boolean drag) {
        String scenario = "backpack=" + backpack + ", column=" + column + ", drag=" + drag;
        context.runOnClient(client -> client.setScreenAndShow(null));
        context.waitTicks(5);
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.closeContainer();
            player.getInventory().clearContent();
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            for (int slot = 1; slot <= 4; slot++) player.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
            if (backpack) player.getInventory().setItem(0,
                    new ItemStack(RemnantItemRegistration.BACKPACK_BASIC));
            player.getInventory().setItem(3, new ItemStack(drag ? Items.SPRUCE_PLANKS : Items.OAK_PLANKS, drag ? 4 : 2));
            player.inventoryMenu.broadcastFullState();
        });
        context.waitFor(client -> client.player != null
                && client.player.getInventory().getItem(3).is(drag ? Items.SPRUCE_PLANKS : Items.OAK_PLANKS)
                && client.player.getInventory().getItem(3).getCount() == (drag ? 4 : 2));
        context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
        context.waitForScreen(InventoryScreen.class);
        context.waitTicks(20);
        click(context, 39, 0); // Vanilla hotbar inventory index 3.
        if (drag) {
            int[] first = point(context, 1 + column);
            int[] second = point(context, 3 + column);
            context.getInput().setCursorPos(first[0], first[1]);
            context.getInput().holdMouse(1);
            context.waitTicks(2);
            context.getInput().moveCursor(1, 0);
            context.waitTicks(2);
            context.getInput().setCursorPos(second[0], second[1]);
            context.waitTicks(2);
            context.getInput().releaseMouse(1);
        } else {
            click(context, 1 + column, 1);
            click(context, 3 + column, 1);
        }
        context.waitTicks(20);

        AtomicReference<String> serverState = new AtomicReference<>();
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            var grid = new ArrayList<ItemStack>();
            for (int slot = 1; slot <= 4; slot++) grid.add(player.inventoryMenu.getSlot(slot).getItem().copy());
            var recipe = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                    CraftingInput.of(2, 2, grid), player.level());
            serverState.set(state(player.inventoryMenu)
                    + ", recipe=" + recipe.map(holder -> holder.id().toString()).orElse("none")
                    + ", known=" + recipe.map(holder -> player.getRecipeBook().contains(holder.id())).orElse(false)
                    + ", limitedCrafting=" + server.getGameRules().get(GameRules.LIMITED_CRAFTING));
        });
        String clientState = context.computeOnClient(client -> state(client.player.inventoryMenu));
        System.out.println("REMNANT_STICKS " + scenario + "; server=" + serverState.get() + "; client=" + clientState);
        context.getInput().setCursorPos(10, 10);
        context.waitTicks(2);
        context.takeScreenshot("vanilla-sticks-backpack-" + backpack + "-column-" + column + "-drag-" + drag);
        context.runOnClient(client -> {
            if (!isFourSticks(client.player.inventoryMenu.getSlot(0).getItem())) {
                throw new AssertionError("No four-stick result: " + scenario
                        + "; server=" + serverState.get() + "; client=" + clientState);
            }
        });
        world.getServer().runOnServer(server -> {
            if (!isFourSticks(server.getPlayerList().getPlayers().getFirst().inventoryMenu.getSlot(0).getItem())) {
                throw new AssertionError("Server result diverged: " + scenario + "; " + serverState.get());
            }
        });
        if (drag) {
            context.runOnClient(client -> {
                ItemStack carried = client.player.inventoryMenu.getCarried();
                if (!carried.is(Items.SPRUCE_PLANKS) || carried.getCount() != 2) {
                    throw new AssertionError("Right drag did not retain exactly two planks: " + state(client.player.inventoryMenu));
                }
            });
            click(context, 39, 0);
        }
        click(context, 0, 0);
        context.waitTicks(10);
        context.runOnClient(client -> assertConsumed(client.player.inventoryMenu, scenario + ", client"));
        world.getServer().runOnServer(server -> assertConsumed(
                server.getPlayerList().getPlayers().getFirst().inventoryMenu, scenario + ", server"));
    }

    private static void assertConsumed(InventoryMenu menu, String scenario) {
        if (!isFourSticks(menu.getCarried())) throw new AssertionError("Result pickup failed: " + scenario + "; " + state(menu));
        for (int slot = 1; slot <= 4; slot++) {
            if (menu.getSlot(slot).hasItem()) throw new AssertionError("Input not consumed: " + scenario + "; " + state(menu));
        }
    }

    private static boolean isFourSticks(ItemStack stack) {
        return stack.is(Items.STICK) && stack.getCount() == 4;
    }

    private static String state(InventoryMenu menu) {
        StringBuilder text = new StringBuilder("menu=" + menu.containerId + ", state=" + menu.getStateId());
        for (int slot = 0; slot <= 4; slot++) text.append(", slot").append(slot).append('=').append(item(menu.getSlot(slot).getItem()));
        return text.append(", carried=").append(item(menu.getCarried())).toString();
    }

    private static String item(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "x" + stack.getCount();
    }

    private static void click(ClientGameTestContext context, int slotIndex, int button) {
        int[] point = point(context, slotIndex);
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(button);
        context.waitTicks(3);
    }

    private static int[] point(ClientGameTestContext context, int slotIndex) {
        return context.computeOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) client.gui.screen();
            var slot = screen.getMenu().getSlot(slotIndex);
            double sx = (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth();
            double sy = (double) client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight();
            return new int[]{(int) Math.round(((screen.width - 176) / 2 + slot.x + 9) * sx),
                    (int) Math.round(((screen.height - 166) / 2 + slot.y + 9) * sy)};
        });
    }
}
