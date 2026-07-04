package com.adaptor.deadrecall;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Discord 聊天橋接工具
 * 負責將 Minecraft 聊天訊息透過 Cloudflare Worker API 傳送到 Discord
 * 支援多頻道管理
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
    private static List<DiscordChannel> channels = new ArrayList<>();
    private static Path configFilePath;

    public static class DiscordChannel {
        public String id;
        public String name;

        public DiscordChannel(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public DiscordChannel(String id) {
            this(id, id);
        }
    }

    /**
     * 初始化設定（從 config 檔讀取）
     */
    public static void init(Path configDir) {
        configFilePath = configDir.resolve("discord-bridge.json");

        if (!Files.exists(configFilePath)) {
            createDefaultConfig(configFilePath);
            Deadrecall.LOGGER.info("[DiscordBridge] 已建立預設設定檔: {}", configFilePath);
            Deadrecall.LOGGER.info("[DiscordBridge] 請編輯設定檔填入 Worker URL 和 API Key");
            return;
        }

        loadConfigFromFile();
    }

    public static synchronized void updateConfig(boolean newEnabled, String newWorkerUrl, String newApiKey) throws IOException {
        if (configFilePath == null) {
            throw new IllegalStateException("DiscordBridge 尚未初始化");
        }
        String normalizedWorkerUrl = normalizeWorkerUrl(newWorkerUrl);
        String normalizedApiKey = newApiKey == null ? "" : newApiKey.trim();
        if (newEnabled && (normalizedWorkerUrl.isEmpty() || normalizedApiKey.isEmpty())) {
            throw new IllegalArgumentException("啟用 Discord Bridge 時，workerUrl 與 apiKey 不能為空");
        }

        JsonObject config = new JsonObject();
        config.addProperty("enabled", newEnabled);
        config.addProperty("workerUrl", normalizedWorkerUrl);
        config.addProperty("apiKey", normalizedApiKey);

        // 保留現有的 channels
        JsonArray channelArray = new JsonArray();
        for (DiscordChannel ch : channels) {
            JsonObject chObj = new JsonObject();
            chObj.addProperty("id", ch.id);
            chObj.addProperty("name", ch.name);
            channelArray.add(chObj);
        }
        config.add("channels", channelArray);

        Files.createDirectories(configFilePath.getParent());
        Files.writeString(configFilePath, config.toString(), StandardCharsets.UTF_8);
        loadConfigFromFile();
    }

    public static synchronized void addChannel(String channelId, String channelName) throws IOException {
        if (configFilePath == null) {
            throw new IllegalStateException("DiscordBridge 尚未初始化");
        }

        // 檢查是否已存在
        for (DiscordChannel ch : channels) {
            if (ch.id.equals(channelId)) {
                throw new IllegalArgumentException("頻道 " + channelId + " 已存在");
            }
        }

        channels.add(new DiscordChannel(channelId, channelName));
        saveChannelsToFile();
        Deadrecall.LOGGER.info("[DiscordBridge] 已添加頻道: {} ({})", channelName, channelId);
    }

    public static synchronized void removeChannel(String channelId) throws IOException {
        if (configFilePath == null) {
            throw new IllegalStateException("DiscordBridge 尚未初始化");
        }

        boolean removed = channels.removeIf(ch -> ch.id.equals(channelId));
        if (!removed) {
            throw new IllegalArgumentException("頻道 " + channelId + " 不存在");
        }

        saveChannelsToFile();
        Deadrecall.LOGGER.info("[DiscordBridge] 已移除頻道: {}", channelId);
    }

    public static synchronized void reload() {
        if (configFilePath == null) {
            return;
        }
        loadConfigFromFile();
    }

    public static List<DiscordChannel> getChannels() {
        return new ArrayList<>(channels);
    }

    private static synchronized void saveChannelsToFile() throws IOException {
        String content = Files.readString(configFilePath, StandardCharsets.UTF_8);
        JsonObject config = JsonParser.parseString(content).getAsJsonObject();

        JsonArray channelArray = new JsonArray();
        for (DiscordChannel ch : channels) {
            JsonObject chObj = new JsonObject();
            chObj.addProperty("id", ch.id);
            chObj.addProperty("name", ch.name);
            channelArray.add(chObj);
        }
        config.add("channels", channelArray);

        Files.writeString(configFilePath, config.toString(), StandardCharsets.UTF_8);
    }

    private static synchronized void loadConfigFromFile() {
        if (configFilePath == null) {
            enabled = false;
            workerUrl = "";
            apiKey = "";
            channels.clear();
            return;
        }
        try {
            String content = Files.readString(configFilePath, StandardCharsets.UTF_8);
            JsonObject config = JsonParser.parseString(content).getAsJsonObject();

            enabled = config.has("enabled") && config.get("enabled").getAsBoolean();
            workerUrl = normalizeWorkerUrl(config.has("workerUrl") ? config.get("workerUrl").getAsString() : "");
            apiKey = config.has("apiKey") ? config.get("apiKey").getAsString() : "";

            channels.clear();
            if (config.has("channels") && config.get("channels").isJsonArray()) {
                JsonArray channelArray = config.getAsJsonArray("channels");
                for (int i = 0; i < channelArray.size(); i++) {
                    JsonObject chObj = channelArray.get(i).getAsJsonObject();
                    String id = chObj.has("id") ? chObj.get("id").getAsString() : "";
                    String name = chObj.has("name") ? chObj.get("name").getAsString() : id;
                    if (!id.isEmpty()) {
                        channels.add(new DiscordChannel(id, name));
                    }
                }
            }

            if (enabled && (workerUrl.isEmpty() || apiKey.isEmpty())) {
                Deadrecall.LOGGER.warn("[DiscordBridge] 已啟用但缺少 workerUrl 或 apiKey，停用功能");
                enabled = false;
                return;
            }

            if (enabled) {
                Deadrecall.LOGGER.info("[DiscordBridge] 已啟用，Worker URL: {}", workerUrl);
                Deadrecall.LOGGER.info("[DiscordBridge] 已配置 {} 個頻道", channels.size());
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
                        "{\"username\":\"%s\",\"message\":\"%s\",\"channels\":%s}",
                        escapeJson(username),
                        escapeJson(message),
                        channelsToJson()
                );

                String url = workerUrl + "/api/mc/chat";
                Deadrecall.LOGGER.info("[DiscordBridge] 發送請求到: {}", url);

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-API-Key", apiKey);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));

                int responseCode = conn.getResponseCode();

                InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();
                String responseBody = responseStream != null
                    ? new String(responseStream.readAllBytes(), StandardCharsets.UTF_8)
                    : "N/A";

                if (responseCode == 200) {
                    Deadrecall.LOGGER.info("[DiscordBridge] 發送成功 (HTTP 200): {}", responseBody);
                } else {
                    Deadrecall.LOGGER.warn("[DiscordBridge] 發送失敗 (HTTP {}): {}", responseCode, responseBody);
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
                String json = String.format("{\"status\":\"%s\",\"players_online\":\"%d\",\"players_max\":\"%d\",\"version\":\"%s\",\"tps\":\"%.1f\",\"channels\":%s}",
                        escapeJson(status), (Object) playersOnline, (Object) playersMax, escapeJson(version), (Object) tps, channelsToJson()
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

    /**
     * 通知村民升級到 Discord（非同步）
     */
    public static void sendVillagerLevelUp(String villagerName, int oldLevel, int newLevel) {
        if (!enabled) return;

        String name = (villagerName == null || villagerName.isBlank()) ? "村民" : villagerName.trim();
        String message = String.format("%s 升級了：等級 %d → %d", name, oldLevel, newLevel);
        sendChatMessage("系統", message);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String getWorkerUrl() {
        return workerUrl;
    }

    public static String getApiKey() {
        return apiKey;
    }

    private static String channelsToJson() {
        JsonArray arr = new JsonArray();
        for (DiscordChannel ch : channels) {
            arr.add(ch.id);
        }
        return arr.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String normalizeWorkerUrl(String url) {
        if (url == null) {
            return "";
        }
        String normalized = url.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static void createDefaultConfig(Path configFile) {
        String defaultConfig = """
                {
                  "enabled": false,
                  "workerUrl": "",
                  "apiKey": "",
                  "channels": []
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
