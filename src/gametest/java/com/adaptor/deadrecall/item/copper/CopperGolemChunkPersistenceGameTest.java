package com.adaptor.deadrecall.item.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;

public final class CopperGolemChunkPersistenceGameTest {
    private static final Component TOOL_NAME = Component.literal("Chunk-persistent gathering tool");
    private static final Component STORAGE_NAME = Component.literal("Chunk-persistent gathering storage");
    private static final int EXPECTED_REVISION = 41;
    private static final int EXPECTED_SCAN_CURSOR = 137;

    @GameTest(maxTicks = 620)
    public void entityHomeTargetAndScannerStateSurviveChunkUnloadReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Fixture fixture = findUnloadedFixture(helper);
        level.setChunkForced(fixture.chunkX(), fixture.chunkZ(), true);
        level.getChunk(fixture.chunkX(), fixture.chunkZ());

        require(helper, level.setBlockAndUpdate(fixture.golemPos().below(), Blocks.STONE.defaultBlockState()),
                "Could not create the remote copper golem floor");
        require(helper, level.setBlockAndUpdate(fixture.homePos(), copperChest(helper).defaultBlockState()),
                "Could not create the remote copper chest Home");
        require(helper, level.setBlockAndUpdate(fixture.targetPos(), Blocks.STONE.defaultBlockState()),
                "Could not create the remote gathering target");

        CopperGolem golem = constructGolem(helper, fixture.golemPos());
        golem.setPersistenceRequired();
        UUID golemId = golem.getUUID();
        configurePersistentState(level, golem, fixture);

        boolean[] unloadRequested = {false};
        boolean[] observedUnloaded = {false};
        helper.runAtTickTime(20, () -> {
            level.setChunkForced(fixture.chunkX(), fixture.chunkZ(), false);
            unloadRequested[0] = true;
        });

        for (int tick = 21; tick < 600; tick++) {
            helper.runAtTickTime(tick, () -> {
                if (!unloadRequested[0]) {
                    return;
                }

                if (!observedUnloaded[0]) {
                    if (!level.isLoaded(fixture.golemPos())) {
                        observedUnloaded[0] = true;
                        level.setChunkForced(fixture.chunkX(), fixture.chunkZ(), true);
                        level.getChunk(fixture.chunkX(), fixture.chunkZ());
                    }
                    return;
                }

                if (!level.isLoaded(fixture.golemPos())) {
                    return;
                }

                Entity loadedEntity = level.getEntity(golemId);
                if (!(loadedEntity instanceof CopperGolem reloaded)) {
                    return;
                }

                verifyPersistentState(helper, reloaded, fixture);
                require(helper, level.getBlockState(fixture.homePos()).is(copperChest(helper)),
                        "Copper chest Home did not survive chunk unload/reload");
                require(helper, level.getBlockState(fixture.targetPos()).is(Blocks.STONE),
                        "Gathering target block did not survive chunk unload/reload");

                CopperGolemWrenchHandler.untrackCopperGolem(reloaded);
                reloaded.discard();
                level.setBlockAndUpdate(fixture.homePos(), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(fixture.targetPos(), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(fixture.golemPos().below(), Blocks.AIR.defaultBlockState());
                level.setChunkForced(fixture.chunkX(), fixture.chunkZ(), false);
                helper.succeed();
            });
        }

        helper.runAtTickTime(605, () -> {
            level.setChunkForced(fixture.chunkX(), fixture.chunkZ(), false);
            if (!observedUnloaded[0]) {
                throw helper.assertionException("Remote copper golem chunk never unloaded");
            }
            throw helper.assertionException("Remote copper golem did not reload before the timeout");
        });
    }

    private static void configurePersistentState(ServerLevel level, CopperGolem golem, Fixture fixture) {
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        tool.setDamageValue(17);
        tool.set(DataComponents.CUSTOM_NAME, TOOL_NAME);

        ItemStack storage = new ItemStack(Items.COBBLESTONE, 13);
        storage.set(DataComponents.CUSTOM_NAME, STORAGE_NAME);

        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        tag.putInt(CopperGolemData.TAG_DATA_VERSION, CopperGolemData.DATA_VERSION);
        tag.putInt(CopperGolemData.TAG_REVISION, EXPECTED_REVISION);
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.GATHERING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, false);
        tag.putString(CopperGolemData.TAG_ACTIVITY, CopperGolemActivity.RETURNING_HOME.id());
        CopperGolemData.writeItemStack(tag, CopperGolemData.TAG_FUEL_STACK, new ItemStack(Items.COAL, 3));
        tag.putInt(CopperGolemData.TAG_FUEL_TICKS, 211);
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_tool_stack", tool);
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_storage_stack", storage);
        SortingBindingService.writeSourceContainer(tag, new CopperGolemWrenchHandler.Binding(
                level.dimension(), fixture.homePos()));
        tag.putString("deadrecall_gathering_area_dim", level.dimension().identifier().toString());
        writePos(tag, fixture.targetPos(),
                "deadrecall_gathering_corner_a_x",
                "deadrecall_gathering_corner_a_y",
                "deadrecall_gathering_corner_a_z");
        writePos(tag, fixture.targetPos().offset(1, 1, 1),
                "deadrecall_gathering_corner_b_x",
                "deadrecall_gathering_corner_b_y",
                "deadrecall_gathering_corner_b_z");
        writePos(tag, fixture.targetPos(),
                "deadrecall_gathering_target_x",
                "deadrecall_gathering_target_y",
                "deadrecall_gathering_target_z");
        tag.putLong("deadrecall_gathering_scan_index", EXPECTED_SCAN_CURSOR);
        CopperGolemData.writeStringList(
                tag,
                "deadrecall_gathering_manual_targets",
                List.of("minecraft:stone", "minecraft:deepslate"),
                64
        );
        CopperGolemData.writeEntityTag(golem, tag);
    }

