package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DeathNodeAdminPayload(List<Entry> entries, boolean truncated) implements CustomPacketPayload {
    public static final Type<DeathNodeAdminPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "death_node_admin"));
    public static final int MAX_ENTRIES = 2048;

    public record Entry(
            UUID id,
            UUID ownerId,
            String ownerName,
            String name,
            String status,
            String dimension,
            int x,
            int y,
            int z,
            long createdGameTime,
            long updatedGameTime) {
    }

    public DeathNodeAdminPayload {
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public static final StreamCodec<FriendlyByteBuf, DeathNodeAdminPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                int size = Math.min(payload.entries().size(), MAX_ENTRIES);
                buf.writeInt(size);
                for (int index = 0; index < size; index++) {
                    writeEntry(buf, payload.entries().get(index));
                }
                buf.writeBoolean(payload.truncated());
            },
            buf -> {
                int size = Math.max(0, Math.min(buf.readInt(), MAX_ENTRIES));
                List<Entry> entries = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    entries.add(readEntry(buf));
                }
                return new DeathNodeAdminPayload(entries, buf.readBoolean());
            }
    );

    private static void writeEntry(FriendlyByteBuf buf, Entry entry) {
        buf.writeUUID(entry.id());
        buf.writeUUID(entry.ownerId());
        buf.writeUtf(entry.ownerName(), 64);
        buf.writeUtf(entry.name(), 128);
        buf.writeUtf(entry.status(), 32);
        buf.writeUtf(entry.dimension(), 128);
        buf.writeInt(entry.x());
        buf.writeInt(entry.y());
        buf.writeInt(entry.z());
        buf.writeLong(entry.createdGameTime());
        buf.writeLong(entry.updatedGameTime());
    }

    private static Entry readEntry(FriendlyByteBuf buf) {
        return new Entry(
                buf.readUUID(),
                buf.readUUID(),
                buf.readUtf(64),
                buf.readUtf(128),
                buf.readUtf(32),
                buf.readUtf(128),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readLong(),
                buf.readLong()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
