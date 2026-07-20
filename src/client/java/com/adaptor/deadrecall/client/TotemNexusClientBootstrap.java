package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.network.SpaceUnitFriendsPayload;
import com.adaptor.deadrecall.network.SpaceUnitMapPayload;
import com.adaptor.deadrecall.network.SpaceUnitRegistrationPreviewPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Owns client registration for the future TotemNexus module.
 */
public final class TotemNexusClientBootstrap {
    private TotemNexusClientBootstrap() {
    }

    public static void registerNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(SpaceUnitMapPayload.TYPE,
                (payload, context) -> {
                    Minecraft mc = context.client();
                    mc.execute(() -> {
                        SpaceUnitMapScreen screen = SpaceUnitMapScreen.CURRENT;
                        if (screen != null && screen.isFor(payload.sourceType(), payload.sourceUnitId())) {
                            screen.applyPayload(payload);
                        } else {
                            mc.setScreenAndShow(new SpaceUnitMapScreen(payload));
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(SpaceUnitFriendsPayload.TYPE,
                (payload, context) -> {
                    Minecraft mc = context.client();
                    mc.execute(() -> {
                        SpaceUnitFriendsScreen screen = SpaceUnitFriendsScreen.CURRENT;
                        if (screen != null) {
                            screen.applyPayload(payload);
                        } else {
                            mc.setScreenAndShow(new SpaceUnitFriendsScreen(null, payload));
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(SpaceUnitRegistrationPreviewPayload.TYPE,
                (payload, context) -> {
                    Minecraft mc = context.client();
                    mc.execute(() -> mc.setScreenAndShow(new SpaceUnitRegistrationPreviewScreen(payload)));
                });
    }
}
