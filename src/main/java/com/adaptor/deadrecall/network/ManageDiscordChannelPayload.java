package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客戶端 → 伺服器：管理 Discord 頻道（添加或刪除）
 */
public record ManageDiscordChannelPayload(String action, String channelId, String channelName)
        implements CustomPacketPayload {

    public static final Type<ManageDiscordChannelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "manage_discord_channel"));

    public static final StreamCodec<FriendlyByteBuf, ManageDiscordChannelPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.action(), 16);
                        buf.writeUtf(payload.channelId(), 32);
                        buf.writeUtf(payload.channelName(), 64);
                    },
                    buf -> new ManageDiscordChannelPayload(
                            buf.readUtf(16),
                            buf.readUtf(32),
                            buf.readUtf(64)
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