    private static void verifyPersistentState(GameTestHelper helper, CopperGolem golem, Fixture fixture) {
        require(helper, CopperGolemWrenchHandler.getMode(golem) == CopperGolemMode.GATHERING,
                "Chunk reload lost gathering mode");
        require(helper, !CopperGolemWrenchHandler.isTransportEnabled(golem),
                "Chunk reload changed the stopped state");
        require(helper, CopperGolemData.revision(golem) == EXPECTED_REVISION,
                "Chunk reload changed the revision");
        require(helper, CopperGolemData.activity(golem) == CopperGolemActivity.RETURNING_HOME,
                "Chunk reload lost the stored activity");
        require(helper, CopperGolemWrenchHandler.getSourceContainer(golem)
                        .filter(binding -> binding.dimension().equals(helper.getLevel().dimension()))
                        .filter(binding -> binding.containerPos().equals(fixture.homePos()))
                        .isPresent(),
                "Chunk reload lost the Home binding");

        ItemStack tool = CopperGolemWrenchHandler.getGatheringToolStackForMenu(golem);
        require(helper, tool.is(Items.IRON_PICKAXE) && tool.getDamageValue() == 17,
                "Chunk reload lost gathering tool durability");
        require(helper, TOOL_NAME.equals(tool.get(DataComponents.CUSTOM_NAME)),
                "Chunk reload lost gathering tool components");

        ItemStack storage = CopperGolemWrenchHandler.getGatheringStorageStackForMenu(golem);
        require(helper, storage.is(Items.COBBLESTONE) && storage.getCount() == 13,
                "Chunk reload lost gathering storage quantity");
        require(helper, STORAGE_NAME.equals(storage.get(DataComponents.CUSTOM_NAME)),
                "Chunk reload lost gathering storage components");

        ItemStack fuel = CopperGolemData.fuelStack(golem);
        require(helper, fuel.is(Items.COAL) && fuel.getCount() == 3 && CopperGolemData.fuelTicks(golem) == 211,
                "Chunk reload lost fuel state");

        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        require(helper, tag.getLongOr("deadrecall_gathering_scan_index", -1L) == EXPECTED_SCAN_CURSOR,
                "Chunk reload lost the gathering scan cursor");
        require(helper, readPos(tag,
                        "deadrecall_gathering_target_x",
                        "deadrecall_gathering_target_y",
                        "deadrecall_gathering_target_z").equals(fixture.targetPos()),
                "Chunk reload lost the gathering target");
        require(helper, CopperGolemData.readStringList(tag, "deadrecall_gathering_manual_targets")
                        .equals(List.of("minecraft:stone", "minecraft:deepslate")),
                "Chunk reload lost manual gathering targets");
    }

    private static Fixture findUnloadedFixture(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        for (int offset = 64; offset <= 512; offset += 32) {
            int chunkX = originChunkX + offset;
            int chunkZ = originChunkZ + offset;
            BlockPos golemPos = new BlockPos((chunkX << 4) + 4, 200, (chunkZ << 4) + 4);
            if (!level.isLoaded(golemPos)) {
                return new Fixture(
                        golemPos,
                        golemPos.offset(2, 0, 0),
                        golemPos.offset(4, 0, 0),
                        chunkX,
                        chunkZ
                );
            }
        }
        throw helper.assertionException("Could not locate an unloaded chunk for the copper golem fixture");
    }

    private static CopperGolem constructGolem(GameTestHelper helper, BlockPos absolutePos) {
        Object entityType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_golem"));
        if (entityType == null) {
            throw helper.assertionException("Missing minecraft:copper_golem entity type");
        }

        try {
            for (Constructor<?> constructor : CopperGolem.class.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 2
                        || !parameterTypes[0].isInstance(entityType)
                        || !parameterTypes[1].isInstance(helper.getLevel())) {
                    continue;
                }

                constructor.setAccessible(true);
                CopperGolem golem = (CopperGolem) constructor.newInstance(entityType, helper.getLevel());
                golem.snapTo(
                        absolutePos.getX() + 0.5D,
                        absolutePos.getY(),
                        absolutePos.getZ() + 0.5D,
                        0.0F,
                        0.0F
                );
                require(helper, helper.getLevel().addFreshEntity(golem),
                        "Could not add the remote copper golem to the GameTest level");
                return golem;
            }
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not construct copper golem fixture", exception);
        }
        throw helper.assertionException("No compatible CopperGolem constructor was found");
    }

    private static Block copperChest(GameTestHelper helper) {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_chest"));
        if (block == null || block == Blocks.AIR) {
            throw helper.assertionException("Missing minecraft:copper_chest block");
        }
        return block;
    }

    private static void writePos(CompoundTag tag, BlockPos pos, String xKey, String yKey, String zKey) {
        tag.putInt(xKey, pos.getX());
        tag.putInt(yKey, pos.getY());
        tag.putInt(zKey, pos.getZ());
    }

    private static BlockPos readPos(CompoundTag tag, String xKey, String yKey, String zKey) {
        return new BlockPos(
                tag.getIntOr(xKey, Integer.MIN_VALUE),
                tag.getIntOr(yKey, Integer.MIN_VALUE),
                tag.getIntOr(zKey, Integer.MIN_VALUE)
        );
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    private record Fixture(BlockPos golemPos, BlockPos homePos, BlockPos targetPos, int chunkX, int chunkZ) {
    }
}
