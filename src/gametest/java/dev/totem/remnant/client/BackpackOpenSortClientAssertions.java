package dev.totem.remnant.client;

import dev.totem.remnant.client.screen.BackpackScreen;
import dev.totem.remnant.client.screen.BackpackSidebarLayout;
import dev.totem.remnant.inventory.BackpackMenu;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;

/** Open through the production right-click action and middle-click each storage/player region. */
final class BackpackOpenSortClientAssertions {
    static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        for (boolean death : new boolean[]{false, true}) for (int area = 0; area < 4; area++) {
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); p.getInventory().clearContent();
                p.inventoryMenu.setCarried(ItemStack.EMPTY); p.getInventory().setSelectedSlot(0);
                p.getInventory().setItem(0, bag(death));
                p.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 7));
                p.getInventory().setItem(10, new ItemStack(Items.COBBLESTONE, 11)); p.inventoryMenu.broadcastFullState();
            });
            context.waitFor(client -> ItemStack.matches(bag(death), client.player.getMainHandItem()));
            context.runOnClient(client -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND));
            context.waitForScreen(BackpackScreen.class);
            context.waitFor(client -> client.player.containerMenu.getSlot(0).hasItem());
            int region = area;
            int[] point = context.computeOnClient(client -> {
                var screen = (BackpackScreen) client.gui.screen(); var menu = screen.getMenu();
                var layout = (BackpackSidebarLayout) (Object) screen;
                require(!screen.containsStorage(layout.remnant$left() + BackpackMenu.UPGRADE_PANEL_X + 8,
                        layout.remnant$top() + 18), "Upgrade panel admitted as storage");
                require(!screen.containsStorage(layout.remnant$left() + BackpackMenu.CRAFTING_RESULT_X + 8,
                        layout.remnant$top() + BackpackMenu.CRAFTING_RESULT_Y + 8), "Crafting result admitted as storage");
                var slot = region == 3 ? menu.slots.stream().filter(s -> s.container == client.player.getInventory()
                        && s.getContainerSlot() == 9).findFirst().orElseThrow() : menu.getSlot(region == 1 ? 8 : 0);
                double x = layout.remnant$left() + slot.x + (region == 2 ? -5 : 8);
                double y = layout.remnant$top() + slot.y + 8;
                require(screen.containsStorage(x, y) == (region != 3), "Open backpack region hitbox mismatch");
                return new int[]{(int)Math.round(x * client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth()),
                        (int)Math.round(y * client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight())};
            });
            context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(2);
            context.runOnClient(client -> ObserverPacketProbe.reset()); context.getInput().pressMouse(2);
            context.waitFor(client -> region == 3 ? client.player.getInventory().getItem(9).is(Items.COBBLESTONE)
                    : client.player.containerMenu.getSlot(0).getItem().is(Items.APPLE));
            context.runOnClient(client -> require(ObserverPacketProbe.lastSort() != null
                    && ObserverPacketProbe.lastHoveredSort() == null, "Open backpack did not use region sort"));
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                require(p.containerMenu instanceof BackpackMenu, "Open menu unexpectedly closed");
                var root = p.getInventory().getItem(0);
                if (region == 3) {
                    require(ItemStack.matches(root, bag(death)), "Player-region sort changed open backpack");
                    require(p.getInventory().getItem(9).is(Items.COBBLESTONE) && p.getInventory().getItem(9).getCount() == 11
                            && p.getInventory().getItem(10).is(Items.IRON_INGOT) && p.getInventory().getItem(10).getCount() == 7,
                            "Open backpack player-region sort failed");
                } else {
                    var items = root.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().toList();
                    require(items.size() == (death ? 3 : 2) && items.get(0).is(Items.APPLE) && items.get(0).getCount() == 3
                            && items.get(1).is(Items.DIRT) && items.get(1).getCount() == 5
                            && (!death || ItemStack.matches(items.get(2), bag(false))), "Open storage sort lost contents or sorted captured bag");
                    require(p.getInventory().getItem(9).is(Items.IRON_INGOT) && p.getInventory().getItem(9).getCount() == 7
                            && p.getInventory().getItem(10).is(Items.COBBLESTONE) && p.getInventory().getItem(10).getCount() == 11,
                            "Open storage sort changed player inventory");
                }
            });
            context.takeScreenshot("backpack-open-sort-" + (death ? "death-" : "ordinary-") + area);
            context.runOnClient(client -> client.player.closeContainer()); context.waitForScreen(null);
            world.getServer().waitFor(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); return p.containerMenu == p.inventoryMenu;
            });
        }
    }
    private static ItemStack bag(boolean death) {
        var bag = new ItemStack(death ? RemnantItemRegistration.DEATH_BACKPACK : RemnantItemRegistration.BACKPACK_BASIC);
        var items = new ArrayList<ItemStack>();
        if (death) items.add(bag(false));
        items.addAll(List.of(new ItemStack(Items.DIRT, 2), new ItemStack(Items.APPLE, 3), new ItemStack(Items.DIRT, 3)));
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items)); return bag;
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
