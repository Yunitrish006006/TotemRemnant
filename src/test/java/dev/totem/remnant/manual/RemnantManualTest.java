package dev.totem.remnant.manual;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemnantManualTest {
    @Test
    void tutorialCoversEveryPlayerFacingRemnantSystem() {
        assertEquals(1, RemnantManual.sections().size());
        assertEquals(
                "totem:remnant/manual",
                RemnantManual.sections().getFirst().id().toString()
        );
        List<String> pages = RemnantManual.sections().getFirst().pageKeys();
        assertEquals(27, pages.size());
        assertEquals("book.deadrecall.remnant.basics.page.1", pages.getFirst());
        assertEquals("book.deadrecall.remnant.container_safety.page.1", pages.getLast());
        int overviewIndex = pages.indexOf("book.deadrecall.remnant.module_recipes.overview");
        assertEquals(4, overviewIndex);
        for (int module = 1; module <= 9; module++) {
            int descriptionIndex = overviewIndex + module * 2 - 1;
            assertEquals(
                    "book.deadrecall.remnant.module_recipes.description." + module,
                    pages.get(descriptionIndex)
            );
            assertEquals(
                    "book.deadrecall.remnant.module_recipes.page." + module,
                    pages.get(descriptionIndex + 1)
            );
        }
    }
}
