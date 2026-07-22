package dev.totem.remnant.gametest;

import dev.totem.remnant.death.DeathBackpackNodeBinding;
import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Server-only two-JVM restart probe for persisted Remnant death backpack data. */
public final class RemnantRestartProbe implements ModInitializer {
    private static final String PHASE_ENV = "TOTEM_REMNANT_RESTART_PROBE_PHASE";
    private static final String MARKER_DIRECTORY_ENV = "TOTEM_REMNANT_RESTART_PROBE_MARKER_DIR";
    private static final BlockPos POS = new BlockPos(8, 200, 8);
    private static final UUID NODE_ID = UUID.fromString("d5cbf77d-6a2e-42ad-8946-3fcfa61ee9cb");
    private static final int CHUNK_X = SectionPos.blockToSectionCoord(POS.getX());
    private static final int CHUNK_Z = SectionPos.blockToSectionCoord(POS.getZ());

    @Override
    public void onInitialize() {
        String phase = System.getenv(PHASE_ENV);
        if (phase == null || phase.isBlank()) return;
        Path markerDirectory = markerDirectory();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel level = server.overworld();
            level.setChunkForced(CHUNK_X, CHUNK_Z, true);
            level.getChunk(POS);
            ServerTickEvents.END_SERVER_TICK.register(new Session(phase, markerDirectory)::tick);
        });
    }

    private static Path markerDirectory() {
        String configured = System.getenv(MARKER_DIRECTORY_ENV);
        return configured == null || configured.isBlank()
                ? Path.of("remnant-restart-probe").toAbsolutePath().normalize()
                : Path.of(configured).toAbsolutePath().normalize();
    }

    private static void runPhase(MinecraftServer server, String phase) {
        ServerLevel level = server.overworld();
        if ("seed".equals(phase)) {
            require(backpackSlot(level).isEmpty(), "Seed phase found a stale Remnant death backpack");
            level.setBlockAndUpdate(POS.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(POS, Blocks.CHEST.defaultBlockState());
            ItemStack backpack = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
            backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 11))));
            DeathBackpackNodeBinding.write(backpack, NODE_ID);
            chest(level).setItem(0, backpack);
            return;
        }
        if ("verify".equals(phase)) {
            ItemStack backpack = requireBackpack(level);
            List<ItemStack> contents = backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .nonEmptyItemCopyStream().toList();
            require(contents.size() == 1 && contents.getFirst().is(Items.DIAMOND) && contents.getFirst().getCount() == 11,
                    "Restart did not preserve Remnant death backpack contents");
            require(NODE_ID.equals(DeathBackpackNodeBinding.read(backpack)),
                    "Restart did not preserve Remnant death backpack node binding");
            chest(level).setItem(0, ItemStack.EMPTY);
            level.setChunkForced(CHUNK_X, CHUNK_Z, false);
            return;
        }
        throw new IllegalArgumentException("Unknown Remnant restart probe phase: " + phase);
    }

    private static ItemStack requireBackpack(ServerLevel level) {
        ItemStack backpack = backpackSlot(level);
        if (backpack.isEmpty() || !backpack.is(RemnantItemRegistration.DEATH_BACKPACK)) {
            throw new IllegalStateException("Restart did not reload the Remnant death backpack stack");
        }
        return backpack;
    }

    private static ItemStack backpackSlot(ServerLevel level) {
        return level.getBlockState(POS).is(Blocks.CHEST) ? chest(level).getItem(0) : ItemStack.EMPTY;
    }

    private static ChestBlockEntity chest(ServerLevel level) {
        if (!(level.getBlockEntity(POS) instanceof ChestBlockEntity chest)) {
            throw new IllegalStateException("Restart probe chest was not available");
        }
        return chest;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void writeMarker(Path markerDirectory, String name, String content) {
        try {
            Files.createDirectories(markerDirectory);
            Files.writeString(markerDirectory.resolve(name), content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write restart probe marker " + name, exception);
        }
    }

    private static final class Session {
        private final String phase;
        private final Path markerDirectory;
        private int ticksRemaining = 100;
        private boolean executed;

        private Session(String phase, Path markerDirectory) {
            this.phase = phase;
            this.markerDirectory = markerDirectory;
        }

        private void tick(MinecraftServer server) {
            if (--ticksRemaining > 0) return;
            try {
                if (!executed) {
                    runPhase(server, phase);
                    executed = true;
                    ticksRemaining = 40;
                    return;
                }
                if ("seed".equals(phase)) {
                    require(!backpackSlot(server.overworld()).isEmpty(),
                            "Seed phase lost the Remnant death backpack before shutdown");
                }
                writeMarker(markerDirectory, phase + ".ok", "success\n");
                server.halt(false);
            } catch (Throwable throwable) {
                writeMarker(markerDirectory, phase + ".failure", throwable + "\n");
                server.halt(false);
                throw new IllegalStateException("Remnant restart probe failed in phase " + phase, throwable);
            }
        }
    }
}
