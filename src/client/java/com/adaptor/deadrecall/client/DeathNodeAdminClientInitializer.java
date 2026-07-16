package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.network.DeathNodeAdminPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class DeathNodeAdminClientInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DeathNodeAdminPayload.TYPE, (payload, context) -> {
            net.minecraft.client.Minecraft minecraft = context.client();
            minecraft.execute(() -> {
                DeathNodeAdminScreen screen = DeathNodeAdminScreen.CURRENT;
                if (screen != null) {
                    screen.applyPayload(payload);
                } else {
                    minecraft.setScreenAndShow(new DeathNodeAdminScreen(payload));
                }
            });
        });
    }
}
