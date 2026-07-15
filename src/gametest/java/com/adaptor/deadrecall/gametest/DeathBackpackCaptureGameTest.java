package com.adaptor.deadrecall.gametest;

import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class DeathBackpackCaptureGameTest {
    private static final BlockPos DEATH_POS = new BlockPos(2, 2, 2);
    private static final Component DIAMOND_NAME = Component.literal("DeadRecall direct capture");

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void capturesInventoryBeforeVanillaCreatesWorldDrops(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);

        ItemStack diamonds = new ItemStack(Items.DIAMOND, 64);
        diamonds.set(DataComponents.CUSTOM_NAME, DIAMOND_NAME);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET, 1);
        helmet.set(DataComponents.DAMAGE, 37);

        player.getInventory().setItem(0, diamonds);
        player.getInventory().setItem(1, new ItemStack(ModItems.BACKPACK_BASIC, 1));
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD, 1));

        player.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos, 4.0);
                List<ItemEntity> deathBackpacks = deathBackpacks(drops);
                require(helper, deathBackpacks.size() == 1,
                        "Expected exactly one death backpack, found " + deathBackpacks.size());

                List<ItemStack> stored = storedItems(deathBackpacks.getFirst().getItem());
                require(helper, stored.stream().anyMatch(stack ->
                                stack.is(Items.DIAMOND)
                                        && stack.getCount() == 64
                                        && DIAMOND_NAME.equals(stack.get(DataComponents.CUSTOM_NAME))),
                        "Death backpack did not preserve the 64-count named diamond stack");
                require(helper, stored.stream().anyMatch(stack ->
                                stack.is(Items.IRON_HELMET)
                                        && stack.getOrDefault(DataComponents.DAMAGE, 0) == 37),
                        "Death backpack did not preserve damaged equipped armor");
                require(helper, stored.stream().anyMatch(stack -> stack.is(Items.SHIELD)),
                        "Death backpack did not capture the offhand item");
                require(helper, stored.stream().noneMatch(BackpackItemHelper::isBackpackItem),
                        "A backpack was nested inside the death backpack");

                require(helper, drops.stream().noneMatch(entity ->
                                entity.getItem().is(Items.DIAMOND)
                                        || entity.getItem().is(Items.IRON_HELMET)
                                        || entity.getItem().is(Items.SHIELD)),
                        "Captured inventory or equipment was still emitted as world ItemEntities");
                require(helper, drops.stream().anyMatch(entity -> entity.getItem().is(ModItems.BACKPACK_BASIC)),
                        "Excluded tiered backpack was not emitted by vanilla");
                require(helper, player.getInventory().isEmpty(),
                        "Vanilla death processing did not clear the player's Inventory and equipment");

                helper.succeed();
            } finally {
                player.discard();
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void keepInventorySkipsDeathBackpackCapture(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 8));

        boolean previousKeepInventory = helper.getLevel().getGameRules().get(GameRules.KEEP_INVENTORY);
        helper.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY, true, helper.getLevel().getServer());
        try {
            player.die(helper.getLevel().damageSources().generic());
        } finally {
            helper.getLevel().getGameRules().set(
                    GameRules.KEEP_INVENTORY,
                    previousKeepInventory,
                    helper.getLevel().getServer()
            );
        }

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos, 4.0);
                require(helper, deathBackpacks(drops).isEmpty(),
                        "keepInventory death unexpectedly created a death backpack");
                require(helper, drops.stream().noneMatch(entity -> entity.getItem().is(Items.DIAMOND)),
                        "keepInventory death emitted the retained stack as a world ItemEntity");
                require(helper, player.getInventory().getItem(0).is(Items.DIAMOND)
                                && player.getInventory().getItem(0).getCount() == 8,
                        "keepInventory death did not retain the player's stack");
                helper.succeed();
            } finally {
                player.discard();
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void vanishingCurseIsDestroyedBeforeDirectCapture(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);

        ItemStack vanishingSword = new ItemStack(Items.DIAMOND_SWORD);
        var vanishing = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.VANISHING_CURSE);
        vanishingSword.enchant(vanishing, 1);
        player.getInventory().setItem(0, vanishingSword);

        player.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos, 4.0);
                require(helper, deathBackpacks(drops).isEmpty(),
                        "A vanishing-only inventory unexpectedly created a death backpack");
                require(helper, drops.stream().noneMatch(entity -> entity.getItem().is(Items.DIAMOND_SWORD)),
                        "Curse of Vanishing item was emitted as a world ItemEntity");
                require(helper, player.getInventory().isEmpty(),
                        "Curse of Vanishing item remained in the dead player's inventory");
                helper.succeed();
            } finally {
                player.discard();
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void existingNearbyItemEntityIsNotCollected(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ItemEntity existingDrop = helper.spawnItem(Items.EMERALD, DEATH_POS.offset(1, 0, 0));
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 4));

        player.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos, 4.0);
                List<ItemEntity> deathBackpacks = deathBackpacks(drops);
                require(helper, deathBackpacks.size() == 1,
                        "Expected one death backpack while preserving an existing world drop");
                List<ItemStack> stored = storedItems(deathBackpacks.getFirst().getItem());
                require(helper, stored.stream().anyMatch(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 4),
                        "The player's inventory was not captured");
                require(helper, stored.stream().noneMatch(stack -> stack.is(Items.EMERALD)),
                        "An existing nearby ItemEntity was incorrectly captured");
                require(helper, existingDrop.isAlive() && existingDrop.getItem().is(Items.EMERALD),
                        "The existing nearby ItemEntity was removed or changed");
                helper.succeed();
            } finally {
                player.discard();
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 80)
    public void sameTickSamePositionDeathsCreateIndependentBackpacks(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer first = createPlayerAt(helper, absoluteDeathPos);
        ServerPlayer second = createPlayerAt(helper, absoluteDeathPos);
        first.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        second.getInventory().setItem(0, new ItemStack(Items.EMERALD, 5));

        first.die(helper.getLevel().damageSources().generic());
        second.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos, 4.0);
                List<ItemEntity> deathBackpacks = deathBackpacks(drops);
                require(helper, deathBackpacks.size() == 2,
                        "Expected two independent death backpacks, found " + deathBackpacks.size());

                List<List<ItemStack>> contents = deathBackpacks.stream()
                        .map(entity -> storedItems(entity.getItem()))
                        .toList();
                require(helper, contents.stream().anyMatch(stored ->
                                stored.size() == 1
                                        && stored.getFirst().is(Items.DIAMOND)
                                        && stored.getFirst().getCount() == 3),
                        "No independent death backpack contained the first player's diamonds");
                require(helper, contents.stream().anyMatch(stored ->
                                stored.size() == 1
                                        && stored.getFirst().is(Items.EMERALD)
                                        && stored.getFirst().getCount() == 5),
                        "No independent death backpack contained the second player's emeralds");
                require(helper, drops.stream().noneMatch(entity ->
                                entity.getItem().is(Items.DIAMOND) || entity.getItem().is(Items.EMERALD)),
                        "Captured stacks leaked into world ItemEntities during simultaneous deaths");
                helper.succeed();
            } finally {
                first.discard();
                second.discard();
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

    private static List<ItemEntity> itemDropsAround(GameTestHelper helper, BlockPos absolutePos, double radius) {
        return helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(absolutePos).inflate(radius),
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

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
