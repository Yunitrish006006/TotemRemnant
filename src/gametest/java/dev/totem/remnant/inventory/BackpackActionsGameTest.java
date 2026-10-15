package dev.totem.remnant.inventory;

import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;

public final class BackpackActionsGameTest {
    @GameTest(maxTicks = 20)
    public void deathEquipPreservesOccupiedSlotsFullInventoryComponentsAndRepeat(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET); helmet.setDamageValue(17);
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS); boots.setDamageValue(23);
            ItemStack bag = bag(RemnantItemRegistration.DEATH_BACKPACK, List.of(helmet, boots,
                    new ItemStack(Items.ELYTRA), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIRT, 64)));
            p.setItemInHand(InteractionHand.MAIN_HAND, bag);
            for (int i = 9; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            ItemStack existing = new ItemStack(Items.IRON_HELMET);
            p.setItemSlot(EquipmentSlot.HEAD, existing);
            DeathBackpackInventory storage = new DeathBackpackInventory(p, InteractionHand.MAIN_HAND, 9);
            BackpackMenu menu = BackpackMenu.deathServerSide(7, p.getInventory(), storage, 1);
            p.containerMenu = menu;
            require(menu.clickMenuButton(p, BackpackMenu.EQUIP_BUTTON_ID), "Equip request rejected");
            require(p.getItemBySlot(EquipmentSlot.HEAD) == existing, "Existing helmet replaced");
            require(ItemStack.matches(p.getItemBySlot(EquipmentSlot.FEET), boots), "Boot data changed");
            require(p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA), "Stable candidate order failed");
            require(ItemStack.matches(storage.getItem(0), helmet) && storage.getItem(3).is(Items.DIAMOND_CHESTPLATE)
                    && storage.getItem(4).getCount() == 64, "Unequipped items lost");
            var persisted = bag.get(DataComponents.CONTAINER);
            require(!menu.clickMenuButton(p, BackpackMenu.EQUIP_BUTTON_ID), "Repeat equipped occupied slots");
            require(persisted.equals(bag.get(DataComponents.CONTAINER)), "Repeat mutated source");
            DeathBackpackInventory reopened = new DeathBackpackInventory(p, InteractionHand.MAIN_HAND, 9);
            require(ItemStack.matches(reopened.getItem(0), helmet) && reopened.getItem(1).isEmpty(), "Reopen lost equip state");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            require(!menu.clickMenuButton(p, BackpackMenu.EQUIP_BUTTON_ID), "Stale held backpack accepted");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void sortingPreservesHiddenContentsAndPinsOpenBackpack(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            List<ItemStack> contents = new ArrayList<>();
            contents.add(new ItemStack(Items.DIRT, 32)); contents.add(new ItemStack(Items.APPLE, 2));
            contents.add(new ItemStack(Items.DIRT, 40));
            while (contents.size() < 60) contents.add(ItemStack.EMPTY);
            contents.set(59, new ItemStack(Items.DIAMOND, 7));
            ItemStack bag = bag(RemnantItemRegistration.DEATH_BACKPACK, contents);
            p.getInventory().setItem(10, new ItemStack(Items.STONE, 3));
            p.setItemInHand(InteractionHand.OFF_HAND, bag);
            DeathBackpackInventory storage = new DeathBackpackInventory(p, InteractionHand.OFF_HAND, 54);
            BackpackMenu menu = BackpackMenu.deathServerSide(8, p.getInventory(), storage, 6);
            p.containerMenu = menu;
            require(BackpackActions.sort(p, false), "Storage sort rejected");
            require(storage.getItem(0).is(Items.APPLE) && storage.getItem(1).getCount() == 64
                    && storage.getItem(2).getCount() == 8, "Sort did not merge stably");
            require(bag.get(DataComponents.CONTAINER).itemCopies().toList().get(59).getCount() == 7, "Overflow lost");
            require(BackpackActions.sort(p, true), "Player sort rejected");
            require(p.getOffhandItem() == bag, "Open backpack moved/replaced");
            h.succeed();
        } finally { p.containerMenu = p.inventoryMenu; p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void panelSortPinsSelectionAndPreservesDistinctComponents(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            ItemStack named = new ItemStack(Items.DIRT, 3);
            named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keep"));
            ItemStack bag = bag(RemnantItemRegistration.BACKPACK_BASIC, List.of(new ItemStack(Items.DIRT, 32),
                    new ItemStack(Items.APPLE, 2), named, new ItemStack(Items.DIRT, 40)));
            p.getInventory().setItem(12, bag);
            p.getInventory().setItem(11, new ItemStack(Items.STONE, 2));
            BackpackPanelMenuAccess access = (BackpackPanelMenuAccess) p.inventoryMenu;
            access.totem$selectBackpackSlot(12);
            require(BackpackActions.sort(p, false), "Panel sort rejected");
            var stored = bag.get(DataComponents.CONTAINER).itemCopies().toList();
            require(stored.stream().filter(i -> i.is(Items.DIRT)).mapToInt(ItemStack::getCount).sum() == 75,
                    "Panel item count changed");
            require(stored.stream().anyMatch(i -> ItemStack.matches(i, named)), "Components were merged away");
            require(BackpackActions.sort(p, true), "Panel player sort rejected");
            require(p.getInventory().getItem(12) == bag && access.totem$getBackpackPanel().selectedInventorySlot() == 12,
                    "Selected backpack lost identity/position");
            h.succeed();
        } finally { p.discard(); }
    }

    @GameTest(maxTicks = 20)
    public void inventoryWithoutBackpackStillCompactsIntoEmptySlots(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        try {
            p.getInventory().setItem(20, new ItemStack(Items.APPLE, 2));
            require(BackpackActions.sort(p, true), "Empty-selection player sort rejected");
            require(p.getInventory().getItem(9).is(Items.APPLE) && p.getInventory().getItem(20).isEmpty(),
                    "Empty canonical stacks were treated as a pinned backpack");
            h.succeed();
        } finally { p.discard(); }
    }

    private static ItemStack bag(net.minecraft.world.item.Item item, List<ItemStack> contents) {
        ItemStack bag = new ItemStack(item); bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents)); return bag;
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
