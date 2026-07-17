package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CopperGolemAuthorityStressGameTest {
    private static final BlockPos GOLEM_POS = new BlockPos(3, 2, 3);
    private static final BlockPos HOME_POS = new BlockPos(1, 2, 1);
    private static final int STRESS_GOLEM_COUNT = 128;

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void staleRevisionPayloadsCannotMutateModeRunningOrLlm(GameTestHelper helper) {
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        ServerPlayer player = createBoundPlayer(helper, golem, GOLEM_POS.offset(1, 0, 0));
        try {
            CopperGolemData.migrate(golem);
            setTransportEnabled(golem, false);
            int revision = CopperGolemData.revision(golem);
            int staleRevision = revision - 1;

            CopperGolemWrenchHandler.setModeFromUi(
                    player, golem.getUUID(), CopperGolemMode.GATHERING.id(), staleRevision);
            require(helper, CopperGolemData.mode(golem) == CopperGolemMode.SORTING,
                    "Stale mode payload changed the authoritative mode");
            require(helper, CopperGolemData.revision(golem) == revision,
                    "Rejected stale mode payload advanced the revision");

            CopperGolemWrenchHandler.setModeFromUi(
                    player, golem.getUUID(), CopperGolemMode.GATHERING.id(), revision);
            require(helper, CopperGolemData.mode(golem) == CopperGolemMode.GATHERING,
                    "Current mode payload was rejected");
            int currentRevision = CopperGolemData.revision(golem);
            require(helper, currentRevision == revision + 1,
                    "Accepted mode payload did not advance the revision exactly once");

            CopperGolemWrenchHandler.setTransportEnabledFromUi(
                    player, golem.getUUID(), true, revision);
            require(helper, !CopperGolemData.running(golem),
                    "Stale running payload enabled the copper golem");
            require(helper, CopperGolemData.revision(golem) == currentRevision,
                    "Rejected stale running payload advanced the revision");

            CopperGolemWrenchHandler.setGatheringLlmFromUi(
                    player, golem.getUUID(), true, "stale prompt", revision);
            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            require(helper, !tag.getBooleanOr("deadrecall_gathering_llm_enabled", false)
                            && tag.getStringOr("deadrecall_gathering_llm_prompt", "").isBlank(),
                    "Stale LLM payload changed the current gathering configuration");
            require(helper, CopperGolemData.revision(golem) == currentRevision,
                    "Rejected stale LLM payload advanced the revision");
            helper.succeed();
        } finally {
            cleanup(golem, player);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 40)
    public void competingPlayersUsingSameRevisionOnlyFirstMutationWins(GameTestHelper helper) {
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        ServerPlayer first = createBoundPlayer(helper, golem, GOLEM_POS.offset(1, 0, 0));
        ServerPlayer second = createBoundPlayer(helper, golem, GOLEM_POS.offset(-1, 0, 0));
        try {
            CopperGolemData.migrate(golem);
            setTransportEnabled(golem, false);
            int sharedRevision = CopperGolemData.revision(golem);

            CopperGolemWrenchHandler.setModeFromUi(
                    first, golem.getUUID(), CopperGolemMode.GATHERING.id(), sharedRevision);
            CopperGolemWrenchHandler.setTransportEnabledFromUi(
                    second, golem.getUUID(), true, sharedRevision);

            require(helper, CopperGolemData.mode(golem) == CopperGolemMode.GATHERING,
                    "First same-tick mutation did not persist");
            require(helper, !CopperGolemData.running(golem),
                    "Second same-revision mutation was not rejected as stale");
            require(helper, CopperGolemData.revision(golem) == sharedRevision + 1,
                    "Competing same-revision mutations advanced the revision more than once");

            int nextRevision = CopperGolemData.revision(golem);
            CopperGolemWrenchHandler.setTransportEnabledFromUi(
                    first, golem.getUUID(), true, nextRevision);
            CopperGolemWrenchHandler.setGatheringLlmFromUi(
                    second, golem.getUUID(), true, "must remain stale", nextRevision);

            require(helper, CopperGolemData.running(golem),
                    "First running mutation in the second race was lost");
            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            require(helper, !tag.getBooleanOr("deadrecall_gathering_llm_enabled", false),
                    "Second mutation in the second race bypassed revision authority");
            require(helper, CopperGolemData.revision(golem) == nextRevision + 1,
                    "Second race advanced the revision more than once");
            helper.succeed();
        } finally {
            cleanup(golem, first, second);
        }
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 50)
    public void menuSlotClickRechecksLiveRunningAndModeState(GameTestHelper helper) {
        CopperGolem golem = createGolem(helper, GOLEM_POS);
        ServerPlayer menuPlayer = createBoundPlayer(helper, golem, GOLEM_POS.offset(1, 0, 0));
        ServerPlayer operator = createBoundPlayer(helper, golem, GOLEM_POS.offset(-1, 0, 0));
        CopperGolemMenu menu = null;
        try {
            CopperGolemData.migrate(golem);
            setTransportEnabled(golem, false);
            setMode(golem, CopperGolemMode.GATHERING);

            menu = new CopperGolemMenu(77, menuPlayer.getInventory(), menuPlayer, golem);
            menuPlayer.containerMenu = menu;
            Slot toolSlot = menu.getSlot(CopperGolemMenu.SLOT_GATHERING_TOOL);
            ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
            require(helper, toolSlot.isActive() && toolSlot.mayPlace(pickaxe),
                    "Stopped gathering menu did not initially permit a valid tool");

            CopperGolemWrenchHandler.setTransportEnabledFromUi(
                    operator, golem.getUUID(), true, CopperGolemData.revision(golem));
            require(helper, !toolSlot.mayPlace(pickaxe) && !toolSlot.mayPickup(menuPlayer),
                    "Open menu retained stale slot permissions after running was enabled");

            menu.setCarried(pickaxe.copy());
            menu.clicked(CopperGolemMenu.SLOT_GATHERING_TOOL, 0, ContainerInput.PICKUP, menuPlayer);
            require(helper, CopperGolemWrenchHandler.getGatheringToolStackForMenu(golem).isEmpty(),
                    "Running golem accepted a tool through an already-open menu");
            require(helper, menu.getCarried().is(Items.IRON_PICKAXE),
                    "Rejected running slot click consumed the player's carried tool");

            CopperGolemWrenchHandler.setTransportEnabledFromUi(
                    operator, golem.getUUID(), false, CopperGolemData.revision(golem));
            CopperGolemWrenchHandler.setModeFromUi(
                    operator, golem.getUUID(), CopperGolemMode.SORTING.id(), CopperGolemData.revision(golem));
            require(helper, !toolSlot.isActive() && !toolSlot.mayPlace(pickaxe),
                    "Open menu retained gathering slot authority after switching to sorting");

            menu.clicked(CopperGolemMenu.SLOT_GATHERING_TOOL, 0, ContainerInput.PICKUP, menuPlayer);
            require(helper, CopperGolemWrenchHandler.getGatheringToolStackForMenu(golem).isEmpty(),
                    "Sorting mode accepted a gathering tool through an already-open menu");
            helper.succeed();
        } finally {
            if (menu != null) {
                menu.removed(menuPlayer);
            }
            cleanup(golem, menuPlayer, operator);
        }
    }

    @GameTest(maxTicks = 120)
    public void scannerStressAndControllerCleanupRemainBounded(GameTestHelper helper) {
        helper.setBlock(HOME_POS, copperChest(helper));
        List<CopperGolem> golems = new ArrayList<>();
        Set<UUID> golemIds = new LinkedHashSet<>();
        try {
            for (int index = 0; index < STRESS_GOLEM_COUNT; index++) {
                BlockPos relativePos = new BlockPos(2 + (index % 16), 2, 2 + (index / 16));
                helper.setBlock(relativePos.below(), Blocks.STONE);
                CopperGolem golem = createGolem(helper, relativePos);
                golem.setNoAi(true);
                configureScannerStressGolem(helper, golem);
                CopperGolemController.track(golem);
                golems.add(golem);
                golemIds.add(golem.getUUID());
            }

            require(helper, trackedGolems().keySet().containsAll(golemIds),
                    "Controller did not retain all stress-fixture copper golems");
            CopperGolemController.tick(helper.getLevel().getServer());
            CopperGolemController.tick(helper.getLevel().getServer());
            require(helper, trackedGolems().keySet().containsAll(golemIds),
                    "Scanner pressure unexpectedly dropped live managed copper golems");

            for (CopperGolem golem : golems) {
                long scanIndex = CopperGolemData.readEntityTag(golem)
                        .getLongOr("deadrecall_gathering_scan_index", 0L);
                require(helper, scanIndex >= 0L && scanIndex <= 512L,
                        "Scanner cursor escaped the bounded 512-block fixture");
            }

            Set<UUID> discardedIds = new LinkedHashSet<>();
            for (int index = 0; index < 96; index++) {
                CopperGolem discarded = golems.get(index);
                discardedIds.add(discarded.getUUID());
                discarded.discard();
            }
            CopperGolemController.tick(helper.getLevel().getServer());
            require(helper, trackedGolems().keySet().stream().noneMatch(discardedIds::contains),
                    "Controller retained removed stress-fixture copper golems");

            Set<UUID> survivors = new LinkedHashSet<>(golemIds);
            survivors.removeAll(discardedIds);
            require(helper, trackedGolems().keySet().containsAll(survivors),
                    "Controller cleanup removed live stress-fixture copper golems");
            helper.succeed();
        } finally {
            for (CopperGolem golem : golems) {
                CopperGolemController.untrack(golem);
                golem.discard();
            }
            helper.setBlock(HOME_POS, Blocks.AIR);
            for (int index = 0; index < STRESS_GOLEM_COUNT; index++) {
                helper.setBlock(new BlockPos(2 + (index % 16), 1, 2 + (index / 16)), Blocks.AIR);
            }
        }
    }

    @GameTest(maxTicks = 620)
    public void controllerDropsUnloadedEntityAndRediscoversItWithinInterval(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RemoteFixture fixture = findUnloadedFixture(helper);
        level.setChunkForced(fixture.chunkX(), fixture.chunkZ(), true);
        level.getChunk(fixture.chunkX(), fixture.chunkZ());
        level.setBlockAndUpdate(fixture.golemPos().below(), Blocks.STONE.defaultBlockState());

        CopperGolem golem = constructGolem(helper, fixture.golemPos());
        golem.setPersistenceRequired();
        UUID golemId = golem.getUUID();
        configureStoppedManagedGolem(golem);
        CopperGolemController.track(golem);
        require(helper, trackedGolems().containsKey(golemId),
                "Controller did not initially track the remote managed copper golem");

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
                        CopperGolemController.tick(level.getServer());
                        require(helper, !trackedGolems().containsKey(golemId),
                                "Controller retained an entity after its chunk unloaded");
                        level.setChunkForced(fixture.chunkX(), fixture.chunkZ(), true);
                        level.getChunk(fixture.chunkX(), fixture.chunkZ());
                    }
                    return;
                }
                if (!level.isLoaded(fixture.golemPos())) {
                    return;
                }

                Entity loaded = level.getEntity(golemId);
                if (!(loaded instanceof CopperGolem reloaded)) {
                    return;
                }
                for (int discoveryTick = 0; discoveryTick < 20; discoveryTick++) {
                    CopperGolemController.tick(level.getServer());
                }
                require(helper, trackedGolems().containsKey(golemId),
                        "Controller did not rediscover the reloaded managed copper golem within 20 ticks");

                CopperGolemController.untrack(reloaded);
                reloaded.discard();
                level.setBlockAndUpdate(fixture.golemPos().below(), Blocks.AIR.defaultBlockState());
                level.setChunkForced(fixture.chunkX(), fixture.chunkZ(), false);
                helper.succeed();
            });
        }

        helper.runAtTickTime(605, () -> {
            level.setChunkForced(fixture.chunkX(), fixture.chunkZ(), false);
            CopperGolemController.untrack(golem);
            if (!observedUnloaded[0]) {
                throw helper.assertionException("Remote controller fixture chunk never unloaded");
            }
            throw helper.assertionException("Remote controller fixture did not reload before timeout");
        });
    }

    private static void configureScannerStressGolem(GameTestHelper helper, CopperGolem golem) {
        BlockPos areaA = helper.absolutePos(new BlockPos(24, 20, 24));
        BlockPos areaB = areaA.offset(15, 1, 15);
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        tag.putInt(CopperGolemData.TAG_DATA_VERSION, CopperGolemData.DATA_VERSION);
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.GATHERING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, true);
        SortingBindingService.writeSourceContainer(tag, new CopperGolemWrenchHandler.Binding(
                helper.getLevel().dimension(), helper.absolutePos(HOME_POS)));
        tag.putString("deadrecall_gathering_area_dim", helper.getLevel().dimension().identifier().toString());
        writePos(tag, areaA,
                "deadrecall_gathering_corner_a_x", "deadrecall_gathering_corner_a_y", "deadrecall_gathering_corner_a_z");
        writePos(tag, areaB,
                "deadrecall_gathering_corner_b_x", "deadrecall_gathering_corner_b_y", "deadrecall_gathering_corner_b_z");
        CopperGolemData.writeStringList(
                tag, "deadrecall_gathering_manual_targets", List.of("minecraft:diamond_ore"), 64);
        CopperGolemData.writeItemStack(
                tag, "deadrecall_gathering_tool_stack", new ItemStack(Items.IRON_PICKAXE));
        CopperGolemData.writeItemStack(
                tag, CopperGolemData.TAG_FUEL_STACK, new ItemStack(Items.COAL));
        CopperGolemData.writeEntityTag(golem, tag);
    }

    private static void configureStoppedManagedGolem(CopperGolem golem) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        tag.putInt(CopperGolemData.TAG_DATA_VERSION, CopperGolemData.DATA_VERSION);
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.GATHERING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, false);
        CopperGolemData.writeEntityTag(golem, tag);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer createBoundPlayer(
            GameTestHelper helper,
            CopperGolem golem,
            BlockPos relativePos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absolutePos = helper.absolutePos(relativePos);
        player.snapTo(absolutePos.getX() + 0.5D, absolutePos.getY(), absolutePos.getZ() + 0.5D, 0.0F, 0.0F);
        ItemStack wrench = new ItemStack(ModItems.COPPER_WRENCH);
        setSelectedGolem(wrench, golem.getUUID());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        return player;
    }

    private static CopperGolem createGolem(GameTestHelper helper, BlockPos relativePos) {
        return constructGolem(helper, helper.absolutePos(relativePos));
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
                golem.snapTo(absolutePos.getX() + 0.5D, absolutePos.getY(), absolutePos.getZ() + 0.5D, 0.0F, 0.0F);
                require(helper, helper.getLevel().addFreshEntity(golem),
                        "Could not add copper golem fixture to the GameTest level");
                return golem;
            }
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not construct copper golem fixture", exception);
        }
        throw helper.assertionException("No compatible CopperGolem constructor was found");
    }

    private static RemoteFixture findUnloadedFixture(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(GOLEM_POS);
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        for (int offset = 64; offset <= 512; offset += 32) {
            int chunkX = originChunkX + offset;
            int chunkZ = originChunkZ + offset;
            BlockPos golemPos = new BlockPos((chunkX << 4) + 4, 200, (chunkZ << 4) + 4);
            if (!level.isLoaded(golemPos)) {
                return new RemoteFixture(golemPos, chunkX, chunkZ);
            }
        }
        throw helper.assertionException("Could not locate an unloaded chunk for the controller fixture");
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

    private static void setTransportEnabled(CopperGolem golem, boolean enabled) {
        invokeWrench("setTransportEnabled", new Class<?>[]{CopperGolem.class, boolean.class}, golem, enabled);
    }

    private static void setMode(CopperGolem golem, CopperGolemMode mode) {
        invokeWrench("setMode", new Class<?>[]{CopperGolem.class, CopperGolemMode.class}, golem, mode);
    }

    private static void setSelectedGolem(ItemStack wrench, UUID golemId) {
        invokeWrench("setSelectedGolem", new Class<?>[]{ItemStack.class, UUID.class}, wrench, golemId);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ?> trackedGolems() {
        try {
            Field field = CopperGolemController.class.getDeclaredField("TRACKED_COPPER_GOLEMS");
            field.setAccessible(true);
            return (Map<UUID, ?>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not inspect the copper golem controller tracking map", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeWrench(String name, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = CopperGolemWrenchHandler.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(null, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not invoke CopperGolemWrenchHandler#" + name, exception);
        }
    }

    private static void cleanup(CopperGolem golem, ServerPlayer... players) {
        CopperGolemController.untrack(golem);
        golem.discard();
        for (ServerPlayer player : players) {
            player.discard();
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    private record RemoteFixture(BlockPos golemPos, int chunkX, int chunkZ) {
    }
}
