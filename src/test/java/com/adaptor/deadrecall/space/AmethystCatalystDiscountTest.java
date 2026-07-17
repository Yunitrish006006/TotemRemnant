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

    @Test
    void quoteExposesPayloadReadyBreakdown() {
        AmethystCatalystDiscount.Quote quote = AmethystCatalystDiscount.quote(6, 5, 7);

        assertEquals(6, quote.baseCost());
        assertEquals(5, quote.sourceCatalysts());
        assertEquals(7, quote.targetCatalysts());
        assertEquals(3, quote.availableDiscount());
        assertEquals(3, quote.appliedDiscount());
        assertEquals(3, quote.finalCost());
    }

    @Test
    void quoteSeparatesAvailableAndAppliedDiscount() {
        AmethystCatalystDiscount.Quote quote = AmethystCatalystDiscount.quote(2, 40, 40);

        assertEquals(20, quote.availableDiscount());
        assertEquals(1, quote.appliedDiscount());
        assertEquals(1, quote.finalCost());
    }

    @Test
    void quoteNormalizesInvalidNegativeInputs() {
        AmethystCatalystDiscount.Quote quote = AmethystCatalystDiscount.quote(-3, -4, -8);

        assertEquals(0, quote.baseCost());
        assertEquals(0, quote.sourceCatalysts());
        assertEquals(0, quote.targetCatalysts());
        assertEquals(0, quote.availableDiscount());
        assertEquals(0, quote.appliedDiscount());
        assertEquals(0, quote.finalCost());
    }

    @Test
    void playerSourceCannotProvideCatalystDiscount() {
        AmethystCatalystDiscount.Quote quote = AmethystCatalystDiscount.quoteForEndpoints(
                6,
                false,
                64,
                true,
                4
        );

        assertEquals(0, quote.sourceCatalysts());
        assertEquals(4, quote.targetCatalysts());
        assertEquals(1, quote.appliedDiscount());
        assertEquals(5, quote.finalCost());
    }

    @Test
    void playerAndDeathTargetsCannotProvideCatalystDiscount() {
        AmethystCatalystDiscount.Quote playerTarget = AmethystCatalystDiscount.quoteForEndpoints(
                6,
                true,
                4,
                false,
                64
        );
        AmethystCatalystDiscount.Quote deathTarget = AmethystCatalystDiscount.quoteForEndpoints(
                6,
                true,
                4,
                false,
                12
        );

        assertEquals(0, playerTarget.targetCatalysts());
        assertEquals(1, playerTarget.appliedDiscount());
        assertEquals(5, playerTarget.finalCost());
        assertEquals(0, deathTarget.targetCatalysts());
        assertEquals(1, deathTarget.appliedDiscount());
        assertEquals(5, deathTarget.finalCost());
    }

    @Test
    void bothLodestoneEndpointsCanContribute() {
        AmethystCatalystDiscount.Quote quote = AmethystCatalystDiscount.quoteForEndpoints(
                6,
                true,
                5,
                true,
                7
        );

        assertEquals(5, quote.sourceCatalysts());
        assertEquals(7, quote.targetCatalysts());
        assertEquals(3, quote.appliedDiscount());
        assertEquals(3, quote.finalCost());
    }
}
