package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.screen.BackpackScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class DeadrecallClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 註冊背包界面
        HandledScreens.register(BackpackScreenHandler.SCREEN_HANDLER_TYPE, BackpackScreen::new);
    }
}
