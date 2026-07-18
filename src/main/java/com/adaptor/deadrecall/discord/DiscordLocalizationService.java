package com.adaptor.deadrecall.discord;

import com.adaptor.deadrecall.Deadrecall;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordLocalizationService {
    private static final List<String> BUNDLED_TABLES = List.of(
            "/assets/deadrecall/lang/discord_zh_tw/adventure.json",
            "/assets/deadrecall/lang/discord_zh_tw/end.json",
            "/assets/deadrecall/lang/discord_zh_tw/events.json",
            "/assets/deadrecall/lang/discord_zh_tw/husbandry.json",
            "/assets/deadrecall/lang/discord_zh_tw/nether.json",
            "/assets/deadrecall/lang/discord_zh_tw/story.json",
            "/assets/deadrecall/lang/discord_zh_tw/system.json"
    );
    private static final String SERVER_DATA_DIRECTORY = "deadrecall/discord_zh_tw";
    private static final Identifier RELOAD_LISTENER_ID =
            Identifier.fromNamespaceAndPath("deadrecall", "discord_zh_tw");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?s|%%");
    private static final Map<String, String> BUNDLED_TRANSLATIONS = loadBundledTranslations();
    private static volatile Map<String, String> translations = BUNDLED_TRANSLATIONS;
    private static final int MAX_MISSING_KEY_WARNINGS = 128;
    private static final Set<String> WARNED_MISSING_KEYS = new LinkedHashSet<>();

    private DiscordLocalizationService() {
    }

    public static void registerReloadListener() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                RELOAD_LISTENER_ID,
                (ResourceManagerReloadListener) DiscordLocalizationService::reloadFromServerData
        );
    }

    public static String render(Component component) {
        if (component == null) {
            return "";
        }
        try {
            StringBuilder result = new StringBuilder();
            appendComponent(result, component, translations);
            return normalize(result.toString());
        } catch (RuntimeException exception) {
            Deadrecall.LOGGER.warn("[DiscordBridge] 無法解析 Discord zh_tw Component", exception);
            return "未知訊息";
        }
    }

    public static String translate(String key) {
        String translated = translations.get(key);
        if (translated != null) {
            return translated;
        }
        warnMissingKey(key);
        return safeFallback(key);
    }

    public static int translationCount() {
        return translations.size();
    }

    private static void appendComponent(
            StringBuilder output,
            Component component,
            Map<String, String> snapshot
    ) {
        ComponentContents contents = component.getContents();
        if (contents instanceof PlainTextContents plainText) {
            output.append(plainText.text());
        } else if (contents instanceof TranslatableContents translatable) {
            output.append(renderTranslatable(translatable, snapshot));
        } else {
            String fallback = component.getString();
            if (!fallback.isBlank()) {
                output.append(fallback);
            }
        }

        for (Component sibling : component.getSiblings()) {
            appendComponent(output, sibling, snapshot);
        }
    }

    private static String renderTranslatable(
            TranslatableContents contents,
            Map<String, String> snapshot
    ) {
        String template = snapshot.get(contents.getKey());
        if (template == null) {
            warnMissingKey(contents.getKey());
            return safeFallback(contents.getKey());
        }

        Object[] rawArguments = contents.getArgs();
        String[] arguments = new String[rawArguments.length];
        for (int index = 0; index < rawArguments.length; index++) {
            arguments[index] = renderArgument(rawArguments[index], snapshot);
        }
        return applyPlaceholders(template, arguments);
    }

    private static String renderArgument(Object argument, Map<String, String> snapshot) {
        if (argument instanceof Component component) {
            StringBuilder result = new StringBuilder();
            appendComponent(result, component, snapshot);
            return normalize(result.toString());
        }
        return argument == null ? "" : String.valueOf(argument);
    }

    private static String applyPlaceholders(String template, String[] arguments) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer result = new StringBuffer();
        int sequentialIndex = 0;
        while (matcher.find()) {
            if ("%%".equals(matcher.group())) {
                matcher.appendReplacement(result, "%");
                continue;
            }
            int argumentIndex;
            String explicitIndex = matcher.group(1);
            if (explicitIndex == null) {
                argumentIndex = sequentialIndex++;
            } else {
                argumentIndex = Integer.parseInt(explicitIndex) - 1;
            }
            String replacement = argumentIndex >= 0 && argumentIndex < arguments.length
                    ? arguments[argumentIndex]
                    : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String safeFallback(String key) {
        if (key == null || key.isBlank()) {
            return "未知訊息";
        }
        if (key.startsWith("advancements.") && key.endsWith(".title")) {
            return "未知進度";
        }
        if (key.startsWith("entity.")) {
            return "未知實體";
        }
        if (key.startsWith("death.")) {
            return "死亡訊息";
        }
        return "未知訊息";
    }

    private static synchronized void warnMissingKey(String key) {
        if (key == null
                || key.isBlank()
                || WARNED_MISSING_KEYS.contains(key)
                || WARNED_MISSING_KEYS.size() >= MAX_MISSING_KEY_WARNINGS) {
            return;
        }
        WARNED_MISSING_KEYS.add(key);
        Deadrecall.LOGGER.warn("[DiscordBridge] zh_tw 翻譯缺少 key {}，使用安全 fallback", key);
    }

    private static void reloadFromServerData(ResourceManager resourceManager) {
        Map<String, String> candidate = new LinkedHashMap<>(BUNDLED_TRANSLATIONS);
        int overrideCount = 0;
        try {
            Map<Identifier, Resource> resources = resourceManager.listResources(
                    SERVER_DATA_DIRECTORY,
                    id -> "deadrecall".equals(id.getNamespace()) && id.getPath().endsWith(".json")
            );
            List<Map.Entry<Identifier, Resource>> orderedResources = new ArrayList<>(resources.entrySet());
            orderedResources.sort(Comparator.comparing(entry -> entry.getKey().toString()));

            for (Map.Entry<Identifier, Resource> entry : orderedResources) {
                try (Reader reader = entry.getValue().openAsReader()) {
                    overrideCount += mergeTranslationTable(reader, candidate);
                } catch (Exception exception) {
                    Deadrecall.LOGGER.warn(
                            "[DiscordBridge] 無法載入 zh_tw data resource {}",
                            entry.getKey(),
                            exception
                    );
                }
            }
        } catch (RuntimeException exception) {
            Deadrecall.LOGGER.warn("[DiscordBridge] 無法列舉 zh_tw data resources，保留目前 snapshot", exception);
            return;
        }

        publishSnapshot(candidate);
        Deadrecall.LOGGER.info(
                "[DiscordBridge] 已原子載入 {} 個 zh_tw 翻譯（{} 個 data resource key 覆寫）",
                candidate.size(),
                overrideCount
        );
    }

    private static Map<String, String> loadBundledTranslations() {
        Map<String, String> translations = new LinkedHashMap<>();
        for (String path : BUNDLED_TABLES) {
            try (InputStream stream = DiscordLocalizationService.class.getResourceAsStream(path)) {
                if (stream == null) {
                    Deadrecall.LOGGER.warn("[DiscordBridge] 缺少 zh_tw 翻譯資源 {}", path);
                    continue;
                }
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    mergeTranslationTable(reader, translations);
                }
            } catch (Exception exception) {
                Deadrecall.LOGGER.warn("[DiscordBridge] 無法載入 zh_tw 翻譯資源 {}", path, exception);
            }
        }
        return Map.copyOf(translations);
    }

    private static int mergeTranslationTable(Reader reader, Map<String, String> output) {
        JsonElement parsed = JsonParser.parseReader(reader);
        if (!parsed.isJsonObject()) {
            throw new JsonParseException("translation table must be a JSON object");
        }

        int loaded = 0;
        JsonObject table = parsed.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : table.entrySet()) {
            JsonElement value = entry.getValue();
            if (entry.getKey().isBlank()
                    || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isString()) {
                continue;
            }
            output.put(entry.getKey(), value.getAsString());
            loaded++;
        }
        return loaded;
    }

    private static void publishSnapshot(Map<String, String> candidate) {
        translations = Map.copyOf(candidate);
        clearMissingKeyWarnings();
    }

    private static synchronized void clearMissingKeyWarnings() {
        WARNED_MISSING_KEYS.clear();
    }

    static Map<String, String> snapshotForTesting() {
        return translations;
    }

    static void replaceSnapshotForTesting(Map<String, String> snapshot) {
        publishSnapshot(snapshot);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
