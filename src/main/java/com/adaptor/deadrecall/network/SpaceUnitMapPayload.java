package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SpaceUnitMapPayload(
        UUID sourceUnitId,
        String sourceType,
        String sourceName,
        String sourceDimension,
        int sourceX,
        int sourceY,
        int sourceZ,
        List<Entry> entries)
        implements CustomPacketPayload {
    public static final Type<SpaceUnitMapPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "space_unit_map"));
    public static final int MAX_ENTRIES = 128;

    public record Entry(
            UUID id,
            String type,
            String name,
            String visibility,
            boolean friendShared,
            String dimension,
            int x,
            int y,
            int z,
            double resonance,
            int tier,
            int distanceBlocks,
            int saturationCost,
            int hungerCost,
            int foodPointsNeeded,
            int safeFoodPointsAvailable,
            int amethystCost,
            int amethystAvailable,
            int baseAmethystCost,
            int sourceCatalysts,
            int targetCatalysts,
            int catalystDiscount,
            int prepareTicks,
            int maxHorizontalDeviation,
            int damageChancePercent,
            boolean favorite,
            boolean manageable,
            boolean owned,
            int administratorCount,
            int allowedPlayerCount,
            boolean canTeleport,
            String blockedReason) {

        /**
         * Compatibility constructor for call sites that have not yet populated catalyst details.
         */
        public Entry(
                UUID id,
                String type,
                String name,
                String visibility,
                boolean friendShared,
                String dimension,
                int x,
                int y,
                int z,
                double resonance,
                int tier,
                int distanceBlocks,
                int saturationCost,
                int hungerCost,
                int foodPointsNeeded,
                int safeFoodPointsAvailable,
                int amethystCost,
                int amethystAvailable,
                int prepareTicks,
                int maxHorizontalDeviation,
                int damageChancePercent,
                boolean favorite,
                boolean manageable,
                boolean owned,
                int administratorCount,
                int allowedPlayerCount,
                boolean canTeleport,
                String blockedReason) {
            this(
                    id, type, name, visibility, friendShared, dimension, x, y, z, resonance, tier,
                    distanceBlocks, saturationCost, hungerCost, foodPointsNeeded, safeFoodPointsAvailable,
                    amethystCost, amethystAvailable, amethystCost, 0, 0, 0,
                    prepareTicks, maxHorizontalDeviation, damageChancePercent, favorite, manageable, owned,
                    administratorCount, allowedPlayerCount, canTeleport, blockedReason
            );
        }
    }

    public static final StreamCodec<FriendlyByteBuf, SpaceUnitMapPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.sourceUnitId());
                        buf.writeUtf(payload.sourceType(), 32);
                        buf.writeUtf(payload.sourceName(), 128);
                        buf.writeUtf(payload.sourceDimension(), 128);
                        buf.writeInt(payload.sourceX());
                        buf.writeInt(payload.sourceY());
                        buf.writeInt(payload.sourceZ());
                        writeEntries(buf, payload.entries());
                    },
                    buf -> new SpaceUnitMapPayload(
                            buf.readUUID(),
                            buf.readUtf(32),
                            buf.readUtf(128),
                            buf.readUtf(128),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            readEntries(buf)
                    )
            );

    private static void writeEntries(FriendlyByteBuf buf, List<Entry> entries) {
        int size = Math.min(entries.size(), MAX_ENTRIES);
        buf.writeInt(size);
        for (int i = 0; i < size; i++) {
            writeEntry(buf, entries.get(i));
        }
    }

    private static List<Entry> readEntries(FriendlyByteBuf buf) {
        int size = Math.min(buf.readInt(), MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(readEntry(buf));
        }
        return entries;
    }

    private static void writeEntry(FriendlyByteBuf buf, Entry entry) {
        buf.writeUUID(entry.id());
        buf.writeUtf(entry.type(), 32);
        buf.writeUtf(entry.name(), 128);
        buf.writeUtf(entry.visibility(), 32);
        buf.writeBoolean(entry.friendShared());
        buf.writeUtf(entry.dimension(), 128);
        buf.writeInt(entry.x());
        buf.writeInt(entry.y());
        buf.writeInt(entry.z());
        buf.writeDouble(entry.resonance());
        buf.writeInt(entry.tier());
        buf.writeInt(entry.distanceBlocks());
        buf.writeInt(entry.saturationCost());
        buf.writeInt(entry.hungerCost());
        buf.writeInt(entry.foodPointsNeeded());
        buf.writeInt(entry.safeFoodPointsAvailable());
        buf.writeInt(entry.amethystCost());
        buf.writeInt(entry.amethystAvailable());
        buf.writeInt(entry.baseAmethystCost());
        buf.writeInt(entry.sourceCatalysts());
        buf.writeInt(entry.targetCatalysts());
        buf.writeInt(entry.catalystDiscount());
        buf.writeInt(entry.prepareTicks());
        buf.writeInt(entry.maxHorizontalDeviation());
        buf.writeInt(entry.damageChancePercent());
        buf.writeBoolean(entry.favorite());
        buf.writeBoolean(entry.manageable());
        buf.writeBoolean(entry.owned());
        buf.writeInt(entry.administratorCount());
        buf.writeInt(entry.allowedPlayerCount());
        buf.writeBoolean(entry.canTeleport());
        buf.writeUtf(entry.blockedReason(), 128);
    }

    private static Entry readEntry(FriendlyByteBuf buf) {
        return new Entry(
                buf.readUUID(),
                buf.readUtf(32),
                buf.readUtf(128),
                buf.readUtf(32),
                buf.readBoolean(),
                buf.readUtf(128),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readDouble(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readUtf(128)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
