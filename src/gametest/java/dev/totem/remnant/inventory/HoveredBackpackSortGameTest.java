package dev.totem.remnant.inventory;

import dev.totem.remnant.network.SortHoveredBackpackPayload;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.minecraft.core.component.DataComponents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;

public final class HoveredBackpackSortGameTest {
    @GameTest(maxTicks = 20)
    public void hoveredInventoryBagSortsContentsAndPreservesOverflow(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            var bag = bag(); var items = new ArrayList<>(bag.get(DataComponents.CONTAINER).itemCopies().toList());
            while (items.size() < 11) items.add(ItemStack.EMPTY);
            items.set(10, new ItemStack(Items.DIAMOND, 7)); bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            p.getInventory().setItem(0, bag); var loose = new ItemStack(Items.DIRT, 19); p.getInventory().setItem(9, loose);
            ((BackpackPanelMenuAccess) p.inventoryMenu).totem$selectBackpackSlot(0);
            require(request(p, 36), "Hovered inventory backpack rejected");
            checkSorted(bag); require(p.getInventory().getItem(0) == bag && p.getInventory().getItem(9) == loose && loose.getCount() == 19,
                    "Hovered sort moved bag or deposited loose inventory items");
            require(bag.get(DataComponents.CONTAINER).itemCopies().toList().get(10).getCount() == 7, "Hidden overflow lost");
            require(((BackpackPanelMenuAccess) p.inventoryMenu).totem$getBackpackPanel().getItem(0).is(Items.APPLE), "Open panel stayed stale");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void hoveredChestBagSortsOnlyItselfAndRollsBackListenerFailure(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            var fail = new java.util.concurrent.atomic.AtomicBoolean();
            var chest = new SimpleContainer(9) { @Override public void setChanged() {
                if (fail.get()) throw new IllegalStateException("fixture listener"); super.setChanged();
            }};
            var bag = bag(); var original = bag.copy(); var loose = new ItemStack(Items.DIRT, 19);
            chest.setItem(2, bag); chest.setItem(3, loose);
            p.containerMenu = new ChestMenu(MenuType.GENERIC_9x1, 9, p.getInventory(), chest, 1);
            fail.set(true); require(!request(p, 2) && ItemStack.matches(bag, original), "Failed callback did not restore exact bag");
            fail.set(false); require(request(p, 2), "Hovered chest backpack rejected"); checkSorted(bag);
            require(chest.getItem(2) == bag && chest.getItem(3) == loose && loose.getCount() == 19, "Chest surface was sorted/deposited");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void capturedBackpackSortPersistsInsideDeathSidebar(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            var root = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
            root.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(bag(), new ItemStack(Items.DIAMOND, 7))));
            p.getInventory().setItem(0, root); var access = (BackpackPanelMenuAccess) p.inventoryMenu; access.totem$selectBackpackSlot(0);
            require(request(p, access.totem$getBackpackPanelSlotStart()), "Captured backpack sort rejected");
            var persisted = root.get(DataComponents.CONTAINER).itemCopies().toList();
            checkSorted(persisted.getFirst()); require(persisted.get(1).getCount() == 7, "Outer death storage changed");
            access.totem$getBackpackPanel().refreshAfterExternalCommit(); checkSorted(access.totem$getBackpackPanel().getItem(0));
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void staleAndForeignRequestsDoNotSort(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            var bag = bag(); var original = bag.copy(); p.getInventory().setItem(0, bag); var m = p.containerMenu;
            for (var request : List.of(new SortHoveredBackpackPayload(m.containerId + 1, m.getStateId(), 36, bag.copy()),
                    new SortHoveredBackpackPayload(m.containerId, m.getStateId() + 1, 36, bag.copy()),
                    new SortHoveredBackpackPayload(m.containerId, m.getStateId(), -1, bag.copy()),
                    new SortHoveredBackpackPayload(m.containerId, m.getStateId(), m.slots.size(), bag.copy()),
                    new SortHoveredBackpackPayload(m.containerId, m.getStateId(), 36, ItemStack.EMPTY)))
                require(!SortHoveredBackpackPayload.handle(p, request), "Invalid request accepted");
            require(ItemStack.matches(original, bag), "Rejected request mutated backpack");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }
    private static boolean request(ServerPlayer p, int index) {
        var m = p.containerMenu; return SortHoveredBackpackPayload.handle(p, SortHoveredBackpackPayload.forSlot(m, m.getSlot(index)));
    }
    @GameTest(maxTicks = 20)
    public void changedOuterSelectionRejectsIdenticalInnerBackpack(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            var first = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
            first.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(bag())));
            var second = first.copy(); p.getInventory().setItem(0, first); p.getInventory().setItem(1, second);
            var access = (BackpackPanelMenuAccess) p.inventoryMenu; access.totem$selectBackpackSlot(0);
            int source = access.totem$getBackpackPanelSlotStart();
            var old = SortHoveredBackpackPayload.forSlot(p.inventoryMenu, p.inventoryMenu.getSlot(source));
            var beforeFirst = first.copy(); var beforeSecond = second.copy(); int state = p.inventoryMenu.getStateId();
            access.totem$selectBackpackSlot(1);
            require(p.inventoryMenu.getStateId() == state, "Fixture must switch roots without changing stateId");
            require(!SortHoveredBackpackPayload.handle(p, old), "Stale outer selection sorted the other inner backpack");
            require(ItemStack.matches(first, beforeFirst) && ItemStack.matches(second, beforeSecond), "Rejected request changed roots");
            var metadataStale = SortHoveredBackpackPayload.forSlot(p.inventoryMenu, p.inventoryMenu.getSlot(source));
            second.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Changed root"));
            require(!SortHoveredBackpackPayload.handle(p, metadataStale), "Changed outer stack metadata accepted");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }
    @GameTest(maxTicks = 20)
    public void functionalOutputAndSpectatorRequestsAreRejected(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            p.containerMenu = BackpackSidebarAdapters.CRAFTING.createMenu(11, p.getInventory());
            var bag = bag(); var before = bag.copy(); p.containerMenu.getSlot(0).set(bag);
            require(!request(p, 0) && ItemStack.matches(bag, before), "Functional result slot accepted");
            var spectator = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "HoveredSortSpectator"),
                    net.minecraft.server.level.ClientInformation.createDefault()) {
                @Override public net.minecraft.world.level.GameType gameMode() { return net.minecraft.world.level.GameType.SPECTATOR; }
            };
            spectator.getInventory().setItem(0, bag.copy());
            require(spectator.isSpectator() && !request(spectator, 36)
                    && ItemStack.matches(spectator.getInventory().getItem(0), before), "Spectator sorted storage");
            spectator.discard(); h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }
    private static ItemStack bag() {
        var bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                new ItemStack(Items.DIRT, 32), new ItemStack(Items.APPLE, 2), new ItemStack(Items.DIRT, 40)))); return bag;
    }
    private static void checkSorted(ItemStack bag) {
        var items = bag.get(DataComponents.CONTAINER).itemCopies().toList();
        require(items.get(0).is(Items.APPLE) && items.get(0).getCount() == 2
                && items.get(1).is(Items.DIRT) && items.get(1).getCount() == 64
                && items.get(2).is(Items.DIRT) && items.get(2).getCount() == 8, "Internal sort lost or failed to merge items");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
