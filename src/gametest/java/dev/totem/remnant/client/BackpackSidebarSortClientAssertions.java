package dev.totem.remnant.client;

import dev.totem.remnant.client.integration.RemnantSortClientProvider;
import dev.totem.remnant.client.screen.BackpackSidebarLayout;
import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.List;

/** Actual pointer/packet/persistence checks for sidebar slots and non-slot panel regions. */
final class BackpackSidebarSortClientAssertions {
    static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        for (boolean crafting : new boolean[]{false, true}) {
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.getInventory().clearContent(); p.inventoryMenu.setCarried(ItemStack.EMPTY);
                p.getInventory().setItem(0, bag());
                p.getInventory().setItem(9, new ItemStack(Items.DIRT, 11));
                p.getInventory().setItem(10, new ItemStack(Items.IRON_INGOT, 7));
                if (crafting) {
                    p.level().setBlockAndUpdate(p.blockPosition(), net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState());
                    p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, ignored) ->
                            new net.minecraft.world.inventory.CraftingMenu(id, inventory,
                                    net.minecraft.world.inventory.ContainerLevelAccess.create(p.level(), p.blockPosition())),
                            Component.literal("Sidebar sort")));
                } else p.inventoryMenu.broadcastFullState();
            });
            if (crafting) context.waitForScreen(net.minecraft.client.gui.screens.inventory.CraftingScreen.class);
            else {
                context.waitFor(client -> client.player.getInventory().getItem(0).is(RemnantItemRegistration.BACKPACK_BASIC));
                context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
            }
            context.waitFor(client -> ((BackpackPanelMenuAccess) client.player.containerMenu)
                    .totem$getBackpackPanel().selectedInventorySlot() == 0);
            context.waitTicks(3);
            for (int area = 0; area < 5; area++) {
                world.getServer().runOnServer(server -> {
                    var p = server.getPlayerList().getPlayers().getFirst();
                    p.getInventory().setItem(0, bag()); p.containerMenu.broadcastFullState();
                });
                context.waitFor(client -> ItemStack.matches(bag(), client.player.getInventory().getItem(0)));
                int region = area;
                int[] point = context.computeOnClient(client -> {
                    var screen = (AbstractContainerScreen<?>) client.gui.screen();
                    var access = (BackpackPanelMenuAccess) screen.getMenu(); var layout = (BackpackSidebarLayout) screen;
                    var slot = screen.getMenu().getSlot(access.totem$getBackpackPanelSlotStart() + (region == 1 ? 8 : 0));
                    double x = layout.remnant$left() + slot.x + (region == 3 ? -5 : 8);
                    double y = layout.remnant$top() + slot.y + (region == 2 ? -10 : 8);
                    require(layout.remnant$contains(x, y), "Sort point outside sidebar");
                    if (region == 2 || region == 3) require(layout.remnant$hoveredSlot(x, y) == null, "Frame point overlapped a slot");
                    return new int[]{(int)Math.round(x * client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth()),
                            (int)Math.round(y * client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight())};
                });
                context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(2);
                context.runOnClient(client -> ObserverPacketProbe.reset());
                if (region == 3) context.runOnClient(client -> {
                    var screen = (AbstractContainerScreen<?>) client.gui.screen();
                    var stale = screen.getMenu().slots.stream().filter(s -> s.container == client.player.getInventory()
                            && s.getContainerSlot() == 9).findFirst().orElseThrow();
                    require(new RemnantSortClientProvider().apply(screen, stale), "Shared shortcut ignored sidebar frame");
                });
                else if (region == 4) {
                    // The dispatched event is authoritative even if MouseHandler still reports
                    // the player inventory position (queued/forwarded input).
                    int[] playerPoint = context.computeOnClient(client -> {
                        var screen = (AbstractContainerScreen<?>) client.gui.screen();
                        var layout = (BackpackSidebarLayout) screen;
                        var slot = screen.getMenu().slots.stream().filter(s -> s.container == client.player.getInventory()
                                && s.getContainerSlot() == 9).findFirst().orElseThrow();
                        return new int[]{(int)Math.round((layout.remnant$left() + slot.x + 8.0) * client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth()),
                                (int)Math.round((layout.remnant$top() + slot.y + 8.0) * client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight())};
                    });
                    context.getInput().setCursorPos(playerPoint[0], playerPoint[1]); context.waitTicks(2);
                    context.runOnClient(client -> {
                        var screen = (AbstractContainerScreen<?>) client.gui.screen();
                        var layout = (BackpackSidebarLayout) screen;
                        var access = (BackpackPanelMenuAccess) screen.getMenu();
                        var slot = screen.getMenu().getSlot(access.totem$getBackpackPanelSlotStart());
                        ObserverPacketProbe.reset();
                        screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(
                                layout.remnant$left() + slot.x + 8.0, layout.remnant$top() + slot.y + 8.0,
                                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE, 0)), false);
                        var request = ObserverPacketProbe.lastSort();
                        require(request != null && screen.getMenu().getSlot(request.hoveredSlot())
                                instanceof dev.totem.remnant.inventory.BackpackPanelSlot,
                                "Sidebar event coordinates routed sort to player inventory");
                    });
                } else context.getInput().pressMouse(2);
                context.waitFor(client -> sorted(client.player.getInventory().getItem(0)));
                context.runOnClient(client -> require(ObserverPacketProbe.lastSort() != null
                        && ObserverPacketProbe.lastHoveredSort() == null, "Sidebar did not target selected backpack"));
                world.getServer().runOnServer(server -> {
                    var p = server.getPlayerList().getPlayers().getFirst();
                    require(sorted(p.getInventory().getItem(0)), "Sidebar sort was not persisted");
                    require(p.getInventory().getItem(9).is(Items.DIRT) && p.getInventory().getItem(9).getCount() == 11
                            && p.getInventory().getItem(10).getCount() == 7, "Sidebar sort changed player inventory");
                });
                context.takeScreenshot("backpack-sidebar-sort-" + (crafting ? "crafting-" : "inventory-") + area);
            }
            if (crafting) {
                context.runOnClient(client -> client.player.closeContainer()); context.waitForScreen(null);
                world.getServer().waitFor(server -> {
                    var p = server.getPlayerList().getPlayers().getFirst(); return p.containerMenu == p.inventoryMenu;
                });
            } else { context.runOnClient(client -> client.setScreenAndShow(null)); context.waitForScreen(null); }
        }
    }
    private static ItemStack bag() {
        var bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                new ItemStack(Items.DIRT, 2), new ItemStack(Items.APPLE, 3), new ItemStack(Items.DIRT, 3))));
        return bag;
    }
    private static boolean sorted(ItemStack bag) {
        if (!bag.is(RemnantItemRegistration.BACKPACK_BASIC)) return false;
        var items = bag.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().toList();
        return items.size() == 2 && items.get(0).is(Items.APPLE) && items.get(0).getCount() == 3
                && items.get(1).is(Items.DIRT) && items.get(1).getCount() == 5;
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
