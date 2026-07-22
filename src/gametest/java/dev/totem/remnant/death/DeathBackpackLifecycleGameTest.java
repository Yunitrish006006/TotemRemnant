package dev.totem.remnant.death;

import dev.totem.core.api.v1.death.DeathBackpackNodeLifecycle;
import dev.totem.remnant.registry.RemnantItemRegistration;
import dev.totem.remnant.inventory.DeathBackpackInventory;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Verifies Remnant's real capture and recovery lifecycle without the compatibility bundle. */
public final class DeathBackpackLifecycleGameTest {
    private static final BlockPos DEATH_POS = new BlockPos(2, 2, 2);

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void captureThenRecoveryPreservesContentsAndCompletesBoundNode(GameTestHelper helper) {
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        BlockPos position = helper.absolutePos(DEATH_POS);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0.0F, 0.0F);
        UUID nodeId = UUID.randomUUID();
        boolean[] recovered = {false};
        ItemEntity[] deathBackpackEntity = {null};
        DeathBackpackNodeLifecycle.register(new TestNodeLifecycle(nodeId, recovered));

        try {
            ItemStack diamonds = new ItemStack(Items.DIAMOND, 12);
            require(helper, DeathBackpackCaptureLifecycle.commit(player, helper.getLevel(), position, List.of(diamonds)),
                    "Remnant capture lifecycle did not commit");
            deathBackpackEntity[0] = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(3.0D),
                            entity -> entity.isAlive() && entity.getItem().is(RemnantItemRegistration.DEATH_BACKPACK))
                    .stream().findFirst().orElseThrow(() -> helper.assertionException("Remnant did not create a death backpack entity"));
            ItemStack deathBackpack = deathBackpackEntity[0].getItem();
            List<ItemStack> stored = deathBackpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .nonEmptyItemCopyStream().toList();
            require(helper, stored.size() == 1 && stored.getFirst().is(Items.DIAMOND) && stored.getFirst().getCount() == 12,
                    "Remnant death backpack did not preserve captured contents");
            require(helper, nodeId.equals(DeathBackpackNodeBinding.read(deathBackpack)),
                    "Remnant death backpack did not preserve the Core node binding");
            require(helper, DeathBackpackRecoveryService.recoverBoundNode(player, deathBackpack),
                    "Remnant recovery lifecycle did not complete the bound node");
            require(helper, recovered[0], "Core node lifecycle was not invoked during Remnant recovery");
            helper.succeed();
        } finally {
            DeathBackpackNodeLifecycle.register(null);
            if (deathBackpackEntity[0] != null) deathBackpackEntity[0].discard();
            player.discard();
        }
    }

    @GameTest(maxTicks = 40)
    public void legacyDeathBackpackDataSurvivesItemStackNbtRoundTrip(GameTestHelper helper) {
        UUID nodeId = UUID.randomUUID();
        ItemStack legacyDeathBackpack = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
        legacyDeathBackpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.EMERALD, 7))));
        DeathBackpackNodeBinding.write(legacyDeathBackpack, nodeId);

        Tag encoded = ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, legacyDeathBackpack).getOrThrow();
        ItemStack decoded = ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        List<ItemStack> stored = decoded.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream().toList();
        require(helper, decoded.is(RemnantItemRegistration.DEATH_BACKPACK),
                "Legacy deadrecall:death_backpack item ID did not survive NBT serialization");
        require(helper, stored.size() == 1 && stored.getFirst().is(Items.EMERALD) && stored.getFirst().getCount() == 7,
                "Death backpack contents did not survive NBT serialization");
        require(helper, nodeId.equals(DeathBackpackNodeBinding.read(decoded)),
                "Death backpack node binding did not survive NBT serialization");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void nonOwnerRecoveryOnlyCompletesTheBoundNode(GameTestHelper helper) {
        BlockPos firstPosition = helper.absolutePos(DEATH_POS);
        BlockPos secondPosition = firstPosition.offset(5, 0, 0);
        helper.setBlock(DEATH_POS.below(), Blocks.STONE);
        helper.setBlock(DEATH_POS.offset(5, -1, 0), Blocks.STONE);
        ServerPlayer firstOwner = helper.makeMockServerPlayerInLevel();
        ServerPlayer secondOwner = helper.makeMockServerPlayerInLevel();
        ServerPlayer recoveringPlayer = helper.makeMockServerPlayerInLevel();
        firstOwner.snapTo(firstPosition.getX() + .5D, firstPosition.getY(), firstPosition.getZ() + .5D, 0, 0);
        secondOwner.snapTo(secondPosition.getX() + .5D, secondPosition.getY(), secondPosition.getZ() + .5D, 0, 0);
        UUID firstNode = UUID.randomUUID();
        UUID secondNode = UUID.randomUUID();
        Set<UUID> recovered = new HashSet<>();
        ItemEntity[] firstEntity = {null};
        ItemEntity[] secondEntity = {null};
        DeathBackpackNodeLifecycle.register(new DeathBackpackNodeLifecycle() {
            @Override public UUID create(ServerPlayer owner, ServerLevel level, BlockPos position) {
                return owner == firstOwner ? firstNode : secondNode;
            }
            @Override public void rollback(ServerPlayer owner, ServerLevel level, UUID nodeId) { }
            @Override public boolean recover(ServerPlayer player, UUID nodeId) { return recovered.add(nodeId); }
        });
        try {
            require(helper, DeathBackpackCaptureLifecycle.commit(firstOwner, helper.getLevel(), firstPosition,
                    List.of(new ItemStack(Items.DIAMOND, 3))), "First owner capture failed");
            require(helper, DeathBackpackCaptureLifecycle.commit(secondOwner, helper.getLevel(), secondPosition,
                    List.of(new ItemStack(Items.EMERALD, 5))), "Second owner capture failed");
            List<ItemEntity> entities = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(firstPosition).minmax(new AABB(secondPosition)).inflate(3),
                    entity -> entity.isAlive() && entity.getItem().is(RemnantItemRegistration.DEATH_BACKPACK));
            firstEntity[0] = entities.stream().filter(entity -> entity.getItem().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.DIAMOND))).findFirst()
                    .orElseThrow(() -> helper.assertionException("First owner backpack missing"));
            secondEntity[0] = entities.stream().filter(entity -> entity.getItem().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.EMERALD))).findFirst()
                    .orElseThrow(() -> helper.assertionException("Second owner backpack missing"));
            require(helper, DeathBackpackRecoveryService.recoverBoundNode(recoveringPlayer, firstEntity[0].getItem()),
                    "Non-owner recovery failed");
            require(helper, recovered.equals(Set.of(firstNode)), "Recovery completed an unrelated node");
            require(helper, secondNode.equals(DeathBackpackNodeBinding.read(secondEntity[0].getItem())),
                    "Unrelated backpack binding changed");
            helper.succeed();
        } finally {
            DeathBackpackNodeLifecycle.register(null);
            if (firstEntity[0] != null) firstEntity[0].discard();
            if (secondEntity[0] != null) secondEntity[0].discard();
            firstOwner.discard(); secondOwner.discard(); recoveringPlayer.discard();
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void closingEmptyRemnantInventoryRecoversAndRemovesBackpack(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        UUID nodeId = UUID.randomUUID();
        boolean[] recovered = {false};
        ItemStack backpack = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.GOLD_INGOT, 2))));
        DeathBackpackNodeBinding.write(backpack, nodeId);
        player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
        DeathBackpackNodeLifecycle.register(new TestNodeLifecycle(nodeId, recovered));
        try {
            DeathBackpackInventory inventory = new DeathBackpackInventory(player, InteractionHand.MAIN_HAND, 9);
            inventory.clearContent();
            inventory.stopOpen(player);
            require(helper, player.getMainHandItem().isEmpty(), "Empty Remnant death backpack remained in hand");
            require(helper, recovered[0], "Closing empty Remnant inventory did not recover the bound node");
            helper.succeed();
        } finally {
            DeathBackpackNodeLifecycle.register(null);
            player.discard();
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(message);
    }

    private record TestNodeLifecycle(UUID nodeId, boolean[] recovered) implements DeathBackpackNodeLifecycle {
        @Override public UUID create(ServerPlayer owner, ServerLevel level, BlockPos position) { return nodeId; }
        @Override public void rollback(ServerPlayer owner, ServerLevel level, UUID ignored) { }
        @Override public boolean recover(ServerPlayer recoveringPlayer, UUID recoveredNodeId) {
            recovered[0] = nodeId.equals(recoveredNodeId);
            return recovered[0];
        }
    }
}
