package com.adaptor.deadrecall.network.registration;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.item.copper.CopperGolemLlmService;
import com.adaptor.deadrecall.item.copper.CopperGolemWrenchHandler;
import com.adaptor.deadrecall.network.CopperGolemGatheringTargetPayload;
import com.adaptor.deadrecall.network.CopperGolemModePayload;
import com.adaptor.deadrecall.network.CopperGolemOperationPayload;
import com.adaptor.deadrecall.network.CopperGolemVisualizationPayload;
import com.adaptor.deadrecall.network.CopperWrenchBindingsPayload;
import com.adaptor.deadrecall.network.RequestCopperGolemVisualizationPayload;
import com.adaptor.deadrecall.network.SaveCopperGolemLlmConfigPayload;
import com.adaptor.deadrecall.network.TestCopperGolemLlmConnectionPayload;
import com.adaptor.deadrecall.network.UpdateCopperGolemBindingCachePayload;
import com.adaptor.deadrecall.network.UpdateCopperGolemBindingLlmPayload;
import com.adaptor.deadrecall.network.UpdateCopperGolemGatheringLlmPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class TotemAutomataPayloadRegistration {
    private TotemAutomataPayloadRegistration() {
    }

    static void registerServerboundTypes() {
        PayloadTypeRegistry.serverboundPlay().register(
                CopperGolemOperationPayload.TYPE, CopperGolemOperationPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                CopperGolemGatheringTargetPayload.TYPE, CopperGolemGatheringTargetPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                CopperGolemModePayload.TYPE, CopperGolemModePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                SaveCopperGolemLlmConfigPayload.TYPE, SaveCopperGolemLlmConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                TestCopperGolemLlmConnectionPayload.TYPE, TestCopperGolemLlmConnectionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                UpdateCopperGolemBindingLlmPayload.TYPE, UpdateCopperGolemBindingLlmPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                UpdateCopperGolemBindingCachePayload.TYPE, UpdateCopperGolemBindingCachePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                UpdateCopperGolemGatheringLlmPayload.TYPE, UpdateCopperGolemGatheringLlmPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                RequestCopperGolemVisualizationPayload.TYPE, RequestCopperGolemVisualizationPayload.CODEC);
    }

    static void registerClientboundTypes() {
        PayloadTypeRegistry.clientboundPlay().register(
                CopperWrenchBindingsPayload.TYPE, CopperWrenchBindingsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                CopperGolemVisualizationPayload.TYPE, CopperGolemVisualizationPayload.CODEC);
    }

    static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(CopperGolemOperationPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        CopperGolemWrenchHandler.setTransportEnabledFromUi(context.player(), payload.golemId(), payload.running(), payload.revision())));

        ServerPlayNetworking.registerGlobalReceiver(CopperGolemGatheringTargetPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        CopperGolemWrenchHandler.handleGatheringTargetFromUi(
                                context.player(),
                                payload.golemId(),
                                payload.value(),
                                payload.tag(),
                                payload.targetSet(),
                                payload.action(),
                                payload.revision()
                        )));

        ServerPlayNetworking.registerGlobalReceiver(CopperGolemModePayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        CopperGolemWrenchHandler.setModeFromUi(context.player(), payload.golemId(), payload.mode(), payload.revision())));

        ServerPlayNetworking.registerGlobalReceiver(SaveCopperGolemLlmConfigPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (!PayloadPermissionChecks.canManageServerConfiguration(player)) {
                        player.sendSystemMessage(Component.translatable("message.deadrecall.copper_wrench.llm_permission_modify").withStyle(ChatFormatting.RED));
                        Deadrecall.LOGGER.warn("[CopperGolemLLM] 玩家 {} 嘗試未授權修改設定", player.getName().getString());
                        return;
                    }

                    CopperGolemWrenchHandler.setGolemLlmConfigFromUi(player, payload.golemId(), payload.apiUrl(), payload.apiKey(), payload.model(), payload.revision());
                    player.sendSystemMessage(Component.translatable("message.deadrecall.copper_wrench.llm_config_updated").withStyle(ChatFormatting.GREEN));
                }));

        ServerPlayNetworking.registerGlobalReceiver(TestCopperGolemLlmConnectionPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (!PayloadPermissionChecks.canManageServerConfiguration(player)) {
                        player.sendSystemMessage(Component.translatable("message.deadrecall.copper_wrench.llm_permission_test").withStyle(ChatFormatting.RED));
                        Deadrecall.LOGGER.warn("[CopperGolemLLM] 玩家 {} 嘗試未授權測試連線", player.getName().getString());
                        return;
                    }

                    CopperGolemLlmService.testConnection(
                            context.server(),
                            player.getUUID(),
                            payload.apiUrl(),
                            payload.apiKey(),
                            payload.model()
                    );
                }));

        ServerPlayNetworking.registerGlobalReceiver(UpdateCopperGolemBindingLlmPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        CopperGolemWrenchHandler.setBindingLlmFromUi(
                                context.player(),
                                payload.golemId(),
                                payload.dimension(),
                                payload.x(),
                                payload.y(),
                                payload.z(),
                                payload.enabled(),
                                payload.prompt(),
                                payload.revision()
                        )));

        ServerPlayNetworking.registerGlobalReceiver(UpdateCopperGolemBindingCachePayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        CopperGolemWrenchHandler.moveBindingLlmCacheFromUi(
                                context.player(),
                                payload.golemId(),
                                payload.dimension(),
                                payload.x(),
                                payload.y(),
                                payload.z(),
                                payload.value(),
                                payload.tag(),
                                payload.allowed(),
                                payload.revision()
                        )));

        ServerPlayNetworking.registerGlobalReceiver(UpdateCopperGolemGatheringLlmPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        CopperGolemWrenchHandler.setGatheringLlmFromUi(
                                context.player(),
                                payload.golemId(),
                                payload.enabled(),
                                payload.prompt(),
                                payload.revision()
                        )));

        ServerPlayNetworking.registerGlobalReceiver(RequestCopperGolemVisualizationPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        CopperGolemWrenchHandler.sendVisualization(context.player(), payload.golemId())));
    }
}
