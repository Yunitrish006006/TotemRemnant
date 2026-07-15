package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.Deadrecall;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockLlmClassifier {
    private static final Set<String> PENDING_QUERIES = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, Long> RETRY_AFTER_MS = new ConcurrentHashMap<>();
    private static final long FAILURE_RETRY_DELAY_MS = 60_000L;

    private BlockLlmClassifier() {
    }

    public static void requestClassification(
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

        String queryKey = queryKey(golemId, blockId, blockTags, promptRevision);
        long now = System.currentTimeMillis();
        if (PENDING_QUERIES.contains(queryKey) || RETRY_AFTER_MS.getOrDefault(queryKey, 0L) > now) {
            return;
        }

        PENDING_QUERIES.add(queryKey);
        CopperGolemLlmService.submit(() -> {
            try {
                CopperGolemLlmClient.Decision decision = CopperGolemLlmClient.askBlockClassification(
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
                    CopperGolem golem = CopperGolemLlmService.findCopperGolem(server, golemId);
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
                PENDING_QUERIES.remove(queryKey);
            }
        });
    }

    private static String queryKey(UUID golemId, String blockId, List<String> blockTags, int promptRevision) {
        return golemId + "|block|" + promptRevision + "|" + blockId + "|" + String.join(",", blockTags);
    }
}
