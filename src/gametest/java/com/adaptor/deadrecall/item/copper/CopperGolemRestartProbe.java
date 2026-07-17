package com.adaptor.deadrecall.item.copper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Server-only Copper Golem world persistence probe executed across separate Dedicated Server JVMs.
 */
public final class CopperGolemRestartProbe implements ModInitializer {
    private static final String PHASE_ENV = "DEADRECALL_COPPER_RESTART_PROBE_PHASE";
    private static final String MARKER_DIRECTORY_ENV = "DEADRECALL_COPPER_RESTART_PROBE_MARKER_DIR";
    private static final String UUID_FILE = "copper-golem.uuid";
    private static final String PROBE_MARKER_TAG = "deadrecall_copper_restart_probe";
    private static final BlockPos GOLEM_POS = new BlockPos(164, 200, 164);
    private static final BlockPos HOME_POS = GOLEM_POS.offset(2, 0, 0);
    private static final BlockPos TARGET_POS = GOLEM_POS.offset(4, 0, 0);
    private static final int CHUNK_X = SectionPos.blockToSectionCoord(GOLEM_POS.getX());
    private static final int CHUNK_Z = SectionPos.blockToSectionCoord(GOLEM_POS.getZ());
    private static final int LOAD_SETTLE_TICKS = 120;
    private static final int SAVE_SETTLE_TICKS = 80;
    private static final Component SEED_TOOL_NAME = Component.literal("Copper restart probe tool");
    private static final Component SEED_STORAGE_NAME = Component.literal("Copper restart probe storage");
    private static final Component RECOVERED_STORAGE_NAME = Component.literal("Recovered copper restart storage");

