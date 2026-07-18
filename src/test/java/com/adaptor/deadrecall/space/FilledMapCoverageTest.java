package com.adaptor.deadrecall.space;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilledMapCoverageTest {
    @Test
    void scaleZeroCoversCenterAndExactHalfOpenEdges() {
        assertTrue(covers(0, 0, 0, 0, 0));
        assertTrue(covers(0, 0, 0, -64, -64));
        assertTrue(covers(0, 0, 0, 63, 63));
        assertFalse(covers(0, 0, 0, 64, 0));
        assertFalse(covers(0, 0, 0, 0, 64));
        assertFalse(covers(0, 0, 0, -65, 0));
    }

    @Test
    void vanillaScaleChangesCoverageByPowersOfTwo() {
        assertTrue(covers(100, -200, 2, 355, 55));
        assertFalse(covers(100, -200, 2, 356, 0));
        assertTrue(covers(100, -200, 4, -924, -1224));
        assertFalse(covers(100, -200, 4, 1124, 0));
    }

    @Test
    void dimensionMismatchNeverCoversTarget() {
        assertFalse(FilledMapCoverage.covers(
                Level.OVERWORLD,
                0,
                0,
                4,
                Level.NETHER,
                BlockPos.ZERO
        ));
    }

    @Test
    void rejectsScaleOutsideVanillaRange() {
        assertThrows(IllegalArgumentException.class, () -> FilledMapCoverage.bounds(0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> FilledMapCoverage.bounds(0, 0, 5));
    }

    private static boolean covers(int centerX, int centerZ, int scale, int targetX, int targetZ) {
        return FilledMapCoverage.covers(
                Level.OVERWORLD,
                centerX,
                centerZ,
                scale,
                Level.OVERWORLD,
                new BlockPos(targetX, 64, targetZ)
        );
    }
}
