package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客戶端 → 伺服器：儲存 Discord Bridge 設定
 * 使用封包避免文字指令的字元長度限制
 */
public record SaveDiscordConfigPayload(boolean enabled, String workerUrl, String apiKey)
        implements CustomPacketPayload {

    public static final Type<SaveDiscordConfigPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "save_discord_config"));

    public static final StreamCodec<FriendlyByteBuf, SaveDiscordConfigPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBoolean(payload.enabled());
                        buf.writeUtf(payload.workerUrl(), 2048);
                        buf.writeUtf(payload.apiKey(), 512);
                    },
                    buf -> new SaveDiscordConfigPayload(
                            buf.readBoolean(),
                            buf.readUtf(2048),
                            buf.readUtf(512)
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
