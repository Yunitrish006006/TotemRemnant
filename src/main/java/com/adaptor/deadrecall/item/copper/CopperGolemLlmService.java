package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.Deadrecall;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CopperGolemLlmService {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "CopperGolemLLM-Worker");
        thread.setDaemon(true);
        return thread;
    });
    private static final Set<String> PENDING_QUERIES = ConcurrentHashMap.newKeySet();
    private static final Set<String> PENDING_BLOCK_QUERIES = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PENDING_CONNECTION_TESTS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, Long> RETRY_AFTER_MS = new ConcurrentHashMap<>();
    private static final long FAILURE_RETRY_DELAY_MS = 60_000L;
    private static final String CLASSIFICATION_REFERENCE_TABLE = """
            Keyword reference for container prompts:
            - 礦物 / 礦石 / 金屬: ores, raw ores, ingots, nuggets, gems, coal, charcoal, redstone, lapis, quartz, amethyst, copper, iron, gold, diamond, emerald, netherite materials.
            - 食物 / 料理: edible items, bread, cooked or raw meat, fish, fruit, vegetables, soup, stew, cake, pie, cookies.
            - 工具: tools and usable work items such as pickaxes, axes, shovels, hoes, shears, fishing rods, brushes, flint and steel, buckets, compasses, clocks. Do not include raw crafting materials unless the prompt also asks for materials.
            - 作物 / 農作物: crops, seeds, wheat, carrots, potatoes, beetroot, melon, pumpkin, sugar cane, bamboo, cactus, cocoa beans, nether wart, farming produce.
            - 動物 / 動物掉落: animal-related drops and products such as meat, leather, wool, feathers, eggs, rabbit hide, scutes, milk buckets, ink sacs.
            - 材料 / 合成材料: general crafting ingredients and intermediate materials such as sticks, string, paper, leather, dyes, bone meal, slime balls, honeycomb, blaze powder, gunpowder, clay balls.
            - 建材 / 方塊 / 裝飾: building and decoration blocks such as stone, cobblestone, deepslate, dirt, sand, gravel, wood blocks, planks, bricks, concrete, glass, stairs, slabs, walls, doors, fences, lanterns.
            - 畜牧 / 牧場: animal husbandry and farm-animal management items such as animal feed, wheat, seeds, carrots, potatoes, beetroot, hay bales, leads, name tags, saddles, shears, buckets, eggs.
            Use these references only as interpretation help. The player's container prompt has priority.
            """;

    private CopperGolemLlmService() {
    }

    public static void requestClassification(
            MinecraftServer server,
            UUID golemId,
            CopperGolemWrenchHandler.Binding binding,
            ItemStack stack,
            String itemId,
            List<String> itemTags,
            String prompt,
            String apiUrl,
            String apiKey,
            String model
    ) {
        if (apiUrl == null || apiUrl.isBlank() || model == null || model.isBlank() || prompt == null || prompt.isBlank()) {
            return;
        }

        String queryKey = queryKey(golemId, binding, itemId, itemTags);
        long now = System.currentTimeMillis();
        if (PENDING_QUERIES.contains(queryKey) || RETRY_AFTER_MS.getOrDefault(queryKey, 0L) > now) {
            return;
        }

        PENDING_QUERIES.add(queryKey);
        String itemName = stack.getHoverName().getString();

        EXECUTOR.submit(() -> {
            try {
                LlmDecision decision = askLlm(apiUrl, apiKey, model, prompt, itemId, itemName, itemTags);
                server.execute(() -> {
                    CopperGolem golem = findCopperGolem(server, golemId);
                    if (golem != null) {
                        CopperGolemWrenchHandler.recordLlmDecision(golem, binding, itemId, itemTags, decision.matches(), decision.tags());
                    }
                });
                RETRY_AFTER_MS.remove(queryKey);
            } catch (Exception e) {
                RETRY_AFTER_MS.put(queryKey, System.currentTimeMillis() + FAILURE_RETRY_DELAY_MS);
                Deadrecall.LOGGER.warn("[CopperGolemLLM] 分類請求失敗: {}", e.getMessage());
            } finally {
                PENDING_QUERIES.remove(queryKey);
            }
        });
    }

    public static void requestBlockClassification(
            MinecraftServer server,
            UUID golemId,
            String blockId,
            String blockName,
            List<String> blockTags,
            List<String> expectedDrops,
            String toolSummary,
            String prompt,
            int promptRevision,
            String apiUrl,
            String apiKey,
            String model
    ) {
        if (apiUrl == null || apiUrl.isBlank() || model == null || model.isBlank() || prompt == null || prompt.isBlank()) {
            return;
        }

        String queryKey = blockQueryKey(golemId, blockId, blockTags, promptRevision);
        long now = System.currentTimeMillis();
        if (PENDING_BLOCK_QUERIES.contains(queryKey) || RETRY_AFTER_MS.getOrDefault(queryKey, 0L) > now) {
            return;
        }

        PENDING_BLOCK_QUERIES.add(queryKey);
        EXECUTOR.submit(() -> {
            try {
                LlmDecision decision = askBlockLlm(
                        apiUrl,
                        apiKey,
                        model,
                        prompt,
                        blockId,
                        blockName,
                        blockTags,
                        expectedDrops,
                        toolSummary
                );
                server.execute(() -> {
                    CopperGolem golem = findCopperGolem(server, golemId);
                    if (golem != null) {
                        CopperGolemWrenchHandler.recordGatheringLlmDecision(
                                golem,
                                blockId,
                                blockTags,
                                decision.matches(),
                                decision.tags(),
                                promptRevision
                        );
                    }
                });
                RETRY_AFTER_MS.remove(queryKey);
            } catch (Exception e) {
                RETRY_AFTER_MS.put(queryKey, System.currentTimeMillis() + FAILURE_RETRY_DELAY_MS);
                Deadrecall.LOGGER.warn("[CopperGolemLLM] 採集方塊分類請求失敗: {}", e.getMessage());
            } finally {
                PENDING_BLOCK_QUERIES.remove(queryKey);
            }
        });
    }

    public static void testConnection(MinecraftServer server, UUID playerId, String apiUrl, String apiKey, String model) {
        String normalizedApiUrl = apiUrl == null ? "" : apiUrl.trim();
        String normalizedApiKey = apiKey == null ? "" : apiKey.trim();
        String normalizedModel = model == null ? "" : model.trim();
        if (normalizedApiUrl.isBlank() || normalizedModel.isBlank()) {
            sendToPlayer(server, playerId, Component.literal("§c請先填寫 LLM API URL 與 Model 再測試連線。"));
            return;
        }

        if (!PENDING_CONNECTION_TESTS.add(playerId)) {
            sendToPlayer(server, playerId, Component.literal("§eLLM 連線測試正在進行中，請稍候。"));
            return;
        }

        sendToPlayer(server, playerId, Component.literal("§e正在測試銅魁儡 LLM 連線..."));
        EXECUTOR.submit(() -> {
            long startedAt = System.currentTimeMillis();
            try {
                askConnectionTest(normalizedApiUrl, normalizedApiKey, normalizedModel);
                long elapsedMs = System.currentTimeMillis() - startedAt;
                server.execute(() -> sendToPlayer(server, playerId, Component.literal("§a銅魁儡 LLM 連線成功（" + elapsedMs + "ms）")));
            } catch (Exception e) {
                String message = safeErrorMessage(e);
                server.execute(() -> sendToPlayer(server, playerId, Component.literal("§c銅魁儡 LLM 連線失敗：" + message)));
                Deadrecall.LOGGER.warn("[CopperGolemLLM] 連線測試失敗: {}", message);
            } finally {
                PENDING_CONNECTION_TESTS.remove(playerId);
            }
        });
    }

    private static LlmDecision askLlm(String apiUrl, String apiKey, String model, String prompt, String itemId, String itemName, List<String> itemTags) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", 0);
        body.addProperty("max_tokens", 256);
        body.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        messages.add(message("system", """
                You are a Minecraft item sorting classifier.
                Return only JSON with this schema:
                {"match":true|false,"tags":["tag_id"]}
                The tags array must contain only tag ids from the provided item tags that are useful for future matching.
                If no provided tag is useful, return an empty tags array.
                Do not include markdown, explanations, or thinking text.
                """ + CLASSIFICATION_REFERENCE_TABLE));
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

    private static LlmDecision askBlockLlm(
            String apiUrl,
            String apiKey,
            String model,
            String prompt,
            String blockId,
            String blockName,
            List<String> blockTags,
            List<String> expectedDrops,
            String toolSummary) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", 0);
        body.addProperty("max_tokens", 256);
        body.addProperty("stream", false);

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

    private static void askConnectionTest(String apiUrl, String apiKey, String model) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", 0);
        body.addProperty("max_tokens", 64);
        body.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        messages.add(message("system", "Reply with exactly OK. Do not include thinking text."));
        messages.add(message("user", "Connection test. Reply OK. /no_think"));
        body.add("messages", messages);

        firstChoiceContent(postChatCompletions(apiUrl, apiKey, body));
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
            throw new IllegalStateException("LLM 回應沒有 choices");
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

    private static LlmDecision parseDecision(String content, List<String> providedTags) {
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

        return new LlmDecision(matches, tags);
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

    private static CopperGolem findCopperGolem(MinecraftServer server, UUID golemId) {
        for (var level : server.getAllLevels()) {
            Entity entity = level.getEntity(golemId);
            if (entity instanceof CopperGolem golem) {
                return golem;
            }
        }
        return null;
    }

    private static void sendToPlayer(MinecraftServer server, UUID playerId, Component message) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            player.sendSystemMessage(message);
        }
    }

    private static String safeErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }

        message = message.replaceAll("\\s+", " ").trim();
        int maxLength = 220;
        return message.length() <= maxLength ? message : message.substring(0, maxLength) + "...";
    }

    private static String queryKey(UUID golemId, CopperGolemWrenchHandler.Binding binding, String itemId, List<String> itemTags) {
        return golemId + "|" + binding.dimension().identifier() + "|" + binding.containerPos().asLong() + "|" + itemId + "|" + String.join(",", itemTags);
    }

    private static String blockQueryKey(UUID golemId, String blockId, List<String> blockTags, int promptRevision) {
        return golemId + "|block|" + promptRevision + "|" + blockId + "|" + String.join(",", blockTags);
    }

    public static String itemId(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.toString();
    }

    public static List<String> itemTags(ItemStack stack) {
        return stack.typeHolder()
                .tags()
                .map(tag -> tag.location().toString())
                .sorted()
                .toList();
    }

    private record LlmDecision(boolean matches, List<String> tags) {
    }
}
