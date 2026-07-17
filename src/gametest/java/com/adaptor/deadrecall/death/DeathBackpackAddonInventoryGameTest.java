package com.adaptor.deadrecall.death;

import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryProvider;
import com.adaptor.deadrecall.api.death.DeathBackpackAddonInventoryRegistry;
import com.adaptor.deadrecall.api.death.DeathBackpackAddonSlot;
import com.adaptor.deadrecall.integration.trinkets.TrinketsDeathBackpackInventoryProvider;
import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.item.ModItems;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeathBackpackAddonInventoryGameTest {
    private static final BlockPos CAPTURE_POS = new BlockPos(2, 2, 2);
    private static final Component ADDON_NAME = Component.literal("DeadRecall addon component");
    private static final Identifier TEST_PROVIDER_ID =
            Identifier.fromNamespaceAndPath("deadrecall_gametest", "addon_inventory");
    private static final TestProvider TEST_PROVIDER = new TestProvider();

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void trinketsAdapterLoadsAsOptionalProvider(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper);
        try {
            require(helper, FabricLoader.getInstance().isModLoaded("trinkets_updated"),
                    "Trinkets Updated was missing from the GameTest runtime");
            require(helper, DeathBackpackAddonInventoryRegistry.providers().stream()
                            .anyMatch(provider -> provider.id().equals(TrinketsDeathBackpackInventoryProvider.ID)),
                    "Trinkets death-backpack provider was not registered");
            require(helper, TrinketsApi.getAttachment(player) != null,
                    "Trinkets did not attach its player inventory component");
            new TrinketsDeathBackpackInventoryProvider().collectDroppableSlots(player);
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void addonSlotCommitsWithExactComponents(GameTestHelper helper) {
        ensureTestProviderRegistered();
        ServerPlayer player = createPlayer(helper);
        TestSlot addonSlot = new TestSlot("component-slot", namedDiamonds(7), false);
        TEST_PROVIDER.states.put(player.getUUID(), new TestState(List.of(addonSlot), false));
        try {
            boolean captured = DeathBackpackCaptureService.captureBeforeVanillaDrop(player, helper.getLevel());
            require(helper, captured, "Addon-only capture did not create a death backpack");
            require(helper, addonSlot.current().isEmpty(), "Committed addon slot was not cleared");

            List<ItemEntity> backpacks = deathBackpacks(helper, player.blockPosition());
            require(helper, backpacks.size() == 1,
                    "Expected one addon death backpack, found " + backpacks.size());
            List<ItemStack> stored = storedItems(backpacks.getFirst().getItem());
            require(helper, stored.size() == 1
                            && stored.getFirst().is(Items.DIAMOND)
                            && stored.getFirst().getCount() == 7
                            && ADDON_NAME.equals(stored.getFirst().get(DataComponents.CUSTOM_NAME)),
                    "Addon stack count or Components changed during capture");
            helper.succeed();
        } finally {
            TEST_PROVIDER.states.remove(player.getUUID());
            discardBackpacks(helper, player.blockPosition());
            player.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void partialAddonCommitRollsBackAllSources(GameTestHelper helper) {
        ensureTestProviderRegistered();
        ServerPlayer player = createPlayer(helper);
        player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        TestSlot first = new TestSlot("first", new ItemStack(Items.DIAMOND, 2), false);
        TestSlot rejecting = new TestSlot("rejecting", new ItemStack(Items.EMERALD, 4), true);
        TEST_PROVIDER.states.put(player.getUUID(), new TestState(List.of(first, rejecting), false));
        try {
            boolean captured = DeathBackpackCaptureService.captureBeforeVanillaDrop(player, helper.getLevel());
            require(helper, !captured, "Rejected addon commit unexpectedly succeeded");
            require(helper, first.current().is(Items.DIAMOND) && first.current().getCount() == 2,
                    "Earlier addon slot was not restored after a later slot rejected commit");
            require(helper, rejecting.current().is(Items.EMERALD) && rejecting.current().getCount() == 4,
                    "Rejecting addon slot changed during rollback");
            require(helper, player.getInventory().getItem(0).is(Items.IRON_INGOT)
                            && player.getInventory().getItem(0).getCount() == 3,
                    "Vanilla Inventory was not restored after addon commit failure");
            require(helper, deathBackpacks(helper, player.blockPosition()).isEmpty(),
                    "Incomplete death backpack remained after addon rollback");
            helper.succeed();
        } finally {
            TEST_PROVIDER.states.remove(player.getUUID());
            player.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void addonSnapshotFailureDoesNotBlockVanillaCapture(GameTestHelper helper) {
        ensureTestProviderRegistered();
        ServerPlayer player = createPlayer(helper);
        player.getInventory().setItem(0, new ItemStack(Items.LAPIS_LAZULI, 6));
        TEST_PROVIDER.states.put(player.getUUID(), new TestState(List.of(), true));
        try {
            boolean captured = DeathBackpackCaptureService.captureBeforeVanillaDrop(player, helper.getLevel());
            require(helper, captured, "One failing addon provider blocked vanilla Inventory capture");
            List<ItemEntity> backpacks = deathBackpacks(helper, player.blockPosition());
            require(helper, backpacks.size() == 1, "Vanilla capture did not create one death backpack");
            require(helper, storedItems(backpacks.getFirst().getItem()).stream()
                            .anyMatch(stack -> stack.is(Items.LAPIS_LAZULI) && stack.getCount() == 6),
                    "Vanilla stack was lost when addon snapshot failed");
            helper.succeed();
        } finally {
            TEST_PROVIDER.states.remove(player.getUUID());
            discardBackpacks(helper, player.blockPosition());
            player.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void addonPortableContainerRemainsOutsideDeathBackpack(GameTestHelper helper) {
        ensureTestProviderRegistered();
        ServerPlayer player = createPlayer(helper);
        TestSlot addonSlot = new TestSlot("portable", new ItemStack(ModItems.BACKPACK_BASIC), false);
        TEST_PROVIDER.states.put(player.getUUID(), new TestState(List.of(addonSlot), false));
        try {
            boolean captured = DeathBackpackCaptureService.captureBeforeVanillaDrop(player, helper.getLevel());
            require(helper, !captured, "Portable addon container unexpectedly created a death backpack");
            require(helper, addonSlot.current().is(ModItems.BACKPACK_BASIC),
                    "Excluded addon portable container was removed from its owner slot");
            require(helper, deathBackpacks(helper, player.blockPosition()).isEmpty(),
                    "Portable addon container was nested in a death backpack");
            helper.succeed();
        } finally {
            TEST_PROVIDER.states.remove(player.getUUID());
            player.discard();
        }
    }

    private static synchronized void ensureTestProviderRegistered() {
        if (DeathBackpackAddonInventoryRegistry.providers().stream()
                .noneMatch(provider -> provider.id().equals(TEST_PROVIDER_ID))) {
            DeathBackpackAddonInventoryRegistry.register(TEST_PROVIDER);
        }
    }

    private static ServerPlayer createPlayer(GameTestHelper helper) {
        helper.setBlock(CAPTURE_POS.below(), Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absolute = helper.absolutePos(CAPTURE_POS);
        player.snapTo(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    private static ItemStack namedDiamonds(int count) {
        ItemStack stack = new ItemStack(Items.DIAMOND, count);
        stack.set(DataComponents.CUSTOM_NAME, ADDON_NAME);
        return stack;
    }

    private static List<ItemEntity> deathBackpacks(GameTestHelper helper, BlockPos center) {
        return helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(center).inflate(4.0D),
                entity -> entity.isAlive() && BackpackItemHelper.isDeathBackpackItem(entity.getItem()));
    }

    private static void discardBackpacks(GameTestHelper helper, BlockPos center) {
        deathBackpacks(helper, center).forEach(ItemEntity::discard);
    }

    private static List<ItemStack> storedItems(ItemStack deathBackpack) {
        return deathBackpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream()
                .toList();
    }

    private static boolean sameExactStack(ItemStack first, ItemStack second) {
        return first.getCount() == second.getCount() && ItemStack.isSameItemSameComponents(first, second);
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    private record TestState(List<TestSlot> slots, boolean failSnapshot) {
    }

    private static final class TestProvider implements DeathBackpackAddonInventoryProvider {
        private final Map<UUID, TestState> states = new ConcurrentHashMap<>();

        @Override
        public Identifier id() {
            return TEST_PROVIDER_ID;
        }

        @Override
        public List<? extends DeathBackpackAddonSlot> collectDroppableSlots(ServerPlayer player) {
            TestState state = states.get(player.getUUID());
            if (state == null) {
                return List.of();
            }
            if (state.failSnapshot()) {
                throw new IllegalStateException("Forced addon snapshot failure");
            }
            return state.slots();
        }
    }

    private static final class TestSlot implements DeathBackpackAddonSlot {
        private final String sourceKey;
        private final boolean rejectClear;
        private ItemStack stack;

        private TestSlot(String sourceKey, ItemStack stack, boolean rejectClear) {
            this.sourceKey = sourceKey;
            this.stack = stack.copy();
            this.rejectClear = rejectClear;
        }

        @Override
        public String sourceKey() {
            return sourceKey;
        }

        @Override
        public synchronized ItemStack snapshot() {
            return stack.copy();
        }

        @Override
        public synchronized boolean clearIfUnchanged(ItemStack expected) {
            if (rejectClear || !sameExactStack(stack, expected)) {
                return false;
            }
            stack = ItemStack.EMPTY;
            return true;
        }

        @Override
        public synchronized boolean restoreIfEmpty(ItemStack restored) {
            if (!stack.isEmpty()) {
                return false;
            }
            stack = restored.copy();
            return true;
        }

        private synchronized ItemStack current() {
            return stack.copy();
        }
    }
}
