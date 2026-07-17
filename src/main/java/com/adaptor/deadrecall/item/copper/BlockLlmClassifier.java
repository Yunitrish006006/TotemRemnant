package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.Deadrecall;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.util.List;
import java.util.UUID;

public final class BlockLlmClassifier {
    private static final long FAILURE_RETRY_DELAY_MS = 60_000L;
    private static final LlmRequestGate REQUEST_GATE =
            new LlmRequestGate(FAILURE_RETRY_DELAY_MS, System::currentTimeMillis);

    static {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> REQUEST_GATE.clear());
    }

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
        if (!REQUEST_GATE.tryStart(queryKey)) {
            return;
        }

        try {
            CopperGolemLlmService.submit(() -> executeRequest(
                    server,
                    golemId,
                    blockId,
                    blockName,
                    blockTags,
                    expectedDrops,
                    toolSummary,
                    prompt,
                    promptRevision,
                    apiUrl,
                    apiKey,
                    model,
                    queryKey
            ));
        } catch (RuntimeException exception) {
            REQUEST_GATE.completeFailure(queryKey);
            Deadrecall.LOGGER.warn("[CopperGolemLLM] 無法排程採集方塊分類請求: {}", exception.getMessage());
        }
    }

    private static void executeRequest(
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
            String model,
            String queryKey
    ) {
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
                try {
                    applyDecisionIfCurrent(
                            CopperGolemLlmService.findCopperGolem(server, golemId),
                            blockId,
                            blockTags,
                            decision,
                            promptRevision
                    );
                    REQUEST_GATE.completeSuccess(queryKey);
                } catch (RuntimeException callbackException) {
                    REQUEST_GATE.completeFailure(queryKey);
                    Deadrecall.LOGGER.warn(
                            "[CopperGolemLLM] 套用採集方塊分類結果失敗: {}",
                            callbackException.getMessage()
                    );
                }
            });
        } catch (Exception exception) {
            REQUEST_GATE.completeFailure(queryKey);
            Deadrecall.LOGGER.warn("[CopperGolemLLM] 採集方塊分類請求失敗: {}", exception.getMessage());
        }
    }

    static void applyDecisionIfCurrent(
            CopperGolem golem,
            String blockId,
            List<String> blockTags,
            CopperGolemLlmClient.Decision decision,
            int promptRevision
    ) {
        if (golem == null || decision == null) {
            return;
        }
        CopperGolemWrenchHandler.recordGatheringLlmDecision(
                golem,
                blockId,
                blockTags,
                decision.matches(),
                decision.tags(),
                promptRevision
        );
    }

    static String queryKey(UUID golemId, String blockId, List<String> blockTags, int promptRevision) {
        return golemId
                + "|block|"
                + promptRevision
                + "|"
                + blockId
                + "|"
                + canonicalTags(blockTags);
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
}
