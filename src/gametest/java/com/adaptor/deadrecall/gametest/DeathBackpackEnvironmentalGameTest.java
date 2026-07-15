package com.adaptor.deadrecall.gametest;

import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.mixin.DeadRecallSpaceUnitSavedDataAccessor;
import com.adaptor.deadrecall.space.DeadRecallSpaceUnitSavedData;
import com.adaptor.deadrecall.space.SpaceUnitRecord;
import com.adaptor.deadrecall.space.SpaceUnitType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DeathBackpackEnvironmentalGameTest {
    private static final BlockPos DEATH_POS = new BlockPos(2, 2, 2);
    private static final Component EXISTING_DEATH_BACKPACK_NAME = Component.literal("Existing death backpack");

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void lavaDeathUsesDirectCapture(GameTestHelper helper) {
        verifyEnvironmentalCapture(helper, helper.getLevel().damageSources().lava(), Items.DIAMOND, 2);
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void cactusDeathUsesDirectCapture(GameTestHelper helper) {
        verifyEnvironmentalCapture(helper, helper.getLevel().damageSources().cactus(), Items.EMERALD, 3);
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void voidDeathUsesDirectCapture(GameTestHelper helper) {
        verifyEnvironmentalCapture(helper, helper.getLevel().damageSources().fellOutOfWorld(), Items.NETHERITE_SCRAP, 4);
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void explosionDeathUsesDirectCapture(GameTestHelper helper) {
        verifyEnvironmentalCapture(helper, helper.getLevel().damageSources().explosion(null, null), Items.GOLD_INGOT, 5);
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 60)
    public void backpackOnlyInventoryDoesNotCreateAnotherDeathBackpack(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);

        ItemStack basicBackpack = new ItemStack(ModItems.BACKPACK_BASIC);
        ItemStack existingDeathBackpack = new ItemStack(ModItems.DEATH_BACKPACK);
        existingDeathBackpack.set(DataComponents.CUSTOM_NAME, EXISTING_DEATH_BACKPACK_NAME);
        player.getInventory().setItem(0, basicBackpack);
        player.getInventory().setItem(1, existingDeathBackpack);

        Set<UUID> deathNodesBefore = deathNodeIds(helper, player.getUUID());
        player.die(helper.getLevel().damageSources().generic());

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos);
                List<ItemEntity> basicBackpacks = drops.stream()
                        .filter(entity -> entity.getItem().is(ModItems.BACKPACK_BASIC))
                        .toList();
                List<ItemEntity> deathBackpacks = drops.stream()
                        .filter(entity -> entity.getItem().is(ModItems.DEATH_BACKPACK))
                        .toList();

                require(helper, basicBackpacks.size() == 1,
                        "Backpack-only death did not preserve exactly one basic backpack drop");
                require(helper, deathBackpacks.size() == 1,
                        "Backpack-only death created an additional death backpack");
                require(helper, EXISTING_DEATH_BACKPACK_NAME.equals(
                                deathBackpacks.getFirst().getItem().get(DataComponents.CUSTOM_NAME)),
                        "The dropped death backpack was not the original named stack");
                require(helper, storedItems(deathBackpacks.getFirst().getItem()).isEmpty(),
                        "The existing death backpack was unexpectedly rewritten with nested contents");
                require(helper, deathNodeIds(helper, player.getUUID()).equals(deathNodesBefore),
                        "Backpack-only death created a new death Space Unit");
                require(helper, player.getInventory().isEmpty(),
                        "Vanilla did not clear the excluded backpack slots after dropping them");
                helper.succeed();
            } finally {
                player.discard();
            }
        });
    }

    private static void verifyEnvironmentalCapture(
            GameTestHelper helper,
            DamageSource damageSource,
            Item expectedItem,
            int expectedCount
    ) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos absoluteDeathPos = helper.absolutePos(DEATH_POS);
        ServerPlayer player = createPlayerAt(helper, absoluteDeathPos);
        player.getInventory().setItem(0, new ItemStack(expectedItem, expectedCount));

        Set<UUID> deathNodesBefore = deathNodeIds(helper, player.getUUID());
        player.die(damageSource);

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDropsAround(helper, absoluteDeathPos);
                List<ItemEntity> deathBackpacks = drops.stream()
                        .filter(entity -> BackpackItemHelper.isDeathBackpackItem(entity.getItem()))
                        .toList();
                require(helper, deathBackpacks.size() == 1,
                        "Environmental death expected one death backpack, found " + deathBackpacks.size());

                List<ItemStack> stored = storedItems(deathBackpacks.getFirst().getItem());
                require(helper, stored.stream().anyMatch(stack ->
                                stack.is(expectedItem) && stack.getCount() == expectedCount),
                        "Environmental death did not preserve the expected captured stack");
                require(helper, drops.stream().noneMatch(entity -> entity.getItem().is(expectedItem)),
                        "Environmental death emitted a captured stack as a loose ItemEntity");

                Set<UUID> deathNodesAfter = deathNodeIds(helper, player.getUUID());
                Set<UUID> createdNodes = new HashSet<>(deathNodesAfter);
                createdNodes.removeAll(deathNodesBefore);
                require(helper, createdNodes.size() == 1,
                        "Environmental death did not create exactly one death Space Unit");
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

    private static List<ItemStack> storedItems(ItemStack deathBackpack) {
        return deathBackpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream()
                .toList();
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
