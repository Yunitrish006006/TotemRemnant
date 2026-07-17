package com.adaptor.deadrecall.discord;

import com.adaptor.deadrecall.Deadrecall;
import com.adaptor.deadrecall.DiscordBridge;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends the small allowlisted set of temporary Discord notifications.
 *
 * <p>The Worker remains responsible for enforcing the allowlist and deleting the returned Discord
 * message IDs. This class only adds the fixed 600-second request hint while preserving server-side
 * failure isolation.</p>
 */
public final class DiscordTransientBridge {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "DiscordBridge-Transient");
        thread.setDaemon(true);
        return thread;
    });

    private DiscordTransientBridge() {
    }

    public static void sendEvent(String event, String username, String message) {
        if (!DiscordTransientNotificationPolicy.isTemporaryEvent(event)
                || username == null || username.isBlank()
                || message == null || message.isBlank()
                || !DiscordBridge.isEnabled()) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("event", event);
        payload.addProperty("username", username.trim());
        payload.addProperty("message", message.trim());
        payload.add("channels", channels());
        payload.addProperty("delete_after_seconds", DiscordTransientNotificationPolicy.DELETE_AFTER_SECONDS);
        EXECUTOR.submit(() -> post("/api/mc/chat", payload, event));
    }

    public static void sendServerStatus(
            String status,
            int playersOnline,
            int playersMax,
            String version,
            double tps,
            boolean immediately) {
        if (!DiscordBridge.isEnabled()) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("status", status == null ? "" : status);
        payload.addProperty("players_online", playersOnline);
        payload.addProperty("players_max", playersMax);
        payload.addProperty("version", version == null ? "" : version);
        payload.addProperty("tps", String.format(Locale.ROOT, "%.1f", tps));
        payload.add("channels", channels());
        payload.addProperty("delete_after_seconds", DiscordTransientNotificationPolicy.DELETE_AFTER_SECONDS);

        Runnable request = () -> post("/api/mc/server/status", payload, "server_status");
        if (immediately) {
            request.run();
        } else {
            EXECUTOR.submit(request);
        }
    }

    private static void post(String path, JsonObject payload, String event) {
        String workerUrl = DiscordBridge.getWorkerUrl();
        String apiKey = readApiKey();
        if (workerUrl == null || workerUrl.isBlank() || apiKey.isBlank()) {
            Deadrecall.LOGGER.warn("[DiscordBridge] 無法送出暫時通知 {}：Worker URL 或 API Key 缺失", event);
            return;
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(workerUrl + path).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-API-Key", apiKey);
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.getOutputStream().write(payload.toString().getBytes(StandardCharsets.UTF_8));

            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = responseStream == null
                    ? "N/A"
                    : new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);

            if (responseCode >= 200 && responseCode < 300) {
                Deadrecall.LOGGER.info(
                        "[DiscordBridge] 暫時通知 {} 已送出，{} 秒後刪除 (HTTP {}): {}",
                        event,
                        DiscordTransientNotificationPolicy.DELETE_AFTER_SECONDS,
                        responseCode,
                        responseBody
                );
            } else {
                Deadrecall.LOGGER.warn(
                        "[DiscordBridge] 暫時通知 {} 發送失敗 (HTTP {}): {}",
                        event,
                        responseCode,
                        responseBody
                );
            }
        } catch (Exception exception) {
            Deadrecall.LOGGER.error(
                    "[DiscordBridge] 暫時通知 {} 發送失敗: {}",
                    event,
                    exception.getMessage(),
                    exception
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static JsonArray channels() {
        JsonArray array = new JsonArray();
        for (DiscordBridge.DiscordChannel channel : DiscordBridge.getChannels()) {
            if (channel != null && channel.id != null && !channel.id.isBlank()) {
                array.add(channel.id);
            }
        }
        return array;
    }

    private static String readApiKey() {
        try {
            Field field = DiscordBridge.class.getDeclaredField("apiKey");
            if (!field.canAccess(null)) {
                field.setAccessible(true);
            }
            Object value = field.get(null);
            return value instanceof String string ? string.trim() : "";
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Deadrecall.LOGGER.error("[DiscordBridge] 無法讀取 API Key 供暫時通知使用", exception);
            return "";
        }
    }
}
