package com.adaptor.deadrecall.inventory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerDiagnosticsLanguageResourceTest {
    private static final String[] LOCALES = {"en_us", "zh_tw", "zh_cn"};
    private static final Set<String> REQUIRED_KEYS = Set.of(
            "message.deadrecall.container_scan.scope_all",
            "message.deadrecall.container_scan.player_missing",
            "message.deadrecall.container_scan.clean",
            "message.deadrecall.container_scan.summary",
            "message.deadrecall.container_scan.finding",
            "message.deadrecall.container_scan.more",
            "message.deadrecall.container_scan.truncated",
            "message.deadrecall.container_scan.direction.inside_backpack",
            "message.deadrecall.container_scan.direction.backpack_inside_container"
    );

    @Test
    void everySupportedLocaleContainsTheSameCompleteKeySet() throws IOException {
        for (String locale : LOCALES) {
            JsonObject language = load(locale);
            assertEquals(REQUIRED_KEYS, language.keySet(), "Unexpected key set for " + locale);
            for (String key : REQUIRED_KEYS) {
                assertTrue(!language.get(key).getAsString().isBlank(), "Blank translation for " + key + " in " + locale);
            }
        }
    }

    private static JsonObject load(String locale) throws IOException {
        String path = "/assets/deadrecall_diagnostics/lang/" + locale + ".json";
        try (InputStream stream = ContainerDiagnosticsLanguageResourceTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing additive language resource " + path);
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
