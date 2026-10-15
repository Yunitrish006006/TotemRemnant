package dev.totem.remnant.gametest;

import dev.totem.remnant.death.DeathBackpackNodeBinding;
import dev.totem.remnant.death.SoulboundDeathItemRestartProbeAccess;
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
    private static final UUID SOULBOUND_PLAYER_ID =
            UUID.fromString("0eed6ddb-a3bc-4d10-a8a0-38c8f90b57b8");
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
            seedActions(server, level);
            SoulboundDeathItemRestartProbeAccess.seed(server, SOULBOUND_PLAYER_ID);
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
            verifyActions(level);
            SoulboundDeathItemRestartProbeAccess.verifyAndRemove(server, SOULBOUND_PLAYER_ID);
            for (int i = 0; i <= 4; i++) chest(level).setItem(i, ItemStack.EMPTY);
            level.setChunkForced(CHUNK_X, CHUNK_Z, false);
            return;
        }
        throw new IllegalArgumentException("Unknown Remnant restart probe phase: " + phase);
    }

    private static void seedActions(MinecraftServer server, ServerLevel level) {
        var profile = new com.mojang.authlib.GameProfile(UUID.fromString("df6d381c-9651-41b8-8549-33c069f357eb"), "RestartProbe");
        var player = new net.minecraft.server.level.ServerPlayer(server, level, profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false));
        player.setPos(POS.getX() + 0.5, POS.getY(), POS.getZ() + 0.5);
        try {
            ItemStack ordinary = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
            ordinary.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.IRON_INGOT, 8))));
            chest(level).setItem(1, ordinary); chest(level).setItem(2, new ItemStack(Items.IRON_INGOT));
            player.containerMenu = net.minecraft.world.inventory.ChestMenu.threeRows(21, player.getInventory(), chest(level));
            require(dev.totem.remnant.inventory.BackpackActions.sort(player, false), "Restart matching deposit failed");
            player.containerMenu.setCarried(new ItemStack(Items.IRON_INGOT, 2));
            player.containerMenu.clicked(1, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
            require(player.containerMenu.getCarried().isEmpty(), "Restart direct deposit failed");
            player.containerMenu.clicked(1, 1, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
            require(player.containerMenu.getCarried().is(Items.IRON_INGOT)
                    && player.containerMenu.getCarried().getCount() == 11, "Restart direct withdrawal failed");
            player.containerMenu.clicked(1, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
            require(player.containerMenu.getCarried().isEmpty(), "Restart redeposit failed");
            require(dev.totem.remnant.inventory.BackpackItemInteraction.select(ordinary, 0), "Restart selection failed");
            ItemStack death = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET); helmet.setDamageValue(19);
            death.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(helmet, new ItemStack(Items.APPLE, 2))));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, death);
            var storage = new dev.totem.remnant.inventory.DeathBackpackInventory(player, net.minecraft.world.InteractionHand.MAIN_HAND, 9);
            var menu = dev.totem.remnant.inventory.BackpackMenu.deathServerSide(22, player.getInventory(), storage, 1);
            player.containerMenu = menu;
            require(menu.clickMenuButton(player, dev.totem.remnant.inventory.BackpackMenu.EQUIP_BUTTON_ID), "Restart equip failed");
            chest(level).setItem(3, death);
            ItemStack equipped = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
            chest(level).setItem(4, equipped);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            verifyActions(level);
        } finally {
            player.containerMenu = player.inventoryMenu;
            server.getPlayerList().remove(player);
            channel.finishAndReleaseAll();
        }
    }

    private static void verifyActions(ServerLevel level) {
        ItemStack ordinary = chest(level).getItem(1);
        require(ordinary.is(RemnantItemRegistration.BACKPACK_BASIC)
                        && ordinary.get(DataComponents.CONTAINER).nonEmptyItemCopyStream()
                        .filter(item -> item.is(Items.IRON_INGOT)).mapToInt(ItemStack::getCount).sum() == 11
                        && ordinary.getOrDefault(dev.totem.remnant.registry.RemnantDataComponents.SELECTED_ITEM, -1) == 0
                        && chest(level).getItem(2).isEmpty(), "Restart lost matching-deposit state");
        ItemStack death = chest(level).getItem(3);
        var remaining = death.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList();
        require(death.is(RemnantItemRegistration.DEATH_BACKPACK) && remaining.size() == 1
                        && remaining.getFirst().is(Items.APPLE) && remaining.getFirst().getCount() == 2,
                "Restart duplicated or lost equip source contents");
        require(chest(level).getItem(4).is(Items.DIAMOND_HELMET) && chest(level).getItem(4).getDamageValue() == 19,
                "Restart lost equipped item data");
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
