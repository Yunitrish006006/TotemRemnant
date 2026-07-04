package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 伺服器 → 客戶端：回傳目前 Discord Bridge 設定
 */
public record DiscordConfigSyncPayload(boolean enabled, String workerUrl, String apiKey, List<ChannelData> channels)
        implements CustomPacketPayload {

    public static final Type<DiscordConfigSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "discord_config_sync"));

    public record ChannelData(String id, String name) {
    }

    public static final StreamCodec<FriendlyByteBuf, DiscordConfigSyncPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBoolean(payload.enabled());
                        buf.writeUtf(payload.workerUrl());
                        buf.writeUtf(payload.apiKey());
                        buf.writeInt(payload.channels().size());
                        for (ChannelData channel : payload.channels()) {
                            buf.writeUtf(channel.id(), 32);
                            buf.writeUtf(channel.name(), 64);
                        }
                    },
                    buf -> new DiscordConfigSyncPayload(
                            buf.readBoolean(),
                            buf.readUtf(),
                            buf.readUtf(),
                            readChannels(buf)
                    )
            );

    private static List<ChannelData> readChannels(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ChannelData> channels = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            channels.add(new ChannelData(buf.readUtf(32), buf.readUtf(64)));
        }
        return channels;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
