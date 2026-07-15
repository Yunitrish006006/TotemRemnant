package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.Deadrecall;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

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

    static void submit(Runnable task) {
        EXECUTOR.submit(task);
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
                CopperGolemLlmClient.Decision decision = CopperGolemLlmClient.askItemClassification(
                        apiUrl,
                        apiKey,
                        model,
                        prompt,
                        itemId,
                        itemName,
                        itemTags,
                        CLASSIFICATION_REFERENCE_TABLE);
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

    public static void testConnection(MinecraftServer server, UUID playerId, String apiUrl, String apiKey, String model) {
        String normalizedApiUrl = apiUrl == null ? "" : apiUrl.trim();
        String normalizedApiKey = apiKey == null ? "" : apiKey.trim();
        String normalizedModel = model == null ? "" : model.trim();
        if (normalizedApiUrl.isBlank() || normalizedModel.isBlank()) {
            sendToPlayer(server, playerId, Component.translatable("message.deadrecall.copper_wrench.llm_test_missing_config").withStyle(ChatFormatting.RED));
            return;
        }

        if (!PENDING_CONNECTION_TESTS.add(playerId)) {
            sendToPlayer(server, playerId, Component.translatable("message.deadrecall.copper_wrench.llm_test_pending").withStyle(ChatFormatting.YELLOW));
            return;
        }

        sendToPlayer(server, playerId, Component.translatable("message.deadrecall.copper_wrench.llm_test_started").withStyle(ChatFormatting.YELLOW));
        EXECUTOR.submit(() -> {
            long startedAt = System.currentTimeMillis();
            try {
                CopperGolemLlmClient.askConnectionTest(normalizedApiUrl, normalizedApiKey, normalizedModel);
                long elapsedMs = System.currentTimeMillis() - startedAt;
                server.execute(() -> sendToPlayer(server, playerId, Component.translatable("message.deadrecall.copper_wrench.llm_test_success", elapsedMs).withStyle(ChatFormatting.GREEN)));
            } catch (Exception e) {
                String message = safeErrorMessage(e);
                server.execute(() -> sendToPlayer(server, playerId, Component.translatable("message.deadrecall.copper_wrench.llm_test_failed", message).withStyle(ChatFormatting.RED)));
                Deadrecall.LOGGER.warn("[CopperGolemLLM] 連線測試失敗: {}", message);
            } finally {
                PENDING_CONNECTION_TESTS.remove(playerId);
            }
        });
    }

    static CopperGolem findCopperGolem(MinecraftServer server, UUID golemId) {
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
}
