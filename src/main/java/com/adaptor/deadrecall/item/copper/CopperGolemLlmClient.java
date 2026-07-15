package com.adaptor.deadrecall.item.copper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class CopperGolemLlmClient {
    private CopperGolemLlmClient() {
    }

    static Decision askItemClassification(
            String apiUrl,
            String apiKey,
            String model,
            String prompt,
            String itemId,
            String itemName,
            List<String> itemTags,
            String referenceTable) throws Exception {
        JsonObject body = baseRequestBody(model, 256);

        JsonArray messages = new JsonArray();
        messages.add(message("system", """
                You are a Minecraft item sorting classifier.
                Return only JSON with this schema:
                {"match":true|false,"tags":["tag_id"]}
                The tags array must contain only tag ids from the provided item tags that are useful for future matching.
                If no provided tag is useful, return an empty tags array.
                Do not include markdown, explanations, or thinking text.
                """ + referenceTable));
        messages.add(message("user", String.format(
                "Container prompt: %s%nItem id: %s%nItem name: %s%nItem tags: %s%nShould this item be sorted into this container?%n/no_think",
                prompt,
                itemId,
                itemName,
                itemTags
        )));
        body.add("messages", messages);

        JsonObject response = postChatCompletions(apiUrl, apiKey, body);
        return parseDecision(firstChoiceContent(response), itemTags);
    }

    static Decision askBlockClassification(
            String apiUrl,
            String apiKey,
            String model,
            String prompt,
            String blockId,
            String blockName,
            List<String> blockTags,
            List<String> expectedDrops,
            String toolSummary) throws Exception {
        JsonObject body = baseRequestBody(model, 256);

        JsonArray messages = new JsonArray();
        messages.add(message("system", """
                You are a Minecraft block gathering classifier for a copper golem.
                Return only JSON with this schema:
                {"match":true|false,"tags":["tag_id"]}
                The tags array must contain only tag ids from the provided block tags that are useful for future matching.
                If no provided tag is useful, return an empty tags array.
                The mod already rejected unsafe blocks, containers, unbreakable blocks, wrong tools, and drops that cannot fit.
                Do not include markdown, explanations, or thinking text.
                """));
        messages.add(message("user", String.format(
                "Gathering prompt: %s%nBlock id: %s%nBlock name: %s%nBlock tags: %s%nExpected drops: %s%nTool: %s%nShould this block type be gathered?%n/no_think",
                prompt,
                blockId,
                blockName,
                blockTags,
                expectedDrops,
                toolSummary
        )));
        body.add("messages", messages);

        JsonObject response = postChatCompletions(apiUrl, apiKey, body);
        return parseDecision(firstChoiceContent(response), blockTags);
    }

    static void askConnectionTest(String apiUrl, String apiKey, String model) throws Exception {
        JsonObject body = baseRequestBody(model, 64);

        JsonArray messages = new JsonArray();
        messages.add(message("system", "Reply with exactly OK. Do not include thinking text."));
        messages.add(message("user", "Connection test. Reply OK. /no_think"));
        body.add("messages", messages);

        firstChoiceContent(postChatCompletions(apiUrl, apiKey, body));
    }

    private static JsonObject baseRequestBody(String model, int maxTokens) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", 0);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("stream", false);
        return body;
    }

    private static JsonObject postChatCompletions(String apiUrl, String apiKey, JsonObject body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(15000);
        conn.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));

        int responseCode = conn.getResponseCode();
        InputStream responseStream = responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream();
        String responseBody = responseStream != null ? new String(responseStream.readAllBytes(), StandardCharsets.UTF_8) : "";
        conn.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("HTTP " + responseCode + ": " + responseBody);
        }

        return JsonParser.parseString(responseBody).getAsJsonObject();
    }

    private static String firstChoiceContent(JsonObject response) {
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("LLM response has no choices");
        }

        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        return message == null || !message.has("content") ? "" : message.get("content").getAsString();
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static Decision parseDecision(String content, List<String> providedTags) {
        String normalized = extractJsonObject(stripReasoningBlocks(stripCodeFence(content.trim())));
        JsonObject json = JsonParser.parseString(normalized).getAsJsonObject();
        boolean matches = json.has("match") && json.get("match").getAsBoolean();

        List<String> tags = new ArrayList<>();
        JsonArray tagArray = json.has("tags") && json.get("tags").isJsonArray() ? json.getAsJsonArray("tags") : new JsonArray();
        for (JsonElement element : tagArray) {
            String tag = element.getAsString();
            if (providedTags.contains(tag) && !tags.contains(tag)) {
                tags.add(tag);
            }
        }

        return new Decision(matches, tags);
    }

    private static String stripCodeFence(String content) {
        if (!content.startsWith("```")) {
            return content;
        }

        int firstNewline = content.indexOf('\n');
        int lastFence = content.lastIndexOf("```");
        if (firstNewline >= 0 && lastFence > firstNewline) {
            return content.substring(firstNewline + 1, lastFence).trim();
        }
        return content;
    }

    private static String stripReasoningBlocks(String content) {
        String result = content;
        while (true) {
            String lower = result.toLowerCase();
            int start = lower.indexOf("<think>");
            if (start < 0) {
                return result.trim();
            }

            int end = lower.indexOf("</think>", start + 7);
            if (end < 0) {
                return result.substring(0, start).trim();
            }
            result = (result.substring(0, start) + result.substring(end + 8)).trim();
        }
    }

    private static String extractJsonObject(String content) {
        int start = content.indexOf('{');
        if (start < 0) {
            return content;
        }

        boolean inString = false;
        boolean escaping = false;
        int depth = 0;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (inString && c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(start, i + 1);
                }
            }
        }

        return content.substring(start);
    }

    record Decision(boolean matches, List<String> tags) {
    }
}
