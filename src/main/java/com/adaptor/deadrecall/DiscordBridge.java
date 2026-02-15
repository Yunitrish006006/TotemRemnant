package com.adaptor.deadrecall;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Discord 聊天橋接工具
 * 負責將 Minecraft 聊天訊息透過 Cloudflare Worker API 傳送到 Discord
 */
public class DiscordBridge {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DiscordBridge-Worker");
        t.setDaemon(true);
        return t;
    });

    private static String workerUrl = "";
    private static String apiKey = "";
    private static boolean enabled = false;

    /**
     * 初始化設定（從 config 檔讀取）
     */
    public static void init(Path configDir) {
        Path configFile = configDir.resolve("discord-bridge.json");

        if (!Files.exists(configFile)) {
            // 建立預設設定檔
            createDefaultConfig(configFile);
            Deadrecall.LOGGER.info("[DiscordBridge] 已建立預設設定檔: {}", configFile);
            Deadrecall.LOGGER.info("[DiscordBridge] 請編輯設定檔填入 Worker URL 和 API Key");
            return;
        }

        try {
            String content = Files.readString(configFile, StandardCharsets.UTF_8);
            JsonObject config = JsonParser.parseString(content).getAsJsonObject();

            enabled = config.has("enabled") && config.get("enabled").getAsBoolean();
            workerUrl = config.has("workerUrl") ? config.get("workerUrl").getAsString() : "";
            apiKey = config.has("apiKey") ? config.get("apiKey").getAsString() : "";

            if (enabled && (workerUrl.isEmpty() || apiKey.isEmpty())) {
                Deadrecall.LOGGER.warn("[DiscordBridge] 已啟用但缺少 workerUrl 或 apiKey，停用功能");
                enabled = false;
                return;
            }

            // 移除尾部斜線
            if (workerUrl.endsWith("/")) {
                workerUrl = workerUrl.substring(0, workerUrl.length() - 1);
            }

            if (enabled) {
                Deadrecall.LOGGER.info("[DiscordBridge] 已啟用，Worker URL: {}", workerUrl);
            } else {
                Deadrecall.LOGGER.info("[DiscordBridge] 功能已停用");
            }
        } catch (Exception e) {
            Deadrecall.LOGGER.error("[DiscordBridge] 讀取設定檔失敗", e);
            enabled = false;
        }
    }

    /**
     * 傳送聊天訊息到 Discord（非同步）
     */
    public static void sendChatMessage(String username, String message) {
        if (!enabled) return;

        EXECUTOR.submit(() -> {
            try {
                String json = String.format(
                        "{\"username\":\"%s\",\"message\":\"%s\"}",
                        escapeJson(username),
                        escapeJson(message)
                );

                String url = workerUrl + "/api/mc/chat";
                Deadrecall.LOGGER.info("[DiscordBridge] 發送請求到: {}", url);
                Deadrecall.LOGGER.info("[DiscordBridge] API Key: {}...", apiKey.substring(0, Math.min(10, apiKey.length())));
                Deadrecall.LOGGER.info("[DiscordBridge] JSON 內容: {}", json);

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-API-Key", apiKey);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));

                int responseCode = conn.getResponseCode();

                // 讀取回應內容
                InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();
                String responseBody = responseStream != null
                    ? new String(responseStream.readAllBytes(), StandardCharsets.UTF_8)
                    : "N/A";

                if (responseCode == 200) {
                    Deadrecall.LOGGER.info("[DiscordBridge] 發送成功 (HTTP 200): {}", responseBody);
                } else {
                    Deadrecall.LOGGER.warn("[DiscordBridge] 發送失敗 (HTTP {}): {}", Optional.of(responseCode), responseBody);
                }

                conn.disconnect();
            } catch (Exception e) {
                Deadrecall.LOGGER.error("[DiscordBridge] 發送訊息失敗: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 回報伺服器狀態到 Discord（非同步）
     */
    public static void sendServerStatus(String status, int playersOnline, int playersMax, String version, double tps) {
        if (!enabled) return;

        EXECUTOR.submit(() -> {
            try {
                String json = String.format("{\"status\":\"%s\",\"players_online\":\"%d\",\"players_max\":\"%d\",\"version\":\"%s\",\"tps\":\"%.1f\"}",
                        escapeJson(status), (Object) playersOnline, (Object) playersMax, escapeJson(version), (Object) tps
                );

                HttpURLConnection conn = (HttpURLConnection) new URL(workerUrl + "/api/mc/server/status").openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-API-Key", apiKey);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                Deadrecall.LOGGER.warn("[DiscordBridge] 回報狀態失敗: {}", e.getMessage());
            }
        });
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void createDefaultConfig(Path configFile) {
        String defaultConfig = """
                {
                  "enabled": false,
                  "workerUrl": "https://mc-discord-bot.yunitrish0419.workers.dev",
                  "apiKey": "mc_ak_7Xp9Qm3vKsW2nF8jRtYb6LdA4eHcZu"
                }
                """;
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, defaultConfig, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Deadrecall.LOGGER.error("[DiscordBridge] 建立設定檔失敗", e);
        }
    }
}
