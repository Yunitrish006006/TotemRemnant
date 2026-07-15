package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.item.BackpackItemHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class DeathBackpackWorkstationInputGameTest {
    private static final BlockPos DEATH_POS = new BlockPos(2, 2, 2);

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void craftingTableInputsAreCaptured(GameTestHelper helper) {
        verifyWorkstationCapture(
                helper,
                CraftingMenu::new,
                List.of(
                        new InputExpectation(1, Items.DIAMOND, 2),
                        new InputExpectation(9, Items.EMERALD, 3)
                )
        );
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void anvilInputsAreCaptured(GameTestHelper helper) {
        verifyWorkstationCapture(
                helper,
                AnvilMenu::new,
                List.of(
                        new InputExpectation(0, Items.IRON_SWORD, 1),
                        new InputExpectation(1, Items.IRON_INGOT, 4)
                )
        );
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void smithingInputsAreCaptured(GameTestHelper helper) {
        verifyWorkstationCapture(
                helper,
                SmithingMenu::new,
                List.of(
                        new InputExpectation(0, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1),
                        new InputExpectation(1, Items.DIAMOND_SWORD, 1),
                        new InputExpectation(2, Items.NETHERITE_INGOT, 1)
                )
        );
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void grindstoneInputsAreCaptured(GameTestHelper helper) {
        verifyWorkstationCapture(
                helper,
                GrindstoneMenu::new,
                List.of(
                        new InputExpectation(0, Items.DIAMOND_SWORD, 1),
                        new InputExpectation(1, Items.DIAMOND_SWORD, 1)
                )
        );
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void stonecutterInputIsCaptured(GameTestHelper helper) {
        verifyWorkstationCapture(
                helper,
                StonecutterMenu::new,
                List.of(new InputExpectation(0, Items.STONE, 8))
        );
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void loomInputsAreCaptured(GameTestHelper helper) {
        verifyWorkstationCapture(
                helper,
                LoomMenu::new,
                List.of(
                        new InputExpectation(0, Items.DIAMOND, 1),
                        new InputExpectation(1, Items.EMERALD, 5)
                )
        );
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void cartographyInputsAreCaptured(GameTestHelper helper) {
        verifyWorkstationCapture(
                helper,
                CartographyTableMenu::new,
                List.of(
                        new InputExpectation(0, Items.MAP, 1),
                        new InputExpectation(1, Items.PAPER, 6)
                )
        );
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void enchantingInputsAreCaptured(GameTestHelper helper) {
        verifyWorkstationCapture(
                helper,
                EnchantmentMenu::new,
                List.of(
                        new InputExpectation(0, Items.DIAMOND_SWORD, 1),
                        new InputExpectation(1, Items.LAPIS_LAZULI, 12)
                )
        );
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void failedWorkstationCaptureReturnsInputsToVanillaDrops(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);
        CraftingMenu menu = new CraftingMenu(1, player.getInventory());
        player.containerMenu = menu;
        menu.getSlot(1).set(new ItemStack(Items.DIAMOND, 7));
        menu.getSlot(2).set(new ItemStack(Items.EMERALD, 9));

        DeathBackpackCaptureService.forceFailureForTesting(
                player.getUUID(),
                DeathBackpackCaptureService.CaptureFailurePoint.AFTER_ENTITY_ADD
        );
        player.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos);
                require(helper, deathBackpacks(drops).isEmpty(),
                        "Failed workstation capture left a death backpack in the world");
                requireLooseDrop(helper, drops, Items.DIAMOND, 7, "crafting-table diamond input");
                requireLooseDrop(helper, drops, Items.EMERALD, 9, "crafting-table emerald input");
                require(helper, menu.getSlot(1).getItem().isEmpty() && menu.getSlot(2).getItem().isEmpty(),
                        "Failed capture restored workstation inputs outside vanilla Inventory");
                require(helper, player.getInventory().isEmpty(),
                        "Vanilla dropAll did not clear workstation rollback stacks");
                helper.succeed();
            } finally {
                DeathBackpackCaptureService.clearForcedFailureForTesting(player.getUUID());
                player.discard();
            }
        });
    }

    private static void verifyWorkstationCapture(
            GameTestHelper helper,
            MenuFactory menuFactory,
            List<InputExpectation> inputs
    ) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);
        AbstractContainerMenu menu = menuFactory.create(1, player.getInventory());
        player.containerMenu = menu;

        for (InputExpectation input : inputs) {
            menu.getSlot(input.menuSlot()).set(new ItemStack(input.item(), input.count()));
        }
        player.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos);
                List<ItemEntity> deathBackpacks = deathBackpacks(drops);
                require(helper, deathBackpacks.size() == 1,
                        menu.getClass().getSimpleName() + " death expected one death backpack, found "
                                + deathBackpacks.size());

                List<ItemStack> stored = storedItems(deathBackpacks.getFirst().getItem());
                for (InputExpectation input : inputs) {
                    require(helper, stored.stream().anyMatch(stack ->
                                    stack.is(input.item()) && stack.getCount() == input.count()),
                            menu.getClass().getSimpleName() + " did not capture input slot " + input.menuSlot());
                    require(helper, menu.getSlot(input.menuSlot()).getItem().isEmpty(),
                            menu.getClass().getSimpleName() + " input slot " + input.menuSlot()
                                    + " remained populated after death");
                    require(helper, drops.stream().noneMatch(entity ->
                                    entity.getItem().is(input.item())
                                            && entity.getItem().getCount() == input.count()),
                            menu.getClass().getSimpleName() + " input leaked as a loose ItemEntity");
                }
                helper.succeed();
            } finally {
                player.discard();
            }
        });
    }

    private static ServerPlayer createPlayerAt(GameTestHelper helper, BlockPos absolutePos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(
                absolutePos.getX() + 0.5,
                absolutePos.getY(),
                absolutePos.getZ() + 0.5,
                0.0F,
                0.0F
        );
        return player;
    }

    private static List<ItemEntity> itemDropsAround(GameTestHelper helper, BlockPos absolutePos) {
        return helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(absolutePos).inflate(4.0),
                ItemEntity::isAlive
        );
    }

    private static List<ItemEntity> deathBackpacks(List<ItemEntity> drops) {
        return drops.stream()
                .filter(entity -> BackpackItemHelper.isDeathBackpackItem(entity.getItem()))
                .toList();
    }

    private static List<ItemStack> storedItems(ItemStack deathBackpack) {
        return deathBackpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream()
                .toList();
    }

    private static void requireLooseDrop(
            GameTestHelper helper,
            List<ItemEntity> drops,
            Item item,
            int count,
            String description
    ) {
        require(helper, drops.stream().anyMatch(entity ->
                        entity.getItem().is(item) && entity.getItem().getCount() == count),
                description + " was not restored to vanilla death drops");
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    @FunctionalInterface
    private interface MenuFactory {
        AbstractContainerMenu create(int containerId, Inventory inventory);
    }

    private record InputExpectation(int menuSlot, Item item, int count) {
    }
}
