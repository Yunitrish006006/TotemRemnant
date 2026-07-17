package com.adaptor.deadrecall.inventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortableContainerTagResourceTest {
    private static final String TAG_PATH = "/data/deadrecall/tags/item/portable_containers.json";

    @Test
    void tagIsAppendableByDatapacksAndAddons() throws IOException {
        JsonObject tag = loadTag();

        assertTrue(tag.has("replace"));
        assertFalse(tag.get("replace").getAsBoolean());
    }

    @Test
    void tagContainsBundleAndEveryVanillaShulkerBox() throws IOException {
        JsonArray values = loadTag().getAsJsonArray("values");
        assertNotNull(values);

        Set<String> ids = new HashSet<>();
        values.forEach(value -> ids.add(value.getAsString()));

        assertEquals(18, ids.size());
        assertTrue(ids.contains("minecraft:bundle"));
        for (String color : new String[]{
                "", "white_", "orange_", "magenta_", "light_blue_", "yellow_", "lime_",
                "pink_", "gray_", "light_gray_", "cyan_", "purple_", "blue_", "brown_",
                "green_", "red_", "black_"
        }) {
            assertTrue(ids.contains("minecraft:" + color + "shulker_box"), "Missing " + color + "shulker_box");
        }
    }

    private static JsonObject loadTag() throws IOException {
        try (InputStream stream = PortableContainerTagResourceTest.class.getResourceAsStream(TAG_PATH)) {
            assertNotNull(stream, "Missing portable-container tag resource " + TAG_PATH);
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
