package com.adaptor.deadrecall.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmethystCatalystDiscountTest {
    @Test
    void everyFourCatalystsReduceOneShard() {
        assertEquals(0, AmethystCatalystDiscount.catalystDiscount(3, 0));
        assertEquals(1, AmethystCatalystDiscount.catalystDiscount(4, 0));
        assertEquals(2, AmethystCatalystDiscount.catalystDiscount(4, 4));
        assertEquals(3, AmethystCatalystDiscount.catalystDiscount(7, 5));
    }

    @Test
    void crossDimensionCostNeverBecomesFree() {
        assertEquals(4, AmethystCatalystDiscount.finalCost(4, 0, 3));
        assertEquals(3, AmethystCatalystDiscount.finalCost(4, 4, 0));
        assertEquals(2, AmethystCatalystDiscount.finalCost(4, 4, 4));
        assertEquals(1, AmethystCatalystDiscount.finalCost(4, 12, 0));
        assertEquals(1, AmethystCatalystDiscount.finalCost(4, 64, 64));
    }

    @Test
    void noBaseCostRemainsFree() {
        assertEquals(0, AmethystCatalystDiscount.finalCost(0, 16, 16));
    }
}
