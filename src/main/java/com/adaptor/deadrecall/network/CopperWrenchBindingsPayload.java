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
        int revision,
        boolean running,
        String mode,
        String activity,
        String fuelItemId,
        int fuelCount,
        int fuelTicks,
        String gatheringToolItemId,
        int gatheringToolCount,
        int gatheringToolDamage,
        int gatheringToolMaxDamage,
        String gatheringStorageItemId,
        int gatheringStorageCount,
        String llmApiUrl,
        String llmApiKey,
        String llmModel,
        int llmActiveCount,
        BindingEntry sourceContainer,
        GatheringAreaEntry gatheringArea,
        List<String> gatheringManualTargets,
        boolean gatheringLlmEnabled,
        String gatheringLlmPrompt,
        int gatheringLlmCachedBlockIds,
        int gatheringLlmCachedTags,
        List<String> gatheringLlmAllowedBlockIds,
        List<String> gatheringLlmDeniedBlockIds,
        List<String> gatheringLlmAllowedTags,
        List<String> gatheringLlmDeniedTags,
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

    public record GatheringAreaEntry(
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

    public static final StreamCodec<FriendlyByteBuf, CopperWrenchBindingsPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUUID(payload.golemId());
                        buf.writeInt(payload.revision());
                        buf.writeBoolean(payload.running());
                        buf.writeUtf(payload.mode(), 32);
                        buf.writeUtf(payload.activity(), 64);
                        buf.writeUtf(payload.fuelItemId(), 128);
                        buf.writeInt(payload.fuelCount());
                        buf.writeInt(payload.fuelTicks());
                        buf.writeUtf(payload.gatheringToolItemId(), 128);
                        buf.writeInt(payload.gatheringToolCount());
                        buf.writeInt(payload.gatheringToolDamage());
                        buf.writeInt(payload.gatheringToolMaxDamage());
                        buf.writeUtf(payload.gatheringStorageItemId(), 128);
                        buf.writeInt(payload.gatheringStorageCount());
                        buf.writeUtf(payload.llmApiUrl(), 2048);
                        buf.writeUtf(payload.llmApiKey(), 512);
                        buf.writeUtf(payload.llmModel(), 256);
                        buf.writeInt(payload.llmActiveCount());
                        writeOptionalBindingEntry(buf, payload.sourceContainer());
                        writeOptionalGatheringArea(buf, payload.gatheringArea());
                        writeStringList(buf, payload.gatheringManualTargets());
                        buf.writeBoolean(payload.gatheringLlmEnabled());
                        buf.writeUtf(payload.gatheringLlmPrompt(), 2048);
                        buf.writeInt(payload.gatheringLlmCachedBlockIds());
                        buf.writeInt(payload.gatheringLlmCachedTags());
                        writeStringList(buf, payload.gatheringLlmAllowedBlockIds());
                        writeStringList(buf, payload.gatheringLlmDeniedBlockIds());
                        writeStringList(buf, payload.gatheringLlmAllowedTags());
                        writeStringList(buf, payload.gatheringLlmDeniedTags());
                        buf.writeInt(payload.bindings().size());
                        for (BindingEntry binding : payload.bindings()) {
                            writeBindingEntry(buf, binding);
                        }
                    },
                    buf -> new CopperWrenchBindingsPayload(
                            buf.readUUID(),
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readUtf(32),
                            buf.readUtf(64),
                            buf.readUtf(128),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readUtf(128),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readUtf(128),
                            buf.readInt(),
                            buf.readUtf(2048),
                            buf.readUtf(512),
                            buf.readUtf(256),
                            buf.readInt(),
                            readOptionalBindingEntry(buf),
                            readOptionalGatheringArea(buf),
                            readStringList(buf),
                            buf.readBoolean(),
                            buf.readUtf(2048),
                            buf.readInt(),
                            buf.readInt(),
                            readStringList(buf),
                            readStringList(buf),
                            readStringList(buf),
                            readStringList(buf),
                            readBindings(buf)
                    )
            );

    private static List<BindingEntry> readBindings(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<BindingEntry> bindings = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            bindings.add(readBindingEntry(buf));
        }
        return bindings;
    }

    private static void writeOptionalBindingEntry(FriendlyByteBuf buf, BindingEntry binding) {
        buf.writeBoolean(binding != null);
        if (binding != null) {
            writeBindingEntry(buf, binding);
        }
    }

    private static BindingEntry readOptionalBindingEntry(FriendlyByteBuf buf) {
        return buf.readBoolean() ? readBindingEntry(buf) : null;
    }

    private static void writeOptionalGatheringArea(FriendlyByteBuf buf, GatheringAreaEntry area) {
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

    private static GatheringAreaEntry readOptionalGatheringArea(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }

        return new GatheringAreaEntry(
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

    private static void writeBindingEntry(FriendlyByteBuf buf, BindingEntry binding) {
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

    private static BindingEntry readBindingEntry(FriendlyByteBuf buf) {
        return new BindingEntry(
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
        );
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
