package com.adaptor.deadrecall.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportInterfaceQuotePolicyTest {
    @Test
    void ordinaryCompassPreservesBaselineQuote() {
        TeleportInterfaceQuotePolicy.Quote quote = specialize(
                TeleportInterfaceType.COMPASS,
                SpaceUnitType.LODESTONE,
                true,
                101,
                13,
                7
        );

        assertEquals(101, quote.prepareTicks());
        assertEquals(13, quote.maxHorizontalDeviation());
        assertEquals(7, quote.structureWearChancePercent());
        assertFalse(quote.bonusActive());
    }

    @Test
    void recoveryCompassHalvesOnlyOwnedDeathDeviationWithFloorRounding() {
        TeleportInterfaceQuotePolicy.Quote ownedDeath = specialize(
                TeleportInterfaceType.RECOVERY_COMPASS,
                SpaceUnitType.DEATH,
                true,
                140,
                13,
                22
        );
        TeleportInterfaceQuotePolicy.Quote foreignDeath = specialize(
                TeleportInterfaceType.RECOVERY_COMPASS,
                SpaceUnitType.DEATH,
                false,
                140,
                13,
                22
        );
        TeleportInterfaceQuotePolicy.Quote lodestone = specialize(
                TeleportInterfaceType.RECOVERY_COMPASS,
                SpaceUnitType.LODESTONE,
                true,
                140,
                13,
                22
        );

        assertEquals(6, ownedDeath.maxHorizontalDeviation());
        assertEquals(140, ownedDeath.prepareTicks());
        assertEquals(22, ownedDeath.structureWearChancePercent());
        assertTrue(ownedDeath.bonusActive());
        assertEquals(13, foreignDeath.maxHorizontalDeviation());
        assertFalse(foreignDeath.bonusActive());
        assertEquals(13, lodestone.maxHorizontalDeviation());
        assertFalse(lodestone.bonusActive());
    }

    @Test
    void bookReducesOnlyLodestonePrepareTimeAndStructureWear() {
        TeleportInterfaceQuotePolicy.Quote lodestone = specialize(
                TeleportInterfaceType.BOOK,
                SpaceUnitType.LODESTONE,
                false,
                101,
                13,
                7
        );
        TeleportInterfaceQuotePolicy.Quote player = specialize(
                TeleportInterfaceType.BOOK,
                SpaceUnitType.PLAYER,
                false,
                101,
                13,
                7
        );

        assertEquals(81, lodestone.prepareTicks());
        assertEquals(13, lodestone.maxHorizontalDeviation());
        assertEquals(5, lodestone.structureWearChancePercent());
        assertTrue(lodestone.bonusActive());
        assertEquals(101, player.prepareTicks());
        assertEquals(13, player.maxHorizontalDeviation());
        assertEquals(7, player.structureWearChancePercent());
        assertFalse(player.bonusActive());
    }

    @Test
    void bookPreparationTimeKeepsThirtyTickMinimum() {
        TeleportInterfaceQuotePolicy.Quote quote = specialize(
                TeleportInterfaceType.BOOK,
                SpaceUnitType.LODESTONE,
                true,
                31,
                1,
                1
        );

        assertEquals(30, quote.prepareTicks());
        assertEquals(0, quote.structureWearChancePercent());
    }

    @Test
    void policyClampsInputsBeforeApplyingMultipliers() {
        TeleportInterfaceQuotePolicy.Quote low = specialize(
                TeleportInterfaceType.COMPASS,
                SpaceUnitType.SYSTEM,
                false,
                -1,
                -2,
                -3
        );
        TeleportInterfaceQuotePolicy.Quote high = specialize(
                TeleportInterfaceType.COMPASS,
                SpaceUnitType.SYSTEM,
                false,
                999,
                999,
                999
        );

        assertEquals(0, low.prepareTicks());
        assertEquals(0, low.maxHorizontalDeviation());
        assertEquals(0, low.structureWearChancePercent());
        assertEquals(TeleportInterfaceQuotePolicy.MAX_PREPARE_TICKS, high.prepareTicks());
        assertEquals(TeleportInterfaceQuotePolicy.MAX_DEVIATION, high.maxHorizontalDeviation());
        assertEquals(TeleportInterfaceQuotePolicy.MAX_WEAR_CHANCE_PERCENT,
                high.structureWearChancePercent());
    }

    @Test
    void filledMapReducesCoveredFoodAndDeviationOnly() {
        TeleportInterfaceQuotePolicy.Quote covered = TeleportInterfaceQuotePolicy.specialize(
                TeleportInterfaceType.FILLED_MAP,
                SpaceUnitType.PLAYER,
                false,
                true,
                7,
                101,
                13,
                7
        );
        TeleportInterfaceQuotePolicy.Quote uncovered = TeleportInterfaceQuotePolicy.specialize(
                TeleportInterfaceType.FILLED_MAP,
                SpaceUnitType.PLAYER,
                false,
                false,
                7,
                101,
                13,
                7
        );

        assertEquals(6, covered.foodCost());
        assertEquals(10, covered.maxHorizontalDeviation());
        assertEquals(101, covered.prepareTicks());
        assertEquals(7, covered.structureWearChancePercent());
        assertTrue(covered.bonusActive());
        assertEquals(7, uncovered.foodCost());
        assertEquals(13, uncovered.maxHorizontalDeviation());
        assertFalse(uncovered.bonusActive());
    }

    @Test
    void filledMapFoodDiscountKeepsMinimumOneAndClampsInputs() {
        TeleportInterfaceQuotePolicy.Quote minimum = TeleportInterfaceQuotePolicy.specialize(
                TeleportInterfaceType.FILLED_MAP,
                SpaceUnitType.LODESTONE,
                false,
                true,
                1,
                80,
                1,
                6
        );
        TeleportInterfaceQuotePolicy.Quote clamped = TeleportInterfaceQuotePolicy.specialize(
                TeleportInterfaceType.FILLED_MAP,
                SpaceUnitType.LODESTONE,
                false,
                true,
                999,
                80,
                1,
                6
        );

        assertEquals(1, minimum.foodCost());
        assertEquals(0, minimum.maxHorizontalDeviation());
        assertEquals(16, clamped.foodCost());
    }

    private static TeleportInterfaceQuotePolicy.Quote specialize(
            TeleportInterfaceType interfaceType,
            SpaceUnitType targetType,
            boolean owned,
            int prepareTicks,
            int deviation,
            int wearChance) {
        return TeleportInterfaceQuotePolicy.specialize(
                interfaceType,
                targetType,
                owned,
                false,
                10,
                prepareTicks,
                deviation,
                wearChance
        );
    }
}
