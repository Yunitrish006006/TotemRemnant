package com.adaptor.deadrecall.discord;

import com.adaptor.deadrecall.DiscordBridge;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordWorkerFailureIsolationTest {
    @TempDir
    Path tempDir;

    @Test
    void workerHttpFailureDoesNotEscapeLocalizedMinecraftEvent() throws Exception {
        CountDownLatch requestReceived = new CountDownLatch(1);
        AtomicReference<String> requestBody = new AtomicReference<>("");
        HttpServer worker = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        worker.createContext("/api/mc/chat", exchange -> {
            try (exchange) {
                requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                exchange.sendResponseHeaders(503, -1);
            } finally {
                requestReceived.countDown();
            }
        });
        worker.start();

        try {
            DiscordBridge.init(tempDir);
            DiscordBridge.updateConfig(
                    true,
                    "http://127.0.0.1:" + worker.getAddress().getPort(),
                    "test-api-key"
            );

            assertDoesNotThrow(() -> DiscordEventNotifications.death(Component.translatable(
                    "death.attack.generic",
                    Component.literal("Alex")
            )));
            assertTrue(requestReceived.await(5, TimeUnit.SECONDS), "Worker did not receive the localized event");
            assertTrue(requestBody.get().contains("Alex 死亡"), requestBody.get());
        } finally {
            DiscordBridge.updateConfig(false, "", "");
            worker.stop(0);
        }
    }
}