    @Override
    public void onInitialize() {
        String phase = System.getenv(PHASE_ENV);
        if (phase == null || phase.isBlank()) {
            return;
        }
        Path markerDirectory = markerDirectory();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel level = server.overworld();
            level.setChunkForced(CHUNK_X, CHUNK_Z, true);
            level.getChunk(CHUNK_X, CHUNK_Z);
            ProbeSession session = new ProbeSession(phase, markerDirectory);
            ServerTickEvents.END_SERVER_TICK.register(session::tick);
        });
    }

    private static void runPhase(MinecraftServer server, String phase, Path markerDirectory) {
        ServerLevel level = server.overworld();
        switch (phase) {
            case "seed" -> seed(level, markerDirectory);
            case "recover" -> recover(level, markerDirectory);
            case "verify" -> verify(level, markerDirectory);
            default -> throw new IllegalArgumentException("Unknown copper restart probe phase: " + phase);
        }
    }

    private static void seed(ServerLevel level, Path markerDirectory) {
        require(findMarkedGolem(level) == null, "Seed phase found a stale copper golem probe entity");
        prepareWorld(level);

        CopperGolem golem = constructGolem(level);
        golem.setPersistenceRequired();
        configureSeedState(level, golem);
        require(level.addFreshEntity(golem), "Seed phase could not add the copper golem probe entity");
        writeMarker(markerDirectory, UUID_FILE, golem.getUUID().toString() + "\n");

        Container home = homeContainer(level);
        home.setItem(0, new ItemStack(Items.EMERALD, 3));
        home.setChanged();
        verifySeedState(level, golem);
    }

    private static void recover(ServerLevel level, Path markerDirectory) {
        UUID golemId = readUuid(markerDirectory);
        CopperGolem golem = requireGolem(level, golemId);
        verifySeedState(level, golem);

        ItemStack tool = CopperGolemWrenchHandler.getGatheringToolStackForMenu(golem);
        tool.setDamageValue(23);
        CopperGolemData.writeItemStack(updateTag(golem), "deadrecall_gathering_tool_stack", tool);

        ItemStack storage = new ItemStack(Items.COBBLESTONE, 7);
        storage.set(DataComponents.CUSTOM_NAME, RECOVERED_STORAGE_NAME);
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        tag.putInt(CopperGolemData.TAG_REVISION, 78);
        tag.putString(CopperGolemData.TAG_ACTIVITY, CopperGolemActivity.BLOCKED_HOME_UNAVAILABLE.id());
        tag.putInt(CopperGolemData.TAG_FUEL_TICKS, 111);
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_tool_stack", tool);
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_storage_stack", storage);
        writePos(tag, TARGET_POS.above(),
                "deadrecall_gathering_target_x",
                "deadrecall_gathering_target_y",
                "deadrecall_gathering_target_z");
        tag.putLong("deadrecall_gathering_scan_index", 144L);
        CopperGolemData.writeEntityTag(golem, tag);

        require(level.setBlockAndUpdate(TARGET_POS, Blocks.DEEPSLATE.defaultBlockState()),
                "Recover phase could not update the probe target block");
        Container home = homeContainer(level);
        home.setItem(0, new ItemStack(Items.DIAMOND, 5));
        home.setChanged();
        verifyRecoveredState(level, golem);
    }

    private static void verify(ServerLevel level, Path markerDirectory) {
        CopperGolem golem = requireGolem(level, readUuid(markerDirectory));
        verifyRecoveredState(level, golem);
        level.setChunkForced(CHUNK_X, CHUNK_Z, false);
    }

    private static void prepareWorld(ServerLevel level) {
        require(level.setBlockAndUpdate(GOLEM_POS.below(), Blocks.STONE.defaultBlockState()),
                "Could not create the copper restart probe floor");
        require(level.setBlockAndUpdate(HOME_POS, copperChest().defaultBlockState()),
                "Could not create the copper restart probe Home");
        require(level.setBlockAndUpdate(TARGET_POS, Blocks.STONE.defaultBlockState()),
                "Could not create the copper restart probe target");
    }

    private static void configureSeedState(ServerLevel level, CopperGolem golem) {
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        tool.setDamageValue(9);
        tool.set(DataComponents.CUSTOM_NAME, SEED_TOOL_NAME);
        ItemStack storage = new ItemStack(Items.COBBLESTONE, 12);
        storage.set(DataComponents.CUSTOM_NAME, SEED_STORAGE_NAME);

        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        tag.putBoolean(PROBE_MARKER_TAG, true);
        tag.putInt(CopperGolemData.TAG_DATA_VERSION, CopperGolemData.DATA_VERSION);
        tag.putInt(CopperGolemData.TAG_REVISION, 77);
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.GATHERING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, false);
        tag.putString(CopperGolemData.TAG_ACTIVITY, CopperGolemActivity.RETURNING_HOME.id());
        CopperGolemData.writeItemStack(tag, CopperGolemData.TAG_FUEL_STACK, new ItemStack(Items.COAL, 2));
        tag.putInt(CopperGolemData.TAG_FUEL_TICKS, 321);
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_tool_stack", tool);
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_storage_stack", storage);
        SortingBindingService.writeSourceContainer(tag,
                new CopperGolemWrenchHandler.Binding(level.dimension(), HOME_POS));
        tag.putString("deadrecall_gathering_area_dim", level.dimension().identifier().toString());
        writePos(tag, TARGET_POS,
                "deadrecall_gathering_corner_a_x",
                "deadrecall_gathering_corner_a_y",
                "deadrecall_gathering_corner_a_z");
        writePos(tag, TARGET_POS.offset(1, 2, 1),
                "deadrecall_gathering_corner_b_x",
                "deadrecall_gathering_corner_b_y",
                "deadrecall_gathering_corner_b_z");
        writePos(tag, TARGET_POS,
                "deadrecall_gathering_target_x",
                "deadrecall_gathering_target_y",
                "deadrecall_gathering_target_z");
        tag.putLong("deadrecall_gathering_scan_index", 73L);
        CopperGolemData.writeStringList(tag,
                "deadrecall_gathering_manual_targets",
                List.of("minecraft:stone", "minecraft:deepslate"),
                64);
        CopperGolemData.writeEntityTag(golem, tag);
    }

    private static void verifySeedState(ServerLevel level, CopperGolem golem) {
        require(CopperGolemWrenchHandler.getMode(golem) == CopperGolemMode.GATHERING,
                "Seed state lost gathering mode");
        require(!CopperGolemWrenchHandler.isTransportEnabled(golem),
                "Seed state did not preserve stopped state");
        require(CopperGolemData.revision(golem) == 77,
                "Seed state revision was not persisted");
        require(CopperGolemData.activity(golem) == CopperGolemActivity.RETURNING_HOME,
                "Seed state activity was not persisted");
        require(CopperGolemWrenchHandler.getSourceContainer(golem)
                        .filter(binding -> binding.dimension().equals(level.dimension()))
                        .filter(binding -> binding.containerPos().equals(HOME_POS))
                        .isPresent(),
                "Seed state Home binding was not persisted");

        ItemStack tool = CopperGolemWrenchHandler.getGatheringToolStackForMenu(golem);
        require(tool.is(Items.IRON_PICKAXE) && tool.getDamageValue() == 9,
                "Seed state tool durability was not persisted");
        require(SEED_TOOL_NAME.equals(tool.get(DataComponents.CUSTOM_NAME)),
                "Seed state tool components were not persisted");
        ItemStack storage = CopperGolemWrenchHandler.getGatheringStorageStackForMenu(golem);
        require(storage.is(Items.COBBLESTONE) && storage.getCount() == 12,
                "Seed state storage quantity was not persisted");
        require(SEED_STORAGE_NAME.equals(storage.get(DataComponents.CUSTOM_NAME)),
                "Seed state storage components were not persisted");
        require(CopperGolemData.fuelStack(golem).is(Items.COAL)
                        && CopperGolemData.fuelStack(golem).getCount() == 2
                        && CopperGolemData.fuelTicks(golem) == 321,
                "Seed state fuel was not persisted");

        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        require(tag.getBooleanOr(PROBE_MARKER_TAG, false), "Copper restart marker was lost");
        require(tag.getLongOr("deadrecall_gathering_scan_index", -1L) == 73L,
                "Seed state scan cursor was not persisted");
        require(readPos(tag,
                        "deadrecall_gathering_target_x",
                        "deadrecall_gathering_target_y",
                        "deadrecall_gathering_target_z").equals(TARGET_POS),
                "Seed state target was not persisted");
        require(homeContainer(level).getItem(0).is(Items.EMERALD)
                        && homeContainer(level).getItem(0).getCount() == 3,
                "Seed Home inventory was not persisted");
        require(level.getBlockState(TARGET_POS).is(Blocks.STONE),
                "Seed target block was not persisted");
    }

    private static void verifyRecoveredState(ServerLevel level, CopperGolem golem) {
        require(CopperGolemData.revision(golem) == 78,
                "Recovered revision was not persisted");
        require(CopperGolemData.activity(golem) == CopperGolemActivity.BLOCKED_HOME_UNAVAILABLE,
                "Recovered activity was not persisted");
        ItemStack tool = CopperGolemWrenchHandler.getGatheringToolStackForMenu(golem);
        require(tool.is(Items.IRON_PICKAXE) && tool.getDamageValue() == 23,
                "Recovered tool durability was not persisted");
        require(SEED_TOOL_NAME.equals(tool.get(DataComponents.CUSTOM_NAME)),
                "Recovered tool components were not persisted");
        ItemStack storage = CopperGolemWrenchHandler.getGatheringStorageStackForMenu(golem);
        require(storage.is(Items.COBBLESTONE) && storage.getCount() == 7,
                "Recovered storage quantity was not persisted");
        require(RECOVERED_STORAGE_NAME.equals(storage.get(DataComponents.CUSTOM_NAME)),
                "Recovered storage components were not persisted");
        require(CopperGolemData.fuelTicks(golem) == 111,
                "Recovered fuel ticks were not persisted");

        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        require(tag.getLongOr("deadrecall_gathering_scan_index", -1L) == 144L,
                "Recovered scan cursor was not persisted");
        require(readPos(tag,
                        "deadrecall_gathering_target_x",
                        "deadrecall_gathering_target_y",
                        "deadrecall_gathering_target_z").equals(TARGET_POS.above()),
                "Recovered target was not persisted");
        require(homeContainer(level).getItem(0).is(Items.DIAMOND)
                        && homeContainer(level).getItem(0).getCount() == 5,
                "Recovered Home inventory was not persisted");
        require(level.getBlockState(TARGET_POS).is(Blocks.DEEPSLATE),
                "Recovered target block was not persisted");
    }

    private static CompoundTag updateTag(CopperGolem golem) {
        return CopperGolemData.readEntityTag(golem);
    }

    private static CopperGolem requireGolem(ServerLevel level, UUID golemId) {
        Entity entity = level.getEntity(golemId);
        if (entity instanceof CopperGolem golem) {
            return golem;
        }
        throw new IllegalStateException("No copper golem probe entity was loaded for UUID " + golemId);
    }

    private static CopperGolem findMarkedGolem(ServerLevel level) {
        AABB area = new AABB(GOLEM_POS).inflate(16.0D);
        return level.getEntitiesOfClass(CopperGolem.class, area,
                        golem -> CopperGolemData.readEntityTag(golem).getBooleanOr(PROBE_MARKER_TAG, false))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static CopperGolem constructGolem(ServerLevel level) {
        Object entityType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_golem"));
        require(entityType != null, "Missing minecraft:copper_golem entity type");
        try {
            for (Constructor<?> constructor : CopperGolem.class.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 2
                        || !parameterTypes[0].isInstance(entityType)
                        || !parameterTypes[1].isInstance(level)) {
                    continue;
                }
                constructor.setAccessible(true);
                CopperGolem golem = (CopperGolem) constructor.newInstance(entityType, level);
                golem.snapTo(GOLEM_POS.getX() + 0.5D, GOLEM_POS.getY(), GOLEM_POS.getZ() + 0.5D, 0.0F, 0.0F);
                return golem;
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not construct copper golem restart probe", exception);
        }
        throw new IllegalStateException("No compatible CopperGolem constructor was found");
    }

    private static Container homeContainer(ServerLevel level) {
        Object blockEntity = level.getBlockEntity(HOME_POS);
        if (blockEntity instanceof Container container) {
            return container;
        }
        throw new IllegalStateException("Copper restart probe Home is not a container");
    }

    private static Block copperChest() {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_chest"));
        require(block != null && block != Blocks.AIR, "Missing minecraft:copper_chest block");
        return block;
    }

    private static Path markerDirectory() {
        String configured = System.getenv(MARKER_DIRECTORY_ENV);
        if (configured == null || configured.isBlank()) {
            return Path.of("copper-restart-probe").toAbsolutePath().normalize();
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    private static UUID readUuid(Path markerDirectory) {
        try {
            return UUID.fromString(Files.readString(markerDirectory.resolve(UUID_FILE), StandardCharsets.UTF_8).trim());
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not read copper restart probe UUID", exception);
        }
    }

    private static void writeMarker(Path markerDirectory, String fileName, String content) {
        try {
            Files.createDirectories(markerDirectory);
            Files.writeString(markerDirectory.resolve(fileName), content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write copper restart probe marker " + fileName, exception);
        }
    }

    private static void writePos(CompoundTag tag, BlockPos pos, String xKey, String yKey, String zKey) {
        tag.putInt(xKey, pos.getX());
        tag.putInt(yKey, pos.getY());
        tag.putInt(zKey, pos.getZ());
    }

    private static BlockPos readPos(CompoundTag tag, String xKey, String yKey, String zKey) {
        return new BlockPos(tag.getIntOr(xKey, Integer.MIN_VALUE),
                tag.getIntOr(yKey, Integer.MIN_VALUE),
                tag.getIntOr(zKey, Integer.MIN_VALUE));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class ProbeSession {
        private final String phase;
        private final Path markerDirectory;
        private int ticksRemaining = LOAD_SETTLE_TICKS;
        private boolean phaseExecuted;
        private boolean finished;

        private ProbeSession(String phase, Path markerDirectory) {
            this.phase = phase;
            this.markerDirectory = markerDirectory;
        }

        private void tick(MinecraftServer server) {
            if (finished || --ticksRemaining > 0) {
                return;
            }
            if (!phaseExecuted) {
                try {
                    runPhase(server, phase, markerDirectory);
                    phaseExecuted = true;
                    ticksRemaining = SAVE_SETTLE_TICKS;
                    return;
                } catch (Throwable throwable) {
                    fail(server, throwable);
                    return;
                }
            }
            try {
                writeMarker(markerDirectory, phase + ".ok", "success\n");
                finished = true;
                server.halt(false);
            } catch (Throwable throwable) {
                fail(server, throwable);
            }
        }

        private void fail(MinecraftServer server, Throwable throwable) {
            finished = true;
            try {
                writeMarker(markerDirectory, phase + ".failure", throwable.toString() + "\n");
            } catch (RuntimeException markerFailure) {
                throwable.addSuppressed(markerFailure);
            } finally {
                server.halt(false);
            }
            throw new IllegalStateException("Copper Golem restart probe failed in phase " + phase, throwable);
        }
    }
}
