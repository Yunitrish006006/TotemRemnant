package com.adaptor.deadrecall.item.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperGolemDataTest {
    @BeforeAll
    static void bootStrap() {
        MinecraftTestBootstrap.bootStrap();
    }

    @Test
    void migrateAddsDefaultsAndConvertsLegacyBinding() {
        CompoundTag tag = new CompoundTag();
        tag.putString(CopperGolemData.TAG_BOUND_CONTAINER_DIM, Level.OVERWORLD.identifier().toString());
        tag.putInt(CopperGolemData.TAG_BOUND_CONTAINER_X, 12);
        tag.putInt(CopperGolemData.TAG_BOUND_CONTAINER_Y, 64);
        tag.putInt(CopperGolemData.TAG_BOUND_CONTAINER_Z, -8);

        assertTrue(CopperGolemData.migrate(tag));

        assertEquals(CopperGolemData.DATA_VERSION, tag.getIntOr(CopperGolemData.TAG_DATA_VERSION, -1));
        assertEquals(CopperGolemMode.SORTING.id(), tag.getStringOr(CopperGolemData.TAG_MODE, ""));
        assertEquals(0, tag.getIntOr(CopperGolemData.TAG_REVISION, -1));
        assertFalse(tag.contains(CopperGolemData.TAG_BOUND_CONTAINER_DIM));
        assertFalse(tag.contains(CopperGolemData.TAG_BOUND_CONTAINER_X));
        assertFalse(tag.contains(CopperGolemData.TAG_BOUND_CONTAINER_Y));
        assertFalse(tag.contains(CopperGolemData.TAG_BOUND_CONTAINER_Z));

        assertEquals(
                List.of(new CopperGolemWrenchHandler.Binding(Level.OVERWORLD, new BlockPos(12, 64, -8))),
                CopperGolemData.readBindings(tag));
        assertFalse(CopperGolemData.migrate(tag));
    }

    @Test
    void bindingAndBlockPosCodecsRoundTrip() {
        CompoundTag tag = new CompoundTag();
        List<CopperGolemWrenchHandler.Binding> bindings = List.of(
                new CopperGolemWrenchHandler.Binding(Level.OVERWORLD, new BlockPos(1, 2, 3)),
                new CopperGolemWrenchHandler.Binding(Level.NETHER, new BlockPos(-4, 70, 9))
        );

        CopperGolemData.writeBindings(tag, bindings);
        assertEquals(bindings, CopperGolemData.readBindings(tag));

        CopperGolemData.writeBlockPos(tag, new BlockPos(7, 8, 9), "x", "y", "z");
        assertEquals(new BlockPos(7, 8, 9), CopperGolemData.readBlockPos(tag, "x", "y", "z").orElseThrow());
    }

    @Test
    void stringListCodecAppliesLimitAndRemovesEmptyLists() {
        CompoundTag tag = new CompoundTag();

        CopperGolemData.writeStringList(tag, "values", List.of("minecraft:stone", "", "minecraft:dirt", "minecraft:oak_log"), 2);
        assertEquals(List.of("minecraft:stone", "minecraft:dirt"), CopperGolemData.readStringList(tag, "values"));

        CopperGolemData.writeStringList(tag, "values", List.of(), 2);
        assertFalse(tag.contains("values"));
    }

    @Test
    void removeSortingBlockedTagsClearsSnapshot() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(CopperGolemData.TAG_SORTING_BLOCKED, true);
        tag.putString(CopperGolemData.TAG_BLOCKED_SOURCE_CONTAINER_DIM, Level.OVERWORLD.identifier().toString());
        tag.putInt(CopperGolemData.TAG_BLOCKED_SOURCE_HASH, 10);
        tag.putInt(CopperGolemData.TAG_BLOCKED_BINDINGS_HASH, 20);
        tag.putInt(CopperGolemData.TAG_BLOCKED_TARGETS_HASH, 30);

        assertTrue(CopperGolemData.removeSortingBlockedTags(tag));
        assertFalse(tag.contains(CopperGolemData.TAG_SORTING_BLOCKED));
        assertFalse(tag.contains(CopperGolemData.TAG_BLOCKED_SOURCE_CONTAINER_DIM));
        assertFalse(tag.contains(CopperGolemData.TAG_BLOCKED_SOURCE_HASH));
        assertFalse(tag.contains(CopperGolemData.TAG_BLOCKED_BINDINGS_HASH));
        assertFalse(tag.contains(CopperGolemData.TAG_BLOCKED_TARGETS_HASH));
        assertFalse(CopperGolemData.removeSortingBlockedTags(tag));
    }
}
