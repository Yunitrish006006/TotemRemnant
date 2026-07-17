package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.Deadrecall;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
    private static final Set<UUID> PENDING_CONNECTION_TESTS = ConcurrentHashMap.newKeySet();
    private static final long FAILURE_RETRY_DELAY_MS = 60_000L;
    private static final LlmRequestGate CLASSIFICATION_GATE =
            new LlmRequestGate(FAILURE_RETRY_DELAY_MS, System::currentTimeMillis);
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

    static {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CLASSIFICATION_GATE.clear();
            PENDING_CONNECTION_TESTS.clear();
        });
    }

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

        String normalizedPrompt = prompt.trim();
        String queryKey = queryKey(golemId, binding, itemId, itemTags, normalizedPrompt);
        if (!CLASSIFICATION_GATE.tryStart(queryKey)) {
            return;
        }

        String itemName = stack.getHoverName().getString();
        try {
            submit(() -> executeClassificationRequest(
                    server,
                    golemId,
                    binding,
                    itemId,
                    itemName,
                    itemTags,
                    normalizedPrompt,
                    apiUrl,
                    apiKey,
                    model,
                    queryKey
            ));
        } catch (RuntimeException exception) {
            CLASSIFICATION_GATE.completeFailure(queryKey);
            Deadrecall.LOGGER.warn("[CopperGolemLLM] 無法排程分類請求: {}", exception.getMessage());
        }
    }

    private static void executeClassificationRequest(
            MinecraftServer server,
            UUID golemId,
            CopperGolemWrenchHandler.Binding binding,
            String itemId,
            String itemName,
            List<String> itemTags,
            String prompt,
            String apiUrl,
            String apiKey,
            String model,
            String queryKey
    ) {
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
                try {
                    applyDecisionIfCurrent(
                            findCopperGolem(server, golemId),
                            binding,
                            prompt,
                            itemId,
                            itemTags,
                            decision
                    );
                    CLASSIFICATION_GATE.completeSuccess(queryKey);
                } catch (RuntimeException callbackException) {
                    CLASSIFICATION_GATE.completeFailure(queryKey);
                    Deadrecall.LOGGER.warn(
                            "[CopperGolemLLM] 套用分類結果失敗: {}",
                            callbackException.getMessage()
                    );
                }
            });
        } catch (Exception exception) {
            CLASSIFICATION_GATE.completeFailure(queryKey);
            Deadrecall.LOGGER.warn("[CopperGolemLLM] 分類請求失敗: {}", exception.getMessage());
        }
    }

    static void applyDecisionIfCurrent(
            CopperGolem golem,
            CopperGolemWrenchHandler.Binding binding,
            String requestPrompt,
            String itemId,
            List<String> itemTags,
            CopperGolemLlmClient.Decision decision
    ) {
        if (golem == null || decision == null) {
            return;
        }

        CopperGolemWrenchHandler.BindingLlmConfig current =
                CopperGolemWrenchHandler.getBindingLlmConfig(golem, binding);
        String normalizedPrompt = requestPrompt == null ? "" : requestPrompt.trim();
        if (!current.enabled() || !current.prompt().equals(normalizedPrompt)) {
            return;
        }

        CopperGolemWrenchHandler.recordLlmDecision(
                golem,
                binding,
                itemId,
                itemTags,
                decision.matches(),
                decision.tags()
        );
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

    static String queryKey(
            UUID golemId,
            CopperGolemWrenchHandler.Binding binding,
            String itemId,
            List<String> itemTags,
            String prompt
    ) {
        return golemId
                + "|"
                + binding.dimension().identifier()
                + "|"
                + binding.containerPos().asLong()
                + "|prompt|"
                + (prompt == null ? "" : prompt.trim())
                + "|"
                + itemId
                + "|"
                + canonicalTags(itemTags);
    }

    private static String canonicalTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(",", tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .distinct()
                .sorted()
                .toList());
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
