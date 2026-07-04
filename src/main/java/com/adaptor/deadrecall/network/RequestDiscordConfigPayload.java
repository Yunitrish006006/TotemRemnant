package com.adaptor.deadrecall.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客戶端 → 伺服器：請求目前 Discord Bridge 設定
 */
public record RequestDiscordConfigPayload() implements CustomPacketPayload {

    public static final Type<RequestDiscordConfigPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "request_discord_config"));

    public static final StreamCodec<FriendlyByteBuf, RequestDiscordConfigPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> { /* 無需寫入任何資料 */ },
                    buf -> new RequestDiscordConfigPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
