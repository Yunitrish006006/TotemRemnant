package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.mixin.DeadRecallSpaceUnitSavedDataAccessor;
import com.adaptor.deadrecall.space.DeadRecallSpaceUnitSavedData;
import com.adaptor.deadrecall.space.SpaceUnitRecord;
import com.adaptor.deadrecall.space.SpaceUnitType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DeathBackpackTransientSlotGameTest {
    private static final BlockPos DEATH_POS = new BlockPos(2, 2, 2);
    private static final Component BASIC_BACKPACK_NAME = Component.literal("Cursor backpack");
    private static final Component EXISTING_DEATH_BACKPACK_NAME = Component.literal("Crafting death backpack");

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void capturesCursorAndCraftingGridWithoutTakingExternalContainerStorage(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);

        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, new ItemStack(Items.GOLD_INGOT, 9));
        ChestMenu chestMenu = ChestMenu.threeRows(1, player.getInventory(), chest);
        player.containerMenu = chestMenu;
        chestMenu.setCarried(new ItemStack(Items.DIAMOND, 3));
        player.inventoryMenu.getCraftSlots().setItem(0, new ItemStack(Items.EMERALD, 4));
        player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 5));

        player.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos);
                List<ItemEntity> deathBackpacks = deathBackpacks(drops);
                require(helper, deathBackpacks.size() == 1,
                        "Cursor/crafting death expected one death backpack, found " + deathBackpacks.size());

                List<ItemStack> stored = storedItems(deathBackpacks.getFirst().getItem());
                requireStored(helper, stored, Items.DIAMOND, 3, "cursor stack");
                requireStored(helper, stored, Items.EMERALD, 4, "2x2 crafting input");
                requireStored(helper, stored, Items.IRON_INGOT, 5, "inventory stack");
                require(helper, stored.stream().noneMatch(stack -> stack.is(Items.GOLD_INGOT)),
                        "External chest storage was incorrectly captured");
                require(helper, chest.getItem(0).is(Items.GOLD_INGOT) && chest.getItem(0).getCount() == 9,
                        "External chest storage changed during player death");
                require(helper, chestMenu.getCarried().isEmpty(),
                        "Captured cursor stack remained on the active menu");
                require(helper, player.inventoryMenu.getCraftSlots().isEmpty(),
                        "Captured 2x2 crafting input remained in the player menu");
                require(helper, drops.stream().noneMatch(entity ->
                                entity.getItem().is(Items.DIAMOND)
                                        || entity.getItem().is(Items.EMERALD)
                                        || entity.getItem().is(Items.IRON_INGOT)
                                        || entity.getItem().is(Items.GOLD_INGOT)),
                        "Captured or external-container stacks leaked into world drops");
                helper.succeed();
            } finally {
                player.discard();
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void transientBackpacksDropLooseAndVanishingStackIsDestroyed(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);

        ItemStack cursorBackpack = new ItemStack(ModItems.BACKPACK_BASIC);
        cursorBackpack.set(DataComponents.CUSTOM_NAME, BASIC_BACKPACK_NAME);
        player.containerMenu.setCarried(cursorBackpack);

        ItemStack existingDeathBackpack = new ItemStack(ModItems.DEATH_BACKPACK);
        existingDeathBackpack.set(DataComponents.CUSTOM_NAME, EXISTING_DEATH_BACKPACK_NAME);
        player.inventoryMenu.getCraftSlots().setItem(0, existingDeathBackpack);

        ItemStack vanishingSword = new ItemStack(Items.DIAMOND_SWORD);
        var vanishing = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.VANISHING_CURSE);
        vanishingSword.enchant(vanishing, 1);
        player.inventoryMenu.getCraftSlots().setItem(1, vanishingSword);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));

        player.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos);
                List<ItemEntity> deathBackpacks = deathBackpacks(drops);
                require(helper, deathBackpacks.size() == 2,
                        "Expected one generated and one existing death backpack drop");

                ItemEntity originalDeathBackpack = deathBackpacks.stream()
                        .filter(entity -> EXISTING_DEATH_BACKPACK_NAME.equals(
                                entity.getItem().get(DataComponents.CUSTOM_NAME)))
                        .findFirst()
                        .orElseThrow(() -> helper.assertionException(
                                "The existing crafting-slot death backpack was not dropped unchanged"));
                ItemEntity generatedDeathBackpack = deathBackpacks.stream()
                        .filter(entity -> entity != originalDeathBackpack)
                        .findFirst()
                        .orElseThrow(() -> helper.assertionException("Generated death backpack was missing"));

                List<ItemStack> stored = storedItems(generatedDeathBackpack.getItem());
                requireStored(helper, stored, Items.DIAMOND, 2, "normal inventory stack");
                require(helper, stored.stream().noneMatch(BackpackItemHelper::isBackpackItem),
                        "A transient backpack was nested inside the generated death backpack");
                require(helper, stored.stream().noneMatch(stack -> stack.is(Items.DIAMOND_SWORD)),
                        "A transient Curse of Vanishing item was captured");
                require(helper, drops.stream().anyMatch(entity ->
                                entity.getItem().is(ModItems.BACKPACK_BASIC)
                                        && BASIC_BACKPACK_NAME.equals(
                                        entity.getItem().get(DataComponents.CUSTOM_NAME))),
                        "The cursor backpack was not emitted as a loose world drop");
                require(helper, drops.stream().noneMatch(entity -> entity.getItem().is(Items.DIAMOND_SWORD)),
                        "The transient Curse of Vanishing item was emitted as a world drop");
                require(helper, player.containerMenu.getCarried().isEmpty(),
                        "The excluded cursor backpack remained on the menu after death");
                require(helper, player.inventoryMenu.getCraftSlots().isEmpty(),
                        "Excluded or vanishing crafting stacks remained after death");
                helper.succeed();
            } finally {
                player.discard();
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void failedCaptureReturnsTransientStacksToVanillaDrops(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);
        player.containerMenu.setCarried(new ItemStack(Items.DIAMOND, 6));
        player.inventoryMenu.getCraftSlots().setItem(0, new ItemStack(Items.EMERALD, 7));

        Set<UUID> deathNodesBefore = deathNodeIds(helper, player.getUUID());
        DeathBackpackCaptureService.forceFailureForTesting(
                player.getUUID(),
                DeathBackpackCaptureService.CaptureFailurePoint.AFTER_ENTITY_ADD
        );
        player.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos);
                require(helper, deathBackpacks(drops).isEmpty(),
                        "Failed transient capture left a death backpack in the world");
                require(helper, drops.stream().anyMatch(entity ->
                                entity.getItem().is(Items.DIAMOND)
                                        && entity.getItem().getCount() == 6),
                        "Cursor stack was not restored to vanilla death drops");
                require(helper, drops.stream().anyMatch(entity ->
                                entity.getItem().is(Items.EMERALD)
                                        && entity.getItem().getCount() == 7),
                        "Crafting input was not restored to vanilla death drops");
                require(helper, player.containerMenu.getCarried().isEmpty(),
                        "Failed capture restored the cursor outside vanilla Inventory");
                require(helper, player.inventoryMenu.getCraftSlots().isEmpty(),
                        "Failed capture restored crafting input outside vanilla Inventory");
                require(helper, player.getInventory().isEmpty(),
                        "Vanilla dropAll did not clear transient rollback stacks");
                require(helper, deathNodeIds(helper, player.getUUID()).equals(deathNodesBefore),
                        "Failed transient capture created a death Space Unit");
                helper.succeed();
            } finally {
                DeathBackpackCaptureService.clearForcedFailureForTesting(player.getUUID());
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

    private static void requireStored(
            GameTestHelper helper,
            List<ItemStack> stored,
            net.minecraft.world.item.Item item,
            int count,
            String description
    ) {
        require(helper, stored.stream().anyMatch(stack -> stack.is(item) && stack.getCount() == count),
                "Death backpack did not preserve " + description);
    }

    private static Set<UUID> deathNodeIds(GameTestHelper helper, UUID ownerId) {
        Set<UUID> ids = new HashSet<>();
        for (SpaceUnitRecord unit : unitRecords(helper).values()) {
            if (unit.type() == SpaceUnitType.DEATH && unit.owner().equals(ownerId)) {
                ids.add(unit.id());
            }
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, SpaceUnitRecord> unitRecords(GameTestHelper helper) {
        DeadRecallSpaceUnitSavedData data = helper.getLevel().getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);
        return ((DeadRecallSpaceUnitSavedDataAccessor) (Object) data).deadrecall$getUnitsById();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
