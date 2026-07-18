package com.adaptor.deadrecall.network;

import com.adaptor.deadrecall.space.TeleportInterfaceType;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceUnitMapPayloadCodecTest {
    @Test
    void catalystBreakdownRoundTripsAndEntriesAreImmutable() {
        SpaceUnitMapPayload.Entry entry = entry(5, 4, 4, 2, 3);
        List<SpaceUnitMapPayload.Entry> mutableEntries = new ArrayList<>(List.of(entry));
        SpaceUnitMapPayload payload = payload(mutableEntries);
        mutableEntries.clear();

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SpaceUnitMapPayload.CODEC.encode(buffer, payload);
            SpaceUnitMapPayload decoded = SpaceUnitMapPayload.CODEC.decode(buffer);
            SpaceUnitMapPayload.Entry decodedEntry = decoded.entries().getFirst();

            assertEquals(1, payload.entries().size());
            assertEquals(5, decodedEntry.baseAmethystCost());
            assertEquals(4, decodedEntry.sourceCatalysts());
            assertEquals(4, decodedEntry.targetCatalysts());
            assertEquals(2, decodedEntry.catalystDiscount());
            assertEquals(3, decodedEntry.amethystCost());
            assertEquals(TeleportInterfaceType.BOOK, decoded.interfaceType());
            assertEquals(0, decodedEntry.baseFoodCost());
            assertEquals(0, decodedEntry.finalFoodCost());
            assertEquals(150, decodedEntry.basePrepareTicks());
            assertEquals(120, decodedEntry.prepareTicks());
            assertEquals(8, decodedEntry.baseMaxHorizontalDeviation());
            assertEquals(8, decodedEntry.maxHorizontalDeviation());
            assertEquals(9, decodedEntry.baseStructureWearChancePercent());
            assertEquals(7, decodedEntry.structureWearChancePercent());
            assertTrue(decodedEntry.interfaceBonusActive());
            assertEquals(
                    "message.deadrecall.space_unit.interface_bonus.book.active",
                    decodedEntry.interfaceBonusMessageKey()
            );
            assertThrows(UnsupportedOperationException.class, () -> decoded.entries().clear());
        } finally {
            buffer.release();
        }
    }

    @Test
    void payloadRejectsTooManyEntriesBeforeEncoding() {
        List<SpaceUnitMapPayload.Entry> entries = Collections.nCopies(
                SpaceUnitMapPayload.MAX_ENTRIES + 1,
                entry(5, 4, 4, 2, 3)
        );

        assertThrows(IllegalArgumentException.class, () -> payload(entries));
    }

    @Test
    void decoderRejectsNegativeAndOversizedEntryCounts() {
        assertRejectedEntryCount(-1);
        assertRejectedEntryCount(SpaceUnitMapPayload.MAX_ENTRIES + 1);
    }

    @Test
    void decoderRejectsUnknownInterfaceType() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writePayloadIdentity(buffer);
            buffer.writeUtf("not_an_interface", 32);
            buffer.writeInt(0);
            assertThrows(DecoderException.class, () -> SpaceUnitMapPayload.CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void catalystFieldsRejectImpossibleRangesAndInconsistentTotals() {
        assertThrows(
                IllegalArgumentException.class,
                () -> entry(
                        5,
                        SpaceUnitMapPayload.MAX_CATALYST_BLOCKS_PER_ENDPOINT + 1,
                        0,
                        1,
                        4
                )
        );
        assertThrows(IllegalArgumentException.class, () -> entry(5, 4, 4, 2, 4));
        assertThrows(IllegalArgumentException.class, () -> entry(5, 74, 74, 5, 1));
    }

    @Test
    void interfaceFieldsRejectImpossibleRangesAndMessageKeys() {
        assertThrows(
                IllegalArgumentException.class,
                () -> entry(5, 4, 4, 2, 3, 61, true,
                        "message.deadrecall.space_unit.interface_bonus.book.active")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> entry(5, 4, 4, 2, 3, 7, true, "")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> entry(5, 4, 4, 2, 3, 7, true, "x".repeat(129))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SpaceUnitMapPayload(
                        UUID.randomUUID(),
                        "player",
                        "Source",
                        "minecraft:overworld",
                        0,
                        0,
                        0,
                        null,
                        List.of()
                )
        );
    }

    @Test
    void quoteDetailsRejectFinalValuesAboveBaseOrInconsistentFoodAllocation() {
        assertThrows(IllegalArgumentException.class, () -> detailedEntry(4, 5, 120, 120, 8, 8, 9, 7));
        assertThrows(IllegalArgumentException.class, () -> detailedEntry(5, 4, 100, 120, 8, 8, 9, 7));
        assertThrows(IllegalArgumentException.class, () -> detailedEntry(5, 4, 120, 120, 7, 8, 9, 7));
        assertThrows(IllegalArgumentException.class, () -> detailedEntry(5, 4, 120, 120, 8, 8, 6, 7));
        assertThrows(IllegalArgumentException.class, () -> detailedEntry(5, 4, 120, 120, 8, 8, 9, 7, 3));
    }

    private static void assertRejectedEntryCount(int entryCount) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writePayloadHeader(buffer);
            buffer.writeInt(entryCount);
            assertThrows(DecoderException.class, () -> SpaceUnitMapPayload.CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    private static SpaceUnitMapPayload payload(List<SpaceUnitMapPayload.Entry> entries) {
        return new SpaceUnitMapPayload(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "lodestone",
                "Source",
                "minecraft:overworld",
                1,
                2,
                3,
                TeleportInterfaceType.BOOK,
                entries
        );
    }

    private static SpaceUnitMapPayload.Entry entry(
            int baseCost,
            int sourceCatalysts,
            int targetCatalysts,
            int discount,
            int finalCost
    ) {
        return entry(
                baseCost,
                sourceCatalysts,
                targetCatalysts,
                discount,
                finalCost,
                7,
                true,
                "message.deadrecall.space_unit.interface_bonus.book.active"
        );
    }

    private static SpaceUnitMapPayload.Entry entry(
            int baseCost,
            int sourceCatalysts,
            int targetCatalysts,
            int discount,
            int finalCost,
            int structureWearChancePercent,
            boolean interfaceBonusActive,
            String interfaceBonusMessageKey
    ) {
        return new SpaceUnitMapPayload.Entry(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "lodestone",
                "Target",
                "private",
                false,
                "minecraft:the_nether",
                4,
                5,
                6,
                0.65D,
                1,
                -1,
                0,
                0,
                0,
                0,
                0,
                20,
                finalCost,
                16,
                baseCost,
                sourceCatalysts,
                targetCatalysts,
                discount,
                150,
                120,
                8,
                8,
                10,
                9,
                structureWearChancePercent,
                interfaceBonusActive,
                interfaceBonusMessageKey,
                false,
                true,
                true,
                0,
                0,
                true,
                ""
        );
    }

    private static SpaceUnitMapPayload.Entry detailedEntry(
            int baseFoodCost,
            int finalFoodCost,
            int basePrepareTicks,
            int finalPrepareTicks,
            int baseDeviation,
            int finalDeviation,
            int baseWear,
            int finalWear) {
        return detailedEntry(
                baseFoodCost,
                finalFoodCost,
                basePrepareTicks,
                finalPrepareTicks,
                baseDeviation,
                finalDeviation,
                baseWear,
                finalWear,
                finalFoodCost
        );
    }

    private static SpaceUnitMapPayload.Entry detailedEntry(
            int baseFoodCost,
            int finalFoodCost,
            int basePrepareTicks,
            int finalPrepareTicks,
            int baseDeviation,
            int finalDeviation,
            int baseWear,
            int finalWear,
            int allocatedFoodCost) {
        return new SpaceUnitMapPayload.Entry(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "lodestone",
                "Detailed Target",
                "private",
                false,
                "minecraft:overworld",
                0,
                64,
                0,
                0.75D,
                1,
                128,
                baseFoodCost,
                finalFoodCost,
                allocatedFoodCost,
                0,
                0,
                20,
                0,
                0,
                basePrepareTicks,
                finalPrepareTicks,
                baseDeviation,
                finalDeviation,
                7,
                baseWear,
                finalWear,
                true,
                "message.deadrecall.space_unit.interface_bonus.book.active",
                false,
                true,
                true,
                0,
                0,
                true,
                ""
        );
    }

    private static void writePayloadHeader(FriendlyByteBuf buffer) {
        writePayloadIdentity(buffer);
        buffer.writeUtf(TeleportInterfaceType.BOOK.id(), 32);
    }

    private static void writePayloadIdentity(FriendlyByteBuf buffer) {
        buffer.writeUUID(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        buffer.writeUtf("lodestone", 32);
        buffer.writeUtf("Source", 128);
        buffer.writeUtf("minecraft:overworld", 128);
        buffer.writeInt(1);
        buffer.writeInt(2);
        buffer.writeInt(3);
    }
}
