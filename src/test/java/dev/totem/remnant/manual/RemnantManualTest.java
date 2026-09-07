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
        assertEquals(32, pages.size());
        assertEquals("book.totem.remnant.basics.page.1", pages.getFirst());
        assertEquals("book.totem.remnant.container_safety.page.1", pages.getLast());
        assertEquals("book.totem.remnant.echo_crystallization.page.1", pages.get(4));
        int overviewIndex = pages.indexOf("book.totem.remnant.module_recipes.overview");
        assertEquals(5, overviewIndex);
        for (int module = 1; module <= 11; module++) {
            int descriptionIndex = overviewIndex + module * 2 - 1;
            assertEquals(
                    "book.totem.remnant.module_recipes.description." + module,
                    pages.get(descriptionIndex)
            );
            assertEquals(
                    "book.totem.remnant.module_recipes.page." + module,
                    pages.get(descriptionIndex + 1)
            );
        }
    }
}
