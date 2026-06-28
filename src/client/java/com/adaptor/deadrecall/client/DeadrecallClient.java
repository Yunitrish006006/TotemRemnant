package com.adaptor.deadrecall.client;

import net.fabricmc.api.ClientModInitializer;

public class DeadrecallClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 26.2 版本使用 vanilla chest menu，無需客戶端自訂畫面註冊
    }
}
