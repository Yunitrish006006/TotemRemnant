package dev.totem.remnant.inventory;

import dev.totem.remnant.network.BackpackPanelPayloadRegistration;
import dev.totem.remnant.network.BackpackSortPayload;
import dev.totem.remnant.registry.RemnantItemRegistration;
import dev.totem.remnant.upgrade.BackpackUpgradeData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;

public final class MatchingBackpackSortGameTest {
    @GameTest(maxTicks = 20)
    public void chestDepositsAcrossBagsInOrderWithoutTouchingPlayerSide(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            SimpleContainer chest = new SimpleContainer(9);
            ItemStack first = fullBag(60), second = bag(List.of(new ItemStack(Items.IRON_INGOT, 10)));
            chest.setItem(0, first); chest.setItem(1, second);
            chest.setItem(2, new ItemStack(Items.IRON_INGOT, 64)); chest.setItem(3, new ItemStack(Items.IRON_INGOT, 6));
            chest.setItem(4, new ItemStack(Items.APPLE, 2));
            p.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 11));
            p.containerMenu = new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, 7, p.getInventory(), chest, 1);
            require(BackpackActions.sort(p, false), "Chest matching sort rejected");
            require(chest.getItem(0) == first && chest.getItem(1) == second, "Destination bag identity/slot changed");
            require(iron(first) == 64 && iron(second) == 76, "Multi-bag order/counts incorrect");
            require(chest.getItem(2).is(Items.APPLE) && chest.getItem(2).getCount() == 2, "Remainder not sorted");
            require(p.getInventory().getItem(9).getCount() == 11, "Chest action drained player side");
            var persisted = second.get(DataComponents.CONTAINER);
            require(BackpackActions.sort(p, false) && persisted.equals(second.get(DataComponents.CONTAINER)), "Repeat changed bags");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void fullCapacityLeavesRemainderAndPreservesOverflow(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack bag = fullBag(62);
            List<ItemStack> withOverflow = new ArrayList<>(bag.get(DataComponents.CONTAINER).itemCopies().toList());
            withOverflow.add(new ItemStack(Items.DIAMOND, 7));
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(withOverflow));
            SimpleContainer chest = new SimpleContainer(9);
            chest.setItem(0, bag); chest.setItem(1, new ItemStack(Items.IRON_INGOT, 10));
            p.containerMenu = new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, 8, p.getInventory(), chest, 1);
            require(BackpackActions.sort(p, false), "Partial-capacity sort rejected");
            require(iron(bag) == 64 && chest.getItem(1).getCount() == 8, "Capacity remainder lost");
            require(bag.get(DataComponents.CONTAINER).itemCopies().toList().get(9).getCount() == 7, "Hidden contents lost");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void selectedHotbarBagRefreshesSameTickAndKeepsDistinctComponents(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack named = new ItemStack(Items.IRON_INGOT, 3);
            named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keep"));
            ItemStack bag = bag(List.of(named));
            BackpackUpgradeData.write(bag, List.of(new ItemStack(RemnantItemRegistration.UPGRADE_COMPACTION)), 1);
            p.getInventory().setItem(0, bag);
            p.getInventory().setItem(9, named.copyWithCount(5));
            p.getInventory().setItem(10, new ItemStack(Items.IRON_INGOT, 9));
            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) p.inventoryMenu;
            access.totem$selectBackpackSlot(0);
            int panel = access.totem$getBackpackPanelSlotStart();
            p.inventoryMenu.getSlot(panel).getItem();
            require(BackpackActions.sort(p, true), "Player matching sort rejected");
            require(p.getInventory().getItem(0) == bag, "Hotbar bag moved/replaced");
            require(ItemStack.matches(p.inventoryMenu.getSlot(panel).getItem(), named.copyWithCount(8)), "Same-tick panel stale or components lost");
            require(p.getInventory().getItem(9).is(Items.IRON_INGOT) && p.getInventory().getItem(9).getCount() == 9,
                    "Different components were deposited");
            access.totem$getBackpackPanel().removeItem(0, 1);
            require(ItemStack.matches(bag.get(DataComponents.CONTAINER).itemCopies().toList().get(0), named.copyWithCount(7)),
                    "Subsequent panel write lost newly deposited items");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void compressionStagesBeforeCommitAndBoundRequestsRejectStaleState(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack bag = bag(List.of(new ItemStack(Items.IRON_INGOT, 8)));
            BackpackUpgradeData.write(bag, List.of(new ItemStack(RemnantItemRegistration.UPGRADE_COMPACTION)), 1);
            SimpleContainer chest = new SimpleContainer(9);
            chest.setItem(0, bag); chest.setItem(1, new ItemStack(Items.IRON_INGOT));
            p.containerMenu = new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, 9, p.getInventory(), chest, 1);
            var original = bag.get(DataComponents.CONTAINER);
            var menu = p.containerMenu;
            require(!BackpackPanelPayloadRegistration.handleSort(p, new BackpackSortPayload(9, menu.getStateId() + 1, 1, -1, ItemStack.EMPTY)),
                    "Stale state accepted");
            require(original.equals(bag.get(DataComponents.CONTAINER)) && chest.getItem(1).getCount() == 1, "Rejected request mutated items");
            require(BackpackPanelPayloadRegistration.handleSort(p, new BackpackSortPayload(9, menu.getStateId(), 1, -1, ItemStack.EMPTY)),
                    "Bound chest request rejected");
            require(bag.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().anyMatch(item -> item.is(Items.IRON_BLOCK) && item.getCount() == 1)
                    && iron(bag) == 0 && chest.getItem(1).isEmpty(), "Compression did not conserve nine iron units");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void failureAfterFirstBagCommitRollsBackEverySourceAndDestination(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            class FailingChest extends SimpleContainer {
                boolean armed, triggered;
                ItemStack firstBag, secondBag;
                FailingChest() { super(9); }
                @Override public void setChanged() {
                    super.setChanged();
                    if (armed && iron(firstBag) == 64 && iron(secondBag) == 26) {
                        armed = false; triggered = true;
                        throw new IllegalStateException("Injected second-bag commit failure");
                    }
                }
            }
            FailingChest chest = new FailingChest();
            ItemStack first = fullBag(60), second = bag(List.of(new ItemStack(Items.IRON_INGOT, 10)));
            chest.setItem(0, first); chest.setItem(1, second); chest.setItem(2, new ItemStack(Items.IRON_INGOT, 20));
            List<ItemStack> before = new ArrayList<>(); for (int i = 0; i < 9; i++) before.add(chest.getItem(i).copy());
            p.containerMenu = new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, 10, p.getInventory(), chest, 1);
            chest.firstBag = first; chest.secondBag = second; chest.armed = true;
            require(!BackpackActions.sort(p, false) && chest.triggered, "Injected second-bag commit did not fail");
            for (int i = 0; i < 9; i++) require(ItemStack.matches(before.get(i), chest.getItem(i)), "Rollback mismatch at " + i);
            require(chest.getItem(0) == first && chest.getItem(1) == second, "Rollback replaced bag objects");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void persistentCallbacksStillRestoreEverySlotAndPinDeathBags(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            class FailingChest extends SimpleContainer {
                boolean armed;
                int writes;
                final IllegalStateException persistentFailure = new IllegalStateException("Persistent listener failure");
                FailingChest() { super(9); }
                @Override public void setChanged() {
                    super.setChanged();
                    if (armed && ++writes >= 3) throw persistentFailure;
                }
            }
            FailingChest chest = new FailingChest();
            ItemStack bag = bag(List.of(new ItemStack(Items.IRON_INGOT, 8)));
            ItemStack death = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
            chest.setItem(0, bag); chest.setItem(1, new ItemStack(Items.IRON_INGOT, 10));
            chest.setItem(4, new ItemStack(Items.APPLE, 4)); chest.setItem(8, death);
            List<ItemStack> before = new ArrayList<>(); for (int i = 0; i < 9; i++) before.add(chest.getItem(i).copy());
            p.containerMenu = new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, 11, p.getInventory(), chest, 1); chest.armed = true;
            require(!BackpackActions.sort(p, false), "Persistent callback failure accepted");
            for (int i = 0; i < 9; i++) require(ItemStack.matches(before.get(i), chest.getItem(i)), "Persistent rollback mismatch at " + i);
            require(chest.getItem(8) == death && chest.getItem(0) == bag, "Rollback replaced backpacks");
            chest.armed = false;
            require(BackpackActions.sort(p, false) && chest.getItem(8) == death, "Unopened death bag moved");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void unknownMenusAndStalePanelSelectionCannotMutateInventory(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            p.getInventory().setItem(20, new ItemStack(Items.APPLE, 3));
            p.containerMenu = new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1,
                    12, p.getInventory(), new SimpleContainer(9), 1) {};
            require(!MatchingBackpackSort.sort(p, true, ItemStack.EMPTY), "Unknown menu subclass accepted");
            require(p.getInventory().getItem(20).getCount() == 3, "Unknown menu mutated player inventory");
            p.containerMenu = p.inventoryMenu;
            ItemStack bag = bag(List.of(new ItemStack(Items.IRON_INGOT, 8)));
            p.getInventory().setItem(0, bag);
            p.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT));
            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) p.inventoryMenu;
            access.totem$selectBackpackSlot(0);
            var request = new BackpackSortPayload(p.inventoryMenu.containerId, p.inventoryMenu.getStateId(), 9, 1, bag.copy());
            require(!BackpackPanelPayloadRegistration.handleSort(p, request), "Stale panel selection accepted");
            require(iron(bag) == 8 && p.getInventory().getItem(9).getCount() == 1, "Stale selection mutated inventory");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void openBackpackViewRefreshesBeforeNextStorageWrite(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack bag = bag(List.of(new ItemStack(Items.IRON_INGOT, 8)));
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bag);
            p.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 2));
            var storage = new DeathBackpackInventory(p, net.minecraft.world.InteractionHand.MAIN_HAND, 9);
            var upgrades = new BackpackUpgradeInventory(p, net.minecraft.world.InteractionHand.MAIN_HAND, 1);
            BackpackMenu menu = BackpackMenu.serverSide(13, p.getInventory(), storage, upgrades, 1);
            p.containerMenu = menu;
            require(BackpackActions.sort(p, true) && storage.getItem(0).getCount() == 10, "Open storage remained stale");
            require(storage.removeItem(0, 1).getCount() == 1 && iron(bag) == 9,
                    "Next open-storage write lost the deposited items");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    private static ItemStack fullBag(int iron) {
        List<ItemStack> contents = new ArrayList<>(); contents.add(new ItemStack(Items.IRON_INGOT, iron));
        for (int i = 1; i < 9; i++) contents.add(new ItemStack(Items.STONE, 64)); return bag(contents);
    }
    private static ItemStack bag(List<ItemStack> items) {
        ItemStack bag = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items)); return bag;
    }
    private static int iron(ItemStack bag) {
        return bag.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().filter(item -> item.is(Items.IRON_INGOT)).mapToInt(ItemStack::getCount).sum();
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
