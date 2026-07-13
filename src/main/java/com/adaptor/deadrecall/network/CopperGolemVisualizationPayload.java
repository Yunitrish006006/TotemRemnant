package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CopperGolemVisualizationPayload(
        UUID golemId,
        boolean valid,
        String dimension,
        double golemX,
        double golemY,
        double golemZ,
        String mode,
        String activity,
        PosEntry source,
        AreaEntry gatheringArea,
        PosEntry gatheringTarget,
        List<PosEntry> destinations)
        implements CustomPacketPayload {
    public static final Type<CopperGolemVisualizationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "copper_golem_visualization"));
    private static final int MAX_POS_ENTRIES = 128;

    public record PosEntry(String dimension, int x, int y, int z, boolean available) {
    }

    public record AreaEntry(
            String dimension,
            boolean hasCornerA,
            int cornerAX,
            int cornerAY,
            int cornerAZ,
            boolean hasCornerB,
            int cornerBX,
            int cornerBY,
            int cornerBZ) {
    }

    public static final StreamCodec<FriendlyByteBuf, CopperGolemVisualizationPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeBoolean(payload.valid());
                        buf.writeUtf(payload.dimension(), 128);
                        buf.writeDouble(payload.golemX());
                        buf.writeDouble(payload.golemY());
                        buf.writeDouble(payload.golemZ());
                        buf.writeUtf(payload.mode(), 32);
                        buf.writeUtf(payload.activity(), 64);
                        writeOptionalPos(buf, payload.source());
                        writeOptionalArea(buf, payload.gatheringArea());
                        writeOptionalPos(buf, payload.gatheringTarget());
                        writePosList(buf, payload.destinations());
                    },
                    buf -> new CopperGolemVisualizationPayload(
                            buf.readUUID(),
                            buf.readBoolean(),
                            buf.readUtf(128),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readUtf(32),
                            buf.readUtf(64),
                            readOptionalPos(buf),
                            readOptionalArea(buf),
                            readOptionalPos(buf),
                            readPosList(buf)
                    )
            );

    private static void writeOptionalPos(FriendlyByteBuf buf, PosEntry entry) {
        buf.writeBoolean(entry != null);
        if (entry != null) {
            writePos(buf, entry);
        }
    }

    private static PosEntry readOptionalPos(FriendlyByteBuf buf) {
        return buf.readBoolean() ? readPos(buf) : null;
    }

    private static void writeOptionalArea(FriendlyByteBuf buf, AreaEntry area) {
        buf.writeBoolean(area != null);
        if (area == null) {
            return;
        }
        buf.writeUtf(area.dimension(), 128);
        buf.writeBoolean(area.hasCornerA());
        buf.writeInt(area.cornerAX());
        buf.writeInt(area.cornerAY());
        buf.writeInt(area.cornerAZ());
        buf.writeBoolean(area.hasCornerB());
        buf.writeInt(area.cornerBX());
        buf.writeInt(area.cornerBY());
        buf.writeInt(area.cornerBZ());
    }

    private static AreaEntry readOptionalArea(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        return new AreaEntry(
                buf.readUtf(128),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    private static void writePosList(FriendlyByteBuf buf, List<PosEntry> entries) {
        int size = Math.min(entries.size(), MAX_POS_ENTRIES);
        buf.writeInt(size);
        for (int i = 0; i < size; i++) {
            writePos(buf, entries.get(i));
        }
    }

    private static List<PosEntry> readPosList(FriendlyByteBuf buf) {
        int size = Math.min(buf.readInt(), MAX_POS_ENTRIES);
        List<PosEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(readPos(buf));
        }
        return entries;
    }

    private static void writePos(FriendlyByteBuf buf, PosEntry entry) {
        buf.writeUtf(entry.dimension(), 128);
        buf.writeInt(entry.x());
        buf.writeInt(entry.y());
        buf.writeInt(entry.z());
        buf.writeBoolean(entry.available());
    }

    private static PosEntry readPos(FriendlyByteBuf buf) {
        return new PosEntry(buf.readUtf(128), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
