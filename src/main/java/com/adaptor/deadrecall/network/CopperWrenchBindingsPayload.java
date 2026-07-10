package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CopperWrenchBindingsPayload(
        UUID golemId,
        boolean running,
        String fuelItemId,
        int fuelCount,
        int fuelTicks,
        String llmApiUrl,
        String llmApiKey,
        String llmModel,
        int llmActiveCount,
        List<BindingEntry> bindings)
        implements CustomPacketPayload {

    public static final Type<CopperWrenchBindingsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "copper_wrench_bindings"));
    private static final int MAX_CACHE_VALUES = 64;
    private static final int MAX_CACHE_VALUE_LENGTH = 256;

    public record BindingEntry(
            String dimension,
            int x,
            int y,
            int z,
            String blockId,
            String itemId,
            boolean loaded,
            boolean available,
            boolean llmEnabled,
            String llmPrompt,
            int llmCachedItemIds,
            int llmCachedTags,
            List<String> llmAllowedItemIds,
            List<String> llmDeniedItemIds,
            List<String> llmAllowedTags,
            List<String> llmDeniedTags) {
    }

    public static final StreamCodec<FriendlyByteBuf, CopperWrenchBindingsPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeBoolean(payload.running());
                        buf.writeUtf(payload.fuelItemId(), 128);
                        buf.writeInt(payload.fuelCount());
                        buf.writeInt(payload.fuelTicks());
                        buf.writeUtf(payload.llmApiUrl(), 2048);
                        buf.writeUtf(payload.llmApiKey(), 512);
                        buf.writeUtf(payload.llmModel(), 256);
                        buf.writeInt(payload.llmActiveCount());
                        buf.writeInt(payload.bindings().size());
                        for (BindingEntry binding : payload.bindings()) {
                            buf.writeUtf(binding.dimension(), 128);
                            buf.writeInt(binding.x());
                            buf.writeInt(binding.y());
                            buf.writeInt(binding.z());
                            buf.writeUtf(binding.blockId(), 128);
                            buf.writeUtf(binding.itemId(), 128);
                            buf.writeBoolean(binding.loaded());
                            buf.writeBoolean(binding.available());
                            buf.writeBoolean(binding.llmEnabled());
                            buf.writeUtf(binding.llmPrompt(), 2048);
                            buf.writeInt(binding.llmCachedItemIds());
                            buf.writeInt(binding.llmCachedTags());
                            writeStringList(buf, binding.llmAllowedItemIds());
                            writeStringList(buf, binding.llmDeniedItemIds());
                            writeStringList(buf, binding.llmAllowedTags());
                            writeStringList(buf, binding.llmDeniedTags());
                        }
                    },
                    buf -> new CopperWrenchBindingsPayload(
                            buf.readUUID(),
                            buf.readBoolean(),
                            buf.readUtf(128),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readUtf(2048),
                            buf.readUtf(512),
                            buf.readUtf(256),
                            buf.readInt(),
                            readBindings(buf)
                    )
            );

    private static List<BindingEntry> readBindings(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<BindingEntry> bindings = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            bindings.add(new BindingEntry(
                    buf.readUtf(128),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(128),
                    buf.readUtf(128),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readUtf(2048),
                    buf.readInt(),
                    buf.readInt(),
                    readStringList(buf),
                    readStringList(buf),
                    readStringList(buf),
                    readStringList(buf)
            ));
        }
        return bindings;
    }

    private static void writeStringList(FriendlyByteBuf buf, List<String> values) {
        List<String> normalized = values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(MAX_CACHE_VALUES)
                .toList();
        buf.writeInt(normalized.size());
        for (String value : normalized) {
            buf.writeUtf(value, MAX_CACHE_VALUE_LENGTH);
        }
    }

    private static List<String> readStringList(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<String> values = new ArrayList<>(Math.min(count, MAX_CACHE_VALUES));
        for (int i = 0; i < count; i++) {
            String value = buf.readUtf(MAX_CACHE_VALUE_LENGTH);
            if (i < MAX_CACHE_VALUES && !value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
