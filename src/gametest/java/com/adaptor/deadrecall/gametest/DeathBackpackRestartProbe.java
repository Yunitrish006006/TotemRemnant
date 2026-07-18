package com.adaptor.deadrecall.gametest;

import com.adaptor.deadrecall.inventory.BackpackInventory;
import com.adaptor.deadrecall.item.BackpackItemHelper;
import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.mixin.DeadRecallSpaceUnitSavedDataAccessor;
import com.adaptor.deadrecall.space.DeadRecallSpaceDiscoverySavedData;
import com.adaptor.deadrecall.space.DeadRecallSpaceUnitSavedData;
import com.adaptor.deadrecall.space.SpaceUnitHandler;
import com.adaptor.deadrecall.space.SpaceUnitRecord;
import com.adaptor.deadrecall.space.SpaceUnitStatus;
import com.adaptor.deadrecall.space.SpaceUnitType;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-only integration probe invoked by CI across separate normal Dedicated Server JVMs.
 *
 * <p>The probe is part of the gametest source set and is never packaged in the production mod.
 * Enable one phase with {@code DEADRECALL_RESTART_PROBE_PHASE=seed|recover|verify}.</p>
 */
public final class DeathBackpackRestartProbe implements ModInitializer {
    private static final String PHASE_ENV = "DEADRECALL_RESTART_PROBE_PHASE";
    private static final String MARKER_DIRECTORY_ENV = "DEADRECALL_RESTART_PROBE_MARKER_DIR";
    private static final UUID OWNER_ID = UUID.fromString("6b2fac01-f28d-43fd-b729-5aca6521bb56");
    private static final BlockPos PROBE_POS = new BlockPos(8, 200, 8);
    private static final BlockPos CATALYST_PROBE_POS = new BlockPos(12, 200, 12);
    private static final List<BlockPos> CATALYST_OFFSETS = List.of(
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1)
    );
    private static final int PROBE_CHUNK_X = SectionPos.blockToSectionCoord(PROBE_POS.getX());
    private static final int PROBE_CHUNK_Z = SectionPos.blockToSectionCoord(PROBE_POS.getZ());
    private static final String BACKPACK_ID_TAG = "deadrecall_death_backpack_id";
    private static final int LOAD_SETTLE_TICKS = 100;
    private static final int SAVE_SETTLE_TICKS = 40;

    @Override
    public void onInitialize() {
        String phase = System.getenv(PHASE_ENV);
        if (phase == null || phase.isBlank()) {
            return;
        }
        Path markerDirectory = markerDirectory();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel level = server.overworld();
            level.setChunkForced(PROBE_CHUNK_X, PROBE_CHUNK_Z, true);
            level.getChunk(PROBE_POS);
            ProbeSession session = new ProbeSession(phase, markerDirectory);
            ServerTickEvents.END_SERVER_TICK.register(session::tick);
        });
        if ("seed".equals(phase)) {
            ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
                try {
                    rewriteCatalystSnapshotAsLegacy(server, markerDirectory);
                } catch (Throwable throwable) {
                    writeMarker(markerDirectory, "seed.failure", throwable.toString() + "\n");
                    throw new IllegalStateException("Could not prepare legacy catalyst snapshot", throwable);
                }
            });
        }
    }

    private static Path markerDirectory() {
        String configured = System.getenv(MARKER_DIRECTORY_ENV);
        if (configured == null || configured.isBlank()) {
            return Path.of("restart-probe").toAbsolutePath().normalize();
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    private static void runPhase(MinecraftServer server, String phase) {
        ServerLevel level = server.overworld();
        switch (phase) {
            case "seed" -> seed(level);
            case "recover" -> recover(server, level);
            case "verify" -> verify(level);
            default -> throw new IllegalArgumentException("Unknown restart probe phase: " + phase);
        }
    }

    private static void seed(ServerLevel level) {
        require(findProbeNode(level) == null, "Seed phase found a stale probe death node");
        require(findProbeBackpack(level) == null, "Seed phase found a stale probe death backpack");

        ServerPlayer owner = detachedPlayer(level.getServer(), level, OWNER_ID);
        DeadRecallSpaceUnitSavedData units = units(level);
        DeadRecallSpaceDiscoverySavedData discovery = discovery(level);
        SpaceUnitRecord node = units.createDeathUnit(level, PROBE_POS, owner);
        discovery.markDiscovered(OWNER_ID, node.id());

        level.setBlockAndUpdate(CATALYST_PROBE_POS, Blocks.LODESTONE.defaultBlockState());
        for (BlockPos offset : CATALYST_OFFSETS) {
            level.setBlockAndUpdate(
                    CATALYST_PROBE_POS.offset(offset),
                    Blocks.AMETHYST_BLOCK.defaultBlockState()
            );
        }
        SpaceUnitRecord catalystProbe = units.getOrCreateLodestone(level, CATALYST_PROBE_POS, owner);
        require(catalystProbe.structure().amethystCatalystBlocks() == 4,
                "Seed phase did not persist four catalyst blocks before legacy rewrite");

        ItemStack deathBackpack = new ItemStack(ModItems.DEATH_BACKPACK);
        CompoundTag customData = new CompoundTag();
        customData.putString(BACKPACK_ID_TAG, UUID.randomUUID().toString());
        deathBackpack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        deathBackpack.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 11)))
        );
        SpaceUnitHandler.writeDeathNodeBinding(deathBackpack, node.id());

        ItemEntity entity = new ItemEntity(
                level,
                PROBE_POS.getX() + 0.5,
                PROBE_POS.getY() + 0.5,
                PROBE_POS.getZ() + 0.5,
                deathBackpack
        );
        entity.setUnlimitedLifetime();
        require(level.addFreshEntity(entity), "Seed phase could not add the probe death backpack entity");
        require(node.status() == SpaceUnitStatus.ACTIVE, "Seed phase did not create an ACTIVE death node");
    }

    private static void recover(MinecraftServer server, ServerLevel level) {
        SpaceUnitRecord node = requireProbeNode(level, SpaceUnitStatus.ACTIVE);
        ItemEntity entity = requireProbeBackpack(level);
        require(storedItems(entity.getItem()).stream().anyMatch(stack ->
                        stack.is(Items.DIAMOND) && stack.getCount() == 11),
                "Recover phase loaded a probe backpack with incorrect contents");

        ItemStack backpackStack = entity.getItem();
        entity.discard();
        ServerPlayer replacementOwner = detachedPlayer(server, level, OWNER_ID);
        replacementOwner.setItemInHand(InteractionHand.MAIN_HAND, backpackStack);
        BackpackInventory backpack = new BackpackInventory(replacementOwner, InteractionHand.MAIN_HAND, 9);
        backpack.clearContent();
        backpack.onClose(replacementOwner);

        require(replacementOwner.getMainHandItem().isEmpty(),
                "Recover phase left the empty death backpack on the replacement player");
        SpaceUnitRecord recovered = units(level).get(node.id())
                .orElseThrow(() -> new IllegalStateException("Recover phase lost the probe death node"));
        require(recovered.status() == SpaceUnitStatus.DISABLED,
                "Recover phase did not persistently disable the probe death node");
        require(discovery(level).hasDiscovered(OWNER_ID, node.id()),
                "Recover phase lost the probe discovery reference");

        SpaceUnitRecord legacyCatalystProbe = requireCatalystProbe(level);
        require(legacyCatalystProbe.structure().amethystCatalystBlocks() == 0,
                "Recover phase did not load the missing legacy catalyst field as zero");
        SpaceUnitRecord refreshedCatalystProbe = units(level).rescanLodestone(level, legacyCatalystProbe.id())
                .orElseThrow(() -> new IllegalStateException("Recover phase could not rescan catalyst probe"));
        require(refreshedCatalystProbe.structure().amethystCatalystBlocks() == 4,
                "Recover phase did not restore catalyst count from the live structure scan");
    }

    private static void verify(ServerLevel level) {
        SpaceUnitRecord node = requireProbeNode(level, SpaceUnitStatus.DISABLED);
        require(node.owner().equals(OWNER_ID), "Verify phase loaded the probe node with a different owner");
        require(node.type() == SpaceUnitType.DEATH, "Verify phase loaded the probe node with a different type");
        require(discovery(level).hasDiscovered(OWNER_ID, node.id()),
                "Verify phase did not reload the probe discovery reference");
        require(findProbeBackpack(level) == null,
                "Verify phase reloaded a death backpack that was removed before the previous shutdown");
        require(requireCatalystProbe(level).structure().amethystCatalystBlocks() == 4,
                "Verify phase did not persist the rescanned catalyst count");
        units(level).disableLodestone(level.dimension(), CATALYST_PROBE_POS, level.getGameTime());
        level.setChunkForced(PROBE_CHUNK_X, PROBE_CHUNK_Z, false);
    }

    private static SpaceUnitRecord requireCatalystProbe(ServerLevel level) {
        return unitRecords(level).values().stream()
                .filter(SpaceUnitRecord::isLodestoneAnchor)
                .filter(unit -> unit.owner().equals(OWNER_ID))
                .filter(unit -> unit.pos().equals(CATALYST_PROBE_POS))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No catalyst migration probe was loaded"));
    }

    private static void rewriteCatalystSnapshotAsLegacy(
            MinecraftServer server,
            Path markerDirectory
    ) throws IOException {
        Path savedDataPath = server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions")
                .resolve("minecraft")
                .resolve("overworld")
                .resolve("data")
                .resolve("deadrecall")
                .resolve("space_units.dat");
        require(Files.isRegularFile(savedDataPath),
                "Seed shutdown did not produce Space Unit SavedData at " + savedDataPath);

        CompoundTag root = NbtIo.readCompressed(savedDataPath, NbtAccounter.unlimitedHeap());
        CompoundTag data = root.getCompound("data")
                .orElseThrow(() -> new IllegalStateException("Space Unit SavedData has no data compound"));
        ListTag records = data.getList("units")
                .orElseThrow(() -> new IllegalStateException("Space Unit SavedData has no units list"));
        boolean removed = false;
        for (int index = 0; index < records.size(); index++) {
            CompoundTag record = records.getCompoundOrEmpty(index);
            SpaceUnitRecord decoded = SpaceUnitRecord.CODEC.parse(NbtOps.INSTANCE, record).getOrThrow();
            if (!decoded.isLodestoneAnchor()
                    || !decoded.owner().equals(OWNER_ID)
                    || !decoded.pos().equals(CATALYST_PROBE_POS)) {
                continue;
            }
            CompoundTag structure = record.getCompound("structure")
                    .orElseThrow(() -> new IllegalStateException("Catalyst probe has no structure compound"));
            if (structure.remove("amethyst_catalyst_blocks") != null) {
                removed = true;
                break;
            }
        }
        require(removed, "Seed shutdown could not remove catalyst field from persisted snapshot");
        NbtIo.writeCompressed(root, savedDataPath);
        writeMarker(markerDirectory, "legacy-catalyst-snapshot.ok", "success\n");
    }

    private static SpaceUnitRecord requireProbeNode(ServerLevel level, SpaceUnitStatus expectedStatus) {
        SpaceUnitRecord node = findProbeNode(level);
        if (node == null) {
            throw new IllegalStateException("No probe death node was loaded");
        }
        require(node.status() == expectedStatus,
                "Probe death node status was " + node.status() + ", expected " + expectedStatus);
        return node;
    }

    private static SpaceUnitRecord findProbeNode(ServerLevel level) {
        return unitRecords(level).values().stream()
                .filter(unit -> unit.type() == SpaceUnitType.DEATH)
                .filter(unit -> unit.owner().equals(OWNER_ID))
                .findFirst()
                .orElse(null);
    }

    private static ItemEntity requireProbeBackpack(ServerLevel level) {
        ItemEntity entity = findProbeBackpack(level);
        if (entity == null) {
            throw new IllegalStateException("No probe death backpack entity was loaded");
        }
        return entity;
    }

    private static ItemEntity findProbeBackpack(ServerLevel level) {
        AABB fullSpawnChunkHeight = new AABB(
                -64.0D,
                -2048.0D,
                -64.0D,
                80.0D,
                2048.0D,
                80.0D
        );
        return level.getEntitiesOfClass(
                        ItemEntity.class,
                        fullSpawnChunkHeight,
                        entity -> entity.isAlive() && BackpackItemHelper.isDeathBackpackItem(entity.getItem())
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static List<ItemStack> storedItems(ItemStack backpack) {
        return backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream()
                .toList();
    }

    private static ServerPlayer detachedPlayer(
            MinecraftServer server,
            ServerLevel level,
            UUID playerId
    ) {
        return new ServerPlayer(
                server,
                level,
                new GameProfile(playerId, "restart-probe-owner"),
                ClientInformation.createDefault()
        );
    }

    private static DeadRecallSpaceUnitSavedData units(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(DeadRecallSpaceUnitSavedData.TYPE);
    }

    private static DeadRecallSpaceDiscoverySavedData discovery(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(DeadRecallSpaceDiscoverySavedData.TYPE);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, SpaceUnitRecord> unitRecords(ServerLevel level) {
        return ((DeadRecallSpaceUnitSavedDataAccessor) (Object) units(level)).deadrecall$getUnitsById();
    }

    private static void writeMarker(Path markerDirectory, String fileName, String content) {
        try {
            Files.createDirectories(markerDirectory);
            Files.writeString(
                    markerDirectory.resolve(fileName),
                    content,
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write restart probe marker " + fileName, exception);
        }
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
                    runPhase(server, phase);
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
            throw new IllegalStateException("Death-backpack restart probe failed in phase " + phase, throwable);
        }
    }
}
