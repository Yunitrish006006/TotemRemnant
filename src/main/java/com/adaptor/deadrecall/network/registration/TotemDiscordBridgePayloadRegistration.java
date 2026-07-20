package com.adaptor.deadrecall.network.registration;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.DiscordBridge;
import com.adaptor.deadrecall.network.DiscordConfigSyncPayload;
import com.adaptor.deadrecall.network.ManageDiscordChannelPayload;
import com.adaptor.deadrecall.network.RequestDiscordConfigPayload;
import com.adaptor.deadrecall.network.SaveDiscordConfigPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

final class TotemDiscordBridgePayloadRegistration {
    private TotemDiscordBridgePayloadRegistration() {
    }

    static void registerServerboundTypes() {
        PayloadTypeRegistry.serverboundPlay().register(
                RequestDiscordConfigPayload.TYPE, RequestDiscordConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                SaveDiscordConfigPayload.TYPE, SaveDiscordConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                ManageDiscordChannelPayload.TYPE, ManageDiscordChannelPayload.CODEC);
    }

    static void registerClientboundTypes() {
        PayloadTypeRegistry.clientboundPlay().register(
                DiscordConfigSyncPayload.TYPE, DiscordConfigSyncPayload.CODEC);
    }

    static void registerReceivers() {
        // 收到客戶端請求時，回傳目前設定
        ServerPlayNetworking.registerGlobalReceiver(RequestDiscordConfigPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    if (!PayloadPermissionChecks.canManageServerConfiguration(player)) {
                        player.sendSystemMessage(Component.translatable("message.deadrecall.discord_config.permission_view").withStyle(ChatFormatting.RED));
                        Deadrecall.LOGGER.warn("[DiscordBridge] 玩家 {} 嘗試未授權讀取設定", player.getName().getString());
                        return;
                    }

                    sendDiscordConfigTo(player);
                });

        // 收到客戶端儲存請求時，更新設定（需要 OP 權限）
        ServerPlayNetworking.registerGlobalReceiver(SaveDiscordConfigPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();

                    if (!PayloadPermissionChecks.canManageServerConfiguration(player)) {
                        player.sendSystemMessage(Component.translatable("message.deadrecall.discord_config.permission_modify").withStyle(ChatFormatting.RED));
                        Deadrecall.LOGGER.warn("[DiscordBridge] 玩家 {} 嘗試未授權修改設定", player.getName().getString());
                        return;
                    }

                    try {
                        DiscordBridge.updateConfig(payload.enabled(), payload.workerUrl(), payload.apiKey());
                        player.sendSystemMessage(Component.translatable("message.deadrecall.discord_config.settings_updated").withStyle(ChatFormatting.GREEN));
                        sendDiscordConfigTo(player);
                    } catch (IllegalArgumentException e) {
                        player.sendSystemMessage(Component.literal(e.getMessage()).withStyle(ChatFormatting.RED));
                    } catch (Exception e) {
                        player.sendSystemMessage(Component.translatable("message.deadrecall.discord_config.update_failed", e.getMessage()).withStyle(ChatFormatting.RED));
                        Deadrecall.LOGGER.error("[DiscordBridge] 更新設定失敗", e);
                    }
                });

        // 收到頻道管理請求時，添加或移除頻道
        ServerPlayNetworking.registerGlobalReceiver(ManageDiscordChannelPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();

                    if (!PayloadPermissionChecks.canManageServerConfiguration(player)) {
                        player.sendSystemMessage(Component.translatable("message.deadrecall.discord_config.permission_channels").withStyle(ChatFormatting.RED));
                        Deadrecall.LOGGER.warn("[DiscordBridge] 玩家 {} 嘗試未授權管理頻道", player.getName().getString());
                        return;
                    }

                    try {
                        if ("add".equals(payload.action())) {
                            DiscordBridge.addChannel(payload.channelId(), payload.channelName());
                            player.sendSystemMessage(Component.translatable("message.deadrecall.discord_config.channel_added", payload.channelName()).withStyle(ChatFormatting.GREEN));
                        } else if ("remove".equals(payload.action())) {
                            DiscordBridge.removeChannel(payload.channelId());
                            player.sendSystemMessage(Component.translatable("message.deadrecall.discord_config.channel_removed", payload.channelId()).withStyle(ChatFormatting.GREEN));
                        } else {
                            throw new IllegalArgumentException("Unsupported channel operation");
                        }
                        sendDiscordConfigTo(player);
                    } catch (IllegalArgumentException e) {
                        player.sendSystemMessage(Component.literal(e.getMessage()).withStyle(ChatFormatting.RED));
                    } catch (Exception e) {
                        player.sendSystemMessage(Component.translatable("message.deadrecall.discord_config.operation_failed", e.getMessage()).withStyle(ChatFormatting.RED));
                        Deadrecall.LOGGER.error("[DiscordBridge] 管理頻道失敗", e);
                    }
                });
    }

    private static void sendDiscordConfigTo(ServerPlayer player) {
        var channels = DiscordBridge.getChannels();
        var syncedChannels = new ArrayList<DiscordConfigSyncPayload.ChannelData>(channels.size());
        for (var channel : channels) {
            syncedChannels.add(new DiscordConfigSyncPayload.ChannelData(channel.id, channel.name));
        }
        ServerPlayNetworking.send(player, new DiscordConfigSyncPayload(
                DiscordBridge.isEnabled(),
                DiscordBridge.getWorkerUrl(),
                "",
                syncedChannels
        ));
    }
}
