package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryRegistry;
import com.adaptor.deadrecall.integration.trinkets.TrinketsDeathBackpackInventoryProvider;
import com.adaptor.deadrecall.item.BackpackItemHelper;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class DeathBackpackTrinketsGameTest {
    private static final BlockPos DEATH_POS = new BlockPos(2, 2, 2);
    private static final Component TRINKET_NAME = Component.literal("DeadRecall Trinkets component");

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 80)
    public void droppableTrinketIsCapturedExactlyOnceWithComponents(GameTestHelper helper) {
        BlockPos absoluteDeathPos = prepareDeathPosition(helper);
        ServerPlayer player = createPlayer(helper, absoluteDeathPos);

        try {
            TrinketSlotAccess access = requireDropSlot(helper, player);
            require(helper, access.set(namedDiamonds(5)),
                    "Could not place the component-bearing stack into the Trinkets slot");
            player.die(helper.getLevel().damageSources().generic());
        } catch (RuntimeException exception) {
            player.discard();
            throw exception;
        }

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDrops(helper, absoluteDeathPos);
                List<ItemEntity> deathBackpacks = deathBackpacks(drops);
                require(helper, deathBackpacks.size() == 1,
                        "Expected one death backpack for the droppable Trinkets stack, found "
                                + deathBackpacks.size());

                List<ItemStack> stored = storedItems(deathBackpacks.getFirst().getItem());
                require(helper, stored.stream().anyMatch(stack ->
                                stack.is(Items.DIAMOND)
                                        && stack.getCount() == 5
                                        && TRINKET_NAME.equals(stack.get(DataComponents.CUSTOM_NAME))),
                        "Death backpack did not preserve the Trinkets stack count and Components");
                require(helper, drops.stream().noneMatch(entity -> isNamedDiamond(entity.getItem(), 5)),
                        "The captured Trinkets stack was also emitted as a loose ItemEntity");

                TrinketSlotAccess reloadedAccess = requireDropSlot(helper, player);
                require(helper, reloadedAccess.get().isEmpty(),
                        "Committed Trinkets source slot was not cleared");
                helper.succeed();
            } finally {
                player.discard();
            }
        });
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 80)
    public void failedDeathBackpackCommitReturnsTrinketToItsNativeDropPath(GameTestHelper helper) {
        BlockPos absoluteDeathPos = prepareDeathPosition(helper);
        ServerPlayer player = createPlayer(helper, absoluteDeathPos);

        try {
            TrinketSlotAccess access = requireDropSlot(helper, player);
            require(helper, access.set(namedDiamonds(3)),
                    "Could not place rollback fixture into the Trinkets slot");
            DeathBackpackCaptureService.forceFailureForTesting(
                    player.getUUID(),
                    DeathBackpackCaptureService.CaptureFailurePoint.AFTER_ENTITY_ADD
            );
            player.die(helper.getLevel().damageSources().generic());
        } catch (RuntimeException exception) {
            DeathBackpackCaptureService.clearForcedFailureForTesting(player.getUUID());
            player.discard();
            throw exception;
        }

        helper.runAtTickTime(5, () -> {
            try {
                List<ItemEntity> drops = itemDrops(helper, absoluteDeathPos);
                require(helper, deathBackpacks(drops).isEmpty(),
                        "Failed Trinkets capture left an incomplete death backpack");
                long matchingDrops = drops.stream()
                        .filter(entity -> isNamedDiamond(entity.getItem(), 3))
                        .count();
                require(helper, matchingDrops == 1L,
                        "Trinkets rollback did not produce exactly one native DROP result; found "
                                + matchingDrops);
                TrinketSlotAccess access = requireDropSlot(helper, player);
                require(helper, access.get().isEmpty(),
                        "Trinkets native death handling did not clear the restored source slot");
                helper.succeed();
            } finally {
                DeathBackpackCaptureService.clearForcedFailureForTesting(player.getUUID());
                player.discard();
            }
        });
    }

    private static BlockPos prepareDeathPosition(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        return helper.absolutePos(DEATH_POS);
    }

    private static ServerPlayer createPlayer(GameTestHelper helper, BlockPos absoluteDeathPos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(
                absoluteDeathPos.getX() + 0.5D,
                absoluteDeathPos.getY(),
                absoluteDeathPos.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        return player;
    }

    private static TrinketSlotAccess requireDropSlot(GameTestHelper helper, ServerPlayer player) {
        require(helper, FabricLoader.getInstance().isModLoaded("trinkets_updated"),
                "Trinkets Updated was not loaded in the GameTest runtime");
        require(helper, DeathBackpackAddonInventoryRegistry.providers().stream()
                        .anyMatch(provider -> provider.id().equals(TrinketsDeathBackpackInventoryProvider.ID)),
                "Trinkets death-backpack adapter was not registered");

        TrinketSlotAccess access = TrinketsApi.getAttachment(player)
                .getSlotAccess("deadrecall_test/drop", 0);
        require(helper, access != null && access.isValid(),
                "The GameTest Trinkets DROP slot was not available on the player");
        return access;
    }

    private static ItemStack namedDiamonds(int count) {
        ItemStack stack = new ItemStack(Items.DIAMOND, count);
        stack.set(DataComponents.CUSTOM_NAME, TRINKET_NAME);
        return stack;
    }

    private static List<ItemEntity> itemDrops(GameTestHelper helper, BlockPos absoluteDeathPos) {
        return helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(absoluteDeathPos).inflate(4.0D),
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

    private static boolean isNamedDiamond(ItemStack stack, int count) {
        return stack.is(Items.DIAMOND)
                && stack.getCount() == count
                && TRINKET_NAME.equals(stack.get(DataComponents.CUSTOM_NAME));
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
