package dev.totem.remnant.client;

import dev.totem.remnant.registry.RemnantItemRegistration;
import dev.totem.remnant.client.screen.BackpackScreen;
import dev.totem.remnant.inventory.BackpackMenu;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** Captures the real vanilla inventory with Remnant's adjacent ordinary-backpack panel. */
@SuppressWarnings("UnstableApiUsage")
public final class BackpackInventoryPanelVisualGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        selectLanguage(context, "zh_tw");
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getServer().runOnServer(server -> {
                var players = server.getPlayerList().getPlayers();
                if (players.isEmpty()) {
                    throw new IllegalStateException("Client GameTest server did not provide a player");
                }
                var player = players.getFirst();
                player.getInventory().setItem(0, visualBackpack());
                player.getInventory().setItem(1,
                        new ItemStack(RemnantItemRegistration.BACKPACK_BASIC));
                player.inventoryMenu.broadcastFullState();
            });
            context.waitFor(client -> client.player != null
                    && client.player.getInventory().getItem(0)
                    .is(RemnantItemRegistration.BACKPACK_ADVANCED));
            context.runOnClient(client -> {
                if (client.player == null) {
                    throw new IllegalStateException("Client GameTest did not provide a player inventory");
                }
                client.setScreenAndShow(new InventoryScreen(client.player));
            });
            context.waitForScreen(InventoryScreen.class);
            context.waitTicks(120);
            context.runOnClient(client -> {
                BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) client.player.inventoryMenu;
                int firstPanelSlot = access.totem$getBackpackPanelSlotStart();
                if (!client.player.inventoryMenu.getSlot(firstPanelSlot).isActive()
                        || !client.player.inventoryMenu.getSlot(firstPanelSlot).getItem().is(Items.DIAMOND)
                        || client.player.inventoryMenu.getSlot(firstPanelSlot).x <= -1_000) {
                    throw new AssertionError("Backpack side panel must expose positioned real menu slots");
                }
            });
            context.takeScreenshot("inventory-backpack-side-panel");

            int[] firstSlot = panelSlotCenter(context, 0);
            int[] secondSlot = panelSlotCenter(context, 1);
            context.getInput().setCursorPos(firstSlot[0], firstSlot[1]);
            context.getInput().pressMouse(0);
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (!client.player.inventoryMenu.getCarried().is(Items.DIAMOND)) {
                    throw new AssertionError("Clicking a backpack panel slot must pick up its stack");
                }
            });
            context.getInput().setCursorPos(secondSlot[0], secondSlot[1]);
            context.getInput().pressMouse(0);
            context.waitTicks(3);
            context.runOnClient(client -> {
                BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) client.player.inventoryMenu;
                int secondPanelSlot = access.totem$getBackpackPanelSlotStart() + 1;
                if (!client.player.inventoryMenu.getCarried().isEmpty()
                        || !client.player.inventoryMenu.getSlot(secondPanelSlot).getItem().is(Items.DIAMOND)) {
                    throw new AssertionError("Clicking an empty backpack panel slot must place the carried stack");
                }
            });
            context.takeScreenshot("inventory-backpack-side-panel-interactive");

            selectLanguage(context, "en_us");
            context.waitTicks(20);
            context.takeScreenshot("inventory-backpack-side-panel-en-us");
            selectLanguage(context, "zh_tw");
            context.waitTicks(20);

            context.runOnClient(client -> {
                BackpackMenu menu = BackpackMenu.clientSide(
                        7, client.player.getInventory(), 4, 4);
                menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 12));
                menu.getSlot(3).set(new ItemStack(Items.IRON_PICKAXE));
                menu.getSlot(72).set(new ItemStack(RemnantItemRegistration.UPGRADE_CRAFTING));
                menu.getSlot(73).set(new ItemStack(RemnantItemRegistration.UPGRADE_BLAST_PROTECTION));
                menu.getSlot(74).set(new ItemStack(RemnantItemRegistration.UPGRADE_FIRE_PROTECTION));
                menu.getSlot(75).set(new ItemStack(RemnantItemRegistration.UPGRADE_VOID_PROTECTION));
                menu.craftingInputSlots().get(0).set(new ItemStack(Items.OAK_PLANKS));
                menu.craftingInputSlots().get(1).set(new ItemStack(Items.OAK_PLANKS));
                menu.craftingInputSlots().get(3).set(new ItemStack(Items.OAK_PLANKS));
                menu.craftingInputSlots().get(4).set(new ItemStack(Items.OAK_PLANKS));
                menu.craftingResultSlot().set(new ItemStack(Items.CRAFTING_TABLE));
                client.player.containerMenu = menu;
                client.setScreenAndShow(new BackpackScreen(
                        menu,
                        client.player.getInventory(),
                        Component.translatable("container.deadrecall.backpack.netherite")
                ));
            });
            context.waitForScreen(BackpackScreen.class);
            context.waitTicks(10);
            context.takeScreenshot("backpack-upgrade-bay");

            context.runOnClient(client -> client.options.guiScale().set(2));
            context.getInput().resizeWindow(1280, 720);
            context.waitTicks(5);
            for (int modules = 1; modules <= 4; modules++) {
                int capacityModules = modules;
                context.runOnClient(client -> showCapacityBackpack(client, capacityModules));
                context.waitForScreen(BackpackScreen.class);
                context.waitTicks(10);
                context.getInput().setCursorPos(10, 10);
                context.waitTick();
                context.takeScreenshot("backpack-capacity-" + modules + "-modules");
            }
            context.runOnClient(client -> client.options.guiScale().set(3));
            context.getInput().resizeWindow(1280, 720);
            context.runOnClient(client -> showCapacityBackpack(client, 4));
            context.waitForScreen(BackpackScreen.class);
            context.waitTicks(5);
            context.runOnClient(client -> {
                if (client.getWindow().getGuiScale() != 3) {
                    throw new AssertionError("Scrollable backpack must preserve the player's GUI scale");
                }
                BackpackScreen screen = (BackpackScreen) client.gui.screen();
                screen.mouseScrolled(100.0D, 50.0D, 0.0D, -1.0D);
                screen.mouseScrolled(100.0D, 50.0D, 0.0D, -1.0D);
                if (screen.firstVisibleRow() != 2) {
                    throw new AssertionError("Two wheel steps should reveal the final two capacity rows");
                }
                if (screen.getMenu().getSlot(0).isActive()
                        || !screen.getMenu().getSlot(18).isActive()
                        || screen.getMenu().getSlot(18).y != 18
                        || screen.getMenu().getSlot(63).y != 108) {
                    throw new AssertionError("Scrolled visual slots must retain their real menu indices");
                }
            });
            context.takeScreenshot("backpack-capacity-small-screen-scrolled");
            context.runOnClient(client -> {
                client.setScreenAndShow(null);
                if (client.getWindow().getGuiScale() != 3) {
                    throw new AssertionError("Backpack must not modify the player's GUI scale");
                }
                if (client.player != null) client.player.containerMenu = client.player.inventoryMenu;
            });
        }
    }

    private static ItemStack visualBackpack() {
        ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_ADVANCED);
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.DIAMOND, 12));
        contents.set(3, new ItemStack(Items.IRON_PICKAXE));
        contents.set(8, new ItemStack(Items.TORCH, 48));
        contents.set(10, new ItemStack(Items.BREAD, 16));
        contents.set(17, new ItemStack(Items.WATER_BUCKET));
        contents.set(22, new ItemStack(Items.OAK_LOG, 32));
        contents.set(26, new ItemStack(Items.EMERALD, 7));
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return backpack;
    }

    private static int[] panelSlotCenter(ClientGameTestContext context, int panelSlot) {
        return context.computeOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) client.gui.screen();
            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) client.player.inventoryMenu;
            var slot = client.player.inventoryMenu.getSlot(
                    access.totem$getBackpackPanelSlotStart() + panelSlot
            );
            int left = (screen.width - 176) / 2;
            int top = (screen.height - 166) / 2;
            double scale = client.getWindow().getGuiScale();
            return new int[]{
                    (int) Math.round((left + slot.x + 9) * scale),
                    (int) Math.round((top + slot.y + 9) * scale)
            };
        });
    }

    private static void selectLanguage(ClientGameTestContext context, String language) {
        AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
        context.runOnClient(client -> {
            client.options.languageCode = language;
            client.getLanguageManager().setSelected(language);
            reload.set(client.reloadResourcePacks());
        });
        context.waitFor(client -> reload.get() != null && reload.get().isDone());
    }

    private static void showCapacityBackpack(Minecraft client, int capacityModules) {
        int rows = 4 + capacityModules;
        BackpackMenu menu = BackpackMenu.clientSide(
                8 + capacityModules, client.player.getInventory(), rows, 4);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_PICKAXE));
        menu.getSlot(1).set(new ItemStack(Items.DIAMOND_AXE));
        menu.getSlot(2).set(new ItemStack(Items.TORCH, 64));
        menu.getSlot(9).set(new ItemStack(Items.OAK_LOG, 32));
        menu.getSlot(10).set(new ItemStack(Items.COBBLESTONE, 64));
        menu.getSlot(18).set(new ItemStack(Items.IRON_INGOT, 24));
        menu.getSlot(19).set(new ItemStack(Items.GOLD_INGOT, 12));
        menu.getSlot(27).set(new ItemStack(Items.BREAD, 16));
        menu.getSlot(28).set(new ItemStack(Items.WATER_BUCKET));

        ItemStack[][] addedRows = {
                stacks(Items.OAK_LOG, Items.COBBLESTONE, Items.RAW_IRON, Items.RAW_GOLD,
                        Items.COAL, Items.WHEAT, Items.BREAD, Items.EMERALD, Items.DIAMOND),
                stacks(Items.REDSTONE, Items.LAPIS_LAZULI, Items.QUARTZ, Items.COPPER_INGOT,
                        Items.IRON_INGOT, Items.GOLD_INGOT, Items.AMETHYST_SHARD,
                        Items.ENDER_PEARL, Items.BLAZE_ROD),
                stacks(Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS,
                        Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.SUGAR_CANE,
                        Items.CACTUS, Items.COCOA_BEANS),
                stacks(Items.NETHERRACK, Items.BLACKSTONE, Items.BASALT, Items.SOUL_SAND,
                        Items.END_STONE, Items.OBSIDIAN, Items.CRYING_OBSIDIAN,
                        Items.GLOWSTONE, Items.NETHERITE_SCRAP)
        };
        for (int row = 0; row < capacityModules; row++) {
            for (int column = 0; column < 9; column++) {
                menu.getSlot(36 + row * 9 + column).set(addedRows[row][column]);
            }
        }

        int upgradeStart = rows * 9 + 36;
        for (int module = 0; module < capacityModules; module++) {
            menu.getSlot(upgradeStart + module).set(
                    new ItemStack(RemnantItemRegistration.UPGRADE_CAPACITY));
        }
        client.player.containerMenu = menu;
        client.setScreenAndShow(new BackpackScreen(
                menu,
                client.player.getInventory(),
                Component.translatable("container.deadrecall.backpack.netherite")
        ));
    }

    private static ItemStack[] stacks(net.minecraft.world.item.Item... items) {
        ItemStack[] result = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) {
            result[index] = new ItemStack(items[index], index % 3 == 0 ? 32 : 16);
        }
        return result;
    }
}
