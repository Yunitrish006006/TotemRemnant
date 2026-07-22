package dev.totem.remnant.death;

import dev.totem.core.api.v1.death.DeathBackpackNodeLifecycle;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
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
