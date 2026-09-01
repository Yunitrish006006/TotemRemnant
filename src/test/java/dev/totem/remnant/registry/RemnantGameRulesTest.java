package dev.totem.remnant.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemnantGameRulesTest {
    private static final List<String> REQUIRED_KEYS = List.of(
            "gamerule.totem.remnant_generate_death_backpacks",
            "gamerule.totem.remnant_generate_death_backpacks.description",
            "gamerule.totem.remnant_death_backpack_owner_pickup_only",
            "gamerule.totem.remnant_death_backpack_owner_pickup_only.description",
            "gamerule.totem.remnant_prevent_portable_container_nesting",
            "gamerule.totem.remnant_prevent_portable_container_nesting.description"
    );

    @Test
    void englishAndTraditionalChineseDescribeEveryRule() {
        JsonObject english = language("en_us");
        JsonObject traditionalChinese = language("zh_tw");

        assertEquals(english.keySet(), traditionalChinese.keySet());
        for (String key : REQUIRED_KEYS) {
            assertTrue(english.has(key), "Missing English game-rule text: " + key);
            assertTrue(traditionalChinese.has(key), "Missing Traditional Chinese game-rule text: " + key);
            assertFalse(english.get(key).getAsString().isBlank(), "Blank English game-rule text: " + key);
            assertFalse(traditionalChinese.get(key).getAsString().isBlank(),
                    "Blank Traditional Chinese game-rule text: " + key);
        }
    }

    private static JsonObject language(String locale) {
        String path = "/assets/deadrecall/lang/" + locale + ".json";
        var stream = RemnantGameRulesTest.class.getResourceAsStream(path);
        assertNotNull(stream, "Missing language resource: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError("Could not read language resource: " + path, exception);
        }
    }
}
