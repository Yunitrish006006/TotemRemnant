package com.adaptor.deadrecall.space;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpaceStructureSnapshotCodecTest {
    @Test
    void legacySnapshotWithoutCatalystFieldDefaultsToZero() {
        SpaceStructureSnapshot snapshot = SpaceStructureSnapshot.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("""
                        {
                          "completeness": 0.8,
                          "symmetry": 0.7,
                          "resonance": 0.6,
                          "interference": 0.1,
                          "environment_stability": 0.9,
                          "wear": 0.2,
                          "tier": 2
                        }
                        """)
        ).getOrThrow();

        assertEquals(2, snapshot.tier());
        assertEquals(0, snapshot.amethystCatalystBlocks());
    }

    @Test
    void currentSnapshotPreservesCatalystCount() {
        SpaceStructureSnapshot snapshot = SpaceStructureSnapshot.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("""
                        {
                          "completeness": 1.0,
                          "symmetry": 1.0,
                          "resonance": 0.75,
                          "interference": 0.0,
                          "environment_stability": 1.0,
                          "wear": 0.0,
                          "tier": 3,
                          "amethyst_catalyst_blocks": 12
                        }
                        """)
        ).getOrThrow();

        assertEquals(12, snapshot.amethystCatalystBlocks());
    }
}
