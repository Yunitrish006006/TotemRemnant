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
    private static final int DELIVERY_FAILURE_ALERT_THRESHOLD = 3;
    private static final int MAX_CHANNELS = 10;
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
    private static final Set<String> announcedRaidKeys = new HashSet<>();
    private static int consecutiveDeliveryFailures = 0;
    private static boolean deliveryFailureAlertReported = false;

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
        String existingApiKey = apiKey == null ? "" : apiKey.trim();
        String effectiveApiKey = normalizedApiKey.isEmpty() ? existingApiKey : normalizedApiKey;
        if (newEnabled && (normalizedWorkerUrl.isEmpty() || effectiveApiKey.isEmpty())) {
            throw new IllegalArgumentException("啟用 Discord Bridge 時，workerUrl 與 apiKey 不能為空");
        }

        JsonObject config = new JsonObject();
        config.addProperty("enabled", newEnabled);
        config.addProperty("workerUrl", normalizedWorkerUrl);
        config.addProperty("apiKey", effectiveApiKey);

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
        String normalizedId = normalizeChannelId(channelId);
        validateChannelId(normalizedId);
        if (channels.size() >= MAX_CHANNELS) {
            throw new IllegalArgumentException("最多只能配置 " + MAX_CHANNELS + " 個 Discord 頻道");
        }
        String normalizedName = normalizeChannelName(channelName, normalizedId);

        // 檢查是否已存在
        for (DiscordChannel ch : channels) {
            if (ch.id.equals(normalizedId)) {
                throw new IllegalArgumentException("頻道 " + normalizedId + " 已存在");
            }
        }

        channels.add(new DiscordChannel(normalizedId, normalizedName));
        saveChannelsToFile();
        Deadrecall.LOGGER.info("[DiscordBridge] 已添加頻道: {} ({})", normalizedName, normalizedId);
    }

    public static synchronized void removeChannel(String channelId) throws IOException {
        if (configFilePath == null) {
            throw new IllegalStateException("DiscordBridge 尚未初始化");
        }
        String normalizedId = normalizeChannelId(channelId);
        validateChannelId(normalizedId);

        boolean removed = channels.removeIf(ch -> ch.id.equals(normalizedId));
        if (!removed) {
            throw new IllegalArgumentException("頻道 " + normalizedId + " 不存在");
        }

        saveChannelsToFile();
        Deadrecall.LOGGER.info("[DiscordBridge] 已移除頻道: {}", normalizedId);
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
                Set<String> seenChannelIds = new HashSet<>();
                for (int i = 0; i < channelArray.size(); i++) {
                    JsonObject chObj = channelArray.get(i).getAsJsonObject();
                    String id = normalizeChannelId(chObj.has("id") ? chObj.get("id").getAsString() : "");
                    if (!isValidChannelId(id) || !seenChannelIds.add(id)) {
                        continue;
                    }
                    String name = normalizeChannelName(chObj.has("name") ? chObj.get("name").getAsString() : id, id);
                    channels.add(new DiscordChannel(id, name));
                    if (channels.size() >= MAX_CHANNELS) {
                        break;
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
        sendMinecraftEvent("chat", username, message);
    }

    public static void sendMinecraftEvent(String event, String username, String message) {
        if (!enabled || username == null || username.isBlank() || message == null || message.isBlank()) return;

        EXECUTOR.submit(() -> {
            try {
                String json = String.format(
                        "{\"event\":\"%s\",\"username\":\"%s\",\"message\":\"%s\",\"channels\":%s}",
                        escapeJson(event),
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

                if (responseCode >= 200 && responseCode < 300) {
                    if (responseHasDiscordFailures(responseBody)) {
                        recordDeliveryFailure(event);
                        Deadrecall.LOGGER.warn("[DiscordBridge] Discord 端部分或全部發送失敗 (HTTP {}): {}", responseCode, responseBody);
                    } else {
                        recordDeliverySuccess();
                        Deadrecall.LOGGER.info("[DiscordBridge] 發送成功 (HTTP {}): {}", responseCode, responseBody);
                    }
                } else {
                    recordDeliveryFailure(event);
                    Deadrecall.LOGGER.warn("[DiscordBridge] 發送失敗 (HTTP {}): {}", responseCode, responseBody);
                }

                conn.disconnect();
            } catch (Exception e) {
                recordDeliveryFailure(event);
                Deadrecall.LOGGER.error("[DiscordBridge] 發送訊息失敗: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 回報伺服器狀態到 Discord（非同步）
     */
    public static void sendServerStatus(String status, int playersOnline, int playersMax, String version, double tps) {
        if (!enabled) return;

        EXECUTOR.submit(() -> postServerStatus(status, playersOnline, playersMax, version, tps));
    }

    /**
     * 回報伺服器狀態到 Discord（同步，供關閉流程使用）
     */
    public static void sendServerStatusImmediately(String status, int playersOnline, int playersMax, String version, double tps) {
        if (!enabled) return;

        postServerStatus(status, playersOnline, playersMax, version, tps);
    }

    private static void postServerStatus(String status, int playersOnline, int playersMax, String version, double tps) {
        try {
            String json = String.format(Locale.ROOT,
                    "{\"status\":\"%s\",\"players_online\":%d,\"players_max\":%d,\"version\":\"%s\",\"tps\":%.1f,\"channels\":%s}",
                    escapeJson(status), playersOnline, playersMax, escapeJson(version), tps, channelsToJson()
            );

            String url = workerUrl + "/api/mc/server/status";
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

            if (responseCode >= 200 && responseCode < 300) {
                if (responseHasDiscordFailures(responseBody)) {
                    recordDeliveryFailure("server_status");
                    Deadrecall.LOGGER.warn("[DiscordBridge] Discord 端狀態回報部分或全部失敗 (HTTP {}): {}", responseCode, responseBody);
                } else {
                    recordDeliverySuccess();
                    Deadrecall.LOGGER.info("[DiscordBridge] 狀態回報成功 (HTTP {}): {}", responseCode, responseBody);
                }
            } else {
                recordDeliveryFailure("server_status");
                Deadrecall.LOGGER.warn("[DiscordBridge] 狀態回報失敗 (HTTP {}): {}", responseCode, responseBody);
            }
            conn.disconnect();
        } catch (Exception e) {
            recordDeliveryFailure("server_status");
            Deadrecall.LOGGER.warn("[DiscordBridge] 回報狀態失敗: {}", e.getMessage());
        }
    }

    /**
     * 通知村民升級到 Discord（非同步）
     */
    public static void sendVillagerLevelUp(String villagerName, int oldLevel, int newLevel) {
        if (!enabled) return;

        String name = (villagerName == null || villagerName.isBlank()) ? "村民" : villagerName.trim();
        String message = String.format("%s 升級了：等級 %d → %d", name, oldLevel, newLevel);
        sendMinecraftEvent("villager_level_up", "系統", message);
    }

    /**
     * 通知玩家死亡訊息到 Discord（非同步）
     */
    public static void sendDeathMessage(String deathMessage) {
        if (!enabled || deathMessage == null || deathMessage.isBlank()) return;

        sendMinecraftEvent("player_death", "死亡訊息", deathMessage.trim());
    }

    /**
     * 通知玩家加入伺服器到 Discord（非同步）
     */
    public static void sendPlayerJoined(String playerName) {
        String name = normalizePlayerName(playerName);
        if (name.isEmpty()) return;

        sendMinecraftEvent("player_join", name, "加入伺服器");
    }

    /**
     * 通知玩家離開伺服器到 Discord（非同步）
     */
    public static void sendPlayerLeft(String playerName) {
        String name = normalizePlayerName(playerName);
        if (name.isEmpty()) return;

        sendMinecraftEvent("player_leave", name, "離開伺服器");
    }

    public static void sendPlayerFirstJoined(String playerName) {
        String name = normalizePlayerName(playerName);
        if (name.isEmpty()) return;

        sendMinecraftEvent("player_first_join", name, "第一次加入伺服器");
    }

    public static void sendAdvancement(String playerName, String advancementTitle, String advancementType) {
        String name = normalizePlayerName(playerName);
        String title = normalizeText(advancementTitle);
        if (name.isEmpty() || title.isEmpty()) return;

        String type = normalizeText(advancementType);
        String message = type.isEmpty() ? name + " 完成了進度 " + title : name + " 完成了 " + type + " 進度 " + title;
        sendMinecraftEvent("advancement", name, message);
    }

    public static void sendAdminAction(String actor, String action, String targetSummary) {
        String normalizedAction = normalizeText(action);
        String target = normalizeText(targetSummary);
        if (normalizedAction.isEmpty() || target.isEmpty()) return;

        String source = normalizeActor(actor);
        sendMinecraftEvent("admin_action", source, source + " 執行管理操作：" + normalizedAction + " " + target);
    }

    public static void sendServerHealthAlert(String message) {
        String text = normalizeText(message);
        if (text.isEmpty()) return;

        sendMinecraftEvent("server_health_alert", "系統", text);
    }

    public static void sendDeathBackpackCreated(String playerName) {
        String name = normalizePlayerName(playerName);
        if (name.isEmpty()) return;

        sendMinecraftEvent("death_backpack_created", name, name + " 的死亡背包已建立");
    }

    public static void sendDeathBackpackRecovered(String playerName) {
        String name = normalizePlayerName(playerName);
        if (name.isEmpty()) return;

        sendMinecraftEvent("death_backpack_recovered", name, name + " 的死亡背包已回收");
    }

    public static void sendSpaceUnitPublicUpdate(String actor, String message) {
        String text = normalizeText(message);
        if (text.isEmpty()) return;

        sendMinecraftEvent("space_unit_public_update", normalizeActor(actor), text);
    }

    public static void sendBossDefeated(String bossName, String killerName) {
        String boss = normalizeText(bossName);
        if (boss.isEmpty()) return;

        String killer = normalizePlayerName(killerName);
        String message = killer.isEmpty() ? boss + " 被擊敗了" : killer + " 擊敗了 " + boss;
        sendMinecraftEvent("boss_defeated", killer.isEmpty() ? "系統" : killer, message);
    }

    public static void sendRaidStarted(String playerName) {
        String name = normalizePlayerName(playerName);
        String message = name.isEmpty() ? "襲擊已開始" : name + " 觸發了襲擊";
        sendMinecraftEvent("raid_started", name.isEmpty() ? "系統" : name, message);
    }

    public static synchronized void sendRaidStarted(String raidKey, String playerName) {
        String key = normalizeText(raidKey);
        if (key.isEmpty() || !announcedRaidKeys.add(key)) {
            return;
        }
        sendRaidStarted(playerName);
    }

    public static void sendRaidEnded(String result) {
        String normalizedResult = normalizeText(result);
        String message = normalizedResult.isEmpty() ? "襲擊已結束" : "襲擊已結束：" + normalizedResult;
        sendMinecraftEvent("raid_ended", "系統", message);
    }

    public static synchronized void sendRaidEnded(String raidKey, String result) {
        String key = normalizeText(raidKey);
        if (!key.isEmpty()) {
            announcedRaidKeys.remove(key);
        }
        sendRaidEnded(result);
    }

    public static void sendDifficultyChanged(String actor, String difficulty) {
        String value = normalizeText(difficulty);
        if (value.isEmpty()) return;

        String source = normalizeActor(actor);
        sendMinecraftEvent("difficulty_changed", source, source + " 將難度改為 " + value);
    }

    public static void sendGameruleChanged(String actor, String rule, String value) {
        String normalizedRule = normalizeText(rule);
        String normalizedValue = normalizeText(value);
        if (normalizedRule.isEmpty() || normalizedValue.isEmpty()) return;

        String source = normalizeActor(actor);
        sendMinecraftEvent("gamerule_changed", source, source + " 將 gamerule " + normalizedRule + " 改為 " + normalizedValue);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String getWorkerUrl() {
        return workerUrl;
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

    private static String normalizePlayerName(String playerName) {
        return playerName == null ? "" : playerName.trim();
    }

    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeChannelId(String channelId) {
        return channelId == null ? "" : channelId.trim();
    }

    private static String normalizeChannelName(String channelName, String fallback) {
        String normalized = normalizeText(channelName);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized.length() > 64 ? normalized.substring(0, 64).trim() : normalized;
    }

    private static void validateChannelId(String channelId) {
        if (channelId.isEmpty()) {
            throw new IllegalArgumentException("頻道 ID 不能為空");
        }
        if (!isValidChannelId(channelId)) {
            throw new IllegalArgumentException("頻道 ID 格式不正確，必須是 Discord snowflake");
        }
    }

    private static boolean isValidChannelId(String channelId) {
        if (channelId == null || channelId.length() < 17 || channelId.length() > 20) {
            return false;
        }
        for (int i = 0; i < channelId.length(); i++) {
            if (!Character.isDigit(channelId.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeActor(String actor) {
        String normalized = normalizeText(actor);
        return normalized.isEmpty() ? "server" : normalized;
    }

    private static void recordDeliverySuccess() {
        consecutiveDeliveryFailures = 0;
        deliveryFailureAlertReported = false;
    }

    private static void recordDeliveryFailure(String event) {
        consecutiveDeliveryFailures++;
        if (deliveryFailureAlertReported || "server_health_alert".equals(event)) {
            return;
        }
        if (consecutiveDeliveryFailures >= DELIVERY_FAILURE_ALERT_THRESHOLD) {
            deliveryFailureAlertReported = true;
            Deadrecall.LOGGER.warn("[DiscordBridge] 連續 {} 次傳送失敗", consecutiveDeliveryFailures);
            sendServerHealthAlert("Discord Bridge 連續 " + consecutiveDeliveryFailures + " 次傳送失敗，請檢查 Worker、Bot Token 或 Webhook 設定");
        }
    }

    private static boolean responseHasDiscordFailures(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonObject()) {
                return false;
            }
            JsonObject data = root.getAsJsonObject("data");
            return data.has("failed") && data.get("failed").getAsInt() > 0;
        } catch (Exception ignored) {
            return false;
        }
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
