package dev.totem.remnant.inventory;

import dev.totem.remnant.network.SelectBackpackItemPayload;
import dev.totem.remnant.registry.RemnantDataComponents;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;

public final class BackpackItemInteractionGameTest {
    @GameTest(maxTicks = 20)
    public void nativeClicksDepositSelectWithdrawAndPickUpWholeBackpack(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack named = new ItemStack(Items.DIAMOND, 3);
            named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keep"));
            ItemStack bag = bag(List.of(new ItemStack(Items.IRON_INGOT, 4), named));
            p.getInventory().setItem(0, bag);
            p.inventoryMenu.setCarried(new ItemStack(Items.IRON_INGOT, 6));
            p.inventoryMenu.clicked(36, 0, ContainerInput.PICKUP, p);
            require(p.inventoryMenu.getCarried().isEmpty() && items(bag).getFirst().getCount() == 10, "Left-click did not deposit");
            var menu = p.inventoryMenu;
            require(SelectBackpackItemPayload.handle(p, new SelectBackpackItemPayload(menu.containerId,
                    menu.getStateId(), 36, 1, bag.copy())), "Valid selection rejected");
            menu.clicked(36, 1, ContainerInput.PICKUP, p);
            require(ItemStack.matches(named, menu.getCarried()) && items(bag).size() == 1
                    && items(bag).getFirst().is(Items.IRON_INGOT), "Selected withdrawal lost components/count");
            menu.setCarried(ItemStack.EMPTY);
            ItemStack expectedBag = bag.copy();
            menu.clicked(36, 0, ContainerInput.PICKUP, p);
            require(ItemStack.matches(menu.getCarried(), expectedBag) && p.getInventory().getItem(0).isEmpty(),
                    "Whole-backpack pickup broken");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void heldBackpackConsumesSourceAndRespectsDestinationStackLimit(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack bag = bag(List.of(new ItemStack(Items.IRON_INGOT, 30)));
            SimpleContainer chest = new SimpleContainer(9);
            chest.setItem(0, new ItemStack(Items.IRON_INGOT, 7));
            var menu = new ChestMenu(MenuType.GENERIC_9x1, 7, p.getInventory(), chest, 1);
            p.containerMenu = menu; menu.setCarried(bag);
            menu.clicked(0, 0, ContainerInput.PICKUP, p);
            require(chest.getItem(0).isEmpty() && items(bag).getFirst().getCount() == 37, "Held bag did not consume source");
            var limited = new net.minecraft.world.inventory.Slot(chest, 1, 0, 0) {
                @Override public int getMaxStackSize(ItemStack item) { return 16; }
            };
            limited.index = 1; menu.slots.set(1, limited);
            menu.clicked(1, 1, ContainerInput.PICKUP, p);
            require(chest.getItem(1).getCount() == 16 && items(bag).getFirst().getCount() == 21,
                    "Limited target lost remainder");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void partialCapacityKeepsCursorRemainderAndHiddenContents(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        try {
            var content = new ArrayList<ItemStack>(); content.add(new ItemStack(Items.IRON_INGOT, 62));
            for (int i = 1; i < 9; i++) content.add(new ItemStack(Items.STONE, 64));
            content.add(new ItemStack(Items.DIAMOND, 7));
            ItemStack bag = bag(content); p.getInventory().setItem(0, bag);
            p.inventoryMenu.setCarried(new ItemStack(Items.IRON_INGOT, 10));
            p.inventoryMenu.clicked(36, 0, ContainerInput.PICKUP, p);
            require(p.inventoryMenu.getCarried().getCount() == 8 && items(bag).getFirst().getCount() == 64
                    && items(bag).get(9).getCount() == 7, "Capacity/overflow conservation failed");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void selectionRejectsStaleMenusStacksAndEmptyIndices(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack bag = bag(List.of(new ItemStack(Items.DIAMOND, 5))); p.getInventory().setItem(0, bag);
            var m = p.inventoryMenu;
            for (var request : List.of(
                    new SelectBackpackItemPayload(99, m.getStateId(), 36, 0, bag.copy()),
                    new SelectBackpackItemPayload(m.containerId, m.getStateId() + 1, 36, 0, bag.copy()),
                    new SelectBackpackItemPayload(m.containerId, m.getStateId(), 36, 8, bag.copy()),
                    new SelectBackpackItemPayload(m.containerId, m.getStateId(), 36, 0, ItemStack.EMPTY)))
                require(!SelectBackpackItemPayload.handle(p, request), "Invalid selection accepted");
            require(!bag.has(RemnantDataComponents.SELECTED_ITEM) && items(bag).getFirst().getCount() == 5,
                    "Rejected request changed inventory");
            // GameTestHelper's mock overrides gameMode() to CREATIVE, even after
            // setGameMode. Use a real ServerPlayer with a spectator fixture mode.
            var spectator = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "BundleSpectator"),
                    net.minecraft.server.level.ClientInformation.createDefault()) {
                @Override public net.minecraft.world.level.GameType gameMode() {
                    return net.minecraft.world.level.GameType.SPECTATOR;
                }
            };
            ItemStack spectatorBag = bag.copy(); spectator.getInventory().setItem(0, spectatorBag);
            var spectatorMenu = spectator.inventoryMenu;
            require(spectator.isSpectator(), "Spectator fixture mode invalid");
            require(!SelectBackpackItemPayload.handle(spectator, new SelectBackpackItemPayload(spectatorMenu.containerId,
                    spectatorMenu.getStateId(), 36, 0, spectatorBag.copy()))
                    && !spectatorBag.has(RemnantDataComponents.SELECTED_ITEM),
                    "Spectator selection mutated backpack");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void rejectedCursorAndThrowingSlotRestoreContentsAndSelection(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack bag = bag(List.of(new ItemStack(Items.DIAMOND, 5))); p.getInventory().setItem(0, bag);
            BackpackItemInteraction.select(bag, 0);
            require(BackpackItemInteraction.onMe(bag, ItemStack.EMPTY, p.inventoryMenu.getSlot(36),
                    net.minecraft.world.inventory.ClickAction.SECONDARY, p, new net.minecraft.world.entity.SlotAccess() {
                        public ItemStack get() { return ItemStack.EMPTY; }
                        public boolean set(ItemStack value) { return false; }
                    }), "Rejection not consumed");
            require(items(bag).getFirst().getCount() == 5 && bag.get(RemnantDataComponents.SELECTED_ITEM) == 0,
                    "Rejected cursor ate selected stack");
            class Throwing extends SimpleContainer {
                boolean armed;
                Throwing() { super(9); }
                @Override public void setChanged() { if (armed) throw new IllegalStateException("callback failure"); }
            }
            Throwing chest = new Throwing(); chest.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
            p.getInventory().setItem(0, ItemStack.EMPTY);
            var menu = new ChestMenu(MenuType.GENERIC_9x1, 8, p.getInventory(), chest, 1);
            p.containerMenu = menu; menu.setCarried(bag); chest.armed = true;
            require(BackpackItemInteraction.onOther(bag, menu.getSlot(0),
                    net.minecraft.world.inventory.ClickAction.PRIMARY, p), "Failure action not consumed");
            require(chest.getItem(0).getCount() == 4 && items(bag).getFirst().is(Items.DIAMOND)
                    && items(bag).stream().noneMatch(i -> i.is(Items.IRON_INGOT)), "Callback failure lost/duplicated items");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void nestingDeniedAndSelectedPanelRefreshesBeforeNextWrite(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack bag = bag(List.of(new ItemStack(Items.IRON_INGOT, 4))); p.getInventory().setItem(0, bag);
            var panel = ((BackpackPanelMenuAccess) p.inventoryMenu).totem$getBackpackPanel(); panel.select(0); panel.getItem(0);
            ItemStack nested = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            p.inventoryMenu.setCarried(nested); p.inventoryMenu.clicked(36, 0, ContainerInput.PICKUP, p);
            require(p.inventoryMenu.getCarried() == nested && items(bag).size() == 1, "Backpack nesting accepted");
            p.inventoryMenu.setCarried(new ItemStack(Items.IRON_INGOT, 6));
            p.inventoryMenu.clicked(36, 0, ContainerInput.PICKUP, p);
            require(panel.getItem(0).getCount() == 10, "Same-tick panel was stale");
            panel.removeItem(0, 1);
            require(items(bag).getFirst().getCount() == 9, "Later panel write erased deposit");
            h.succeed();
        } finally { p.discard(); }
    }

    private static ItemStack bag(List<ItemStack> content) {
        var bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(content)); return bag;
    }
    private static List<ItemStack> items(ItemStack bag) { return bag.get(DataComponents.CONTAINER).itemCopies().toList(); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
