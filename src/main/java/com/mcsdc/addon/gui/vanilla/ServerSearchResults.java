package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcsdc.addon.Main;
import com.mcsdc.addon.system.ServerStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ServerSearchResults {
    private static final String[] ARRAY_KEYS = { "data", "servers", "results", "rows", "list", "items", "search" };

    private ServerSearchResults() {}

    public record ParseResult(@Nullable List<ServerStorage> servers, @Nullable String error) {
        public boolean ok() {
            return error == null;
        }

        public List<ServerStorage> serversOrEmpty() {
            return servers != null ? servers : List.of();
        }
    }

    public static ParseResult parse(@Nullable String response) {
        if (response == null || response.isBlank()) return new ParseResult(null, "No response.");

        String trimmed = response.trim();
        if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
            return new ParseResult(null, truncate(trimmed, 120));
        }

        try {
            JsonElement parsed = JsonParser.parseString(fixTrailingComma(trimmed));
            JsonArray array = parsed.isJsonArray()
                ? parsed.getAsJsonArray()
                : parsed.isJsonObject() ? unwrapArray(parsed.getAsJsonObject()) : null;

            if (array == null) {
                String message = parsed.isJsonObject() && parsed.getAsJsonObject().has("error")
                    ? parsed.getAsJsonObject().get("error").getAsString()
                    : "Unexpected response format.";
                return new ParseResult(null, message);
            }

            return new ParseResult(ServerStorage.fromJsonArray(array), null);
        } catch (Exception e) {
            Main.LOG.warn("Search parse failed ({} chars): {}", trimmed.length(), truncate(trimmed, 200), e);
            return new ParseResult(null, "Error parsing response.");
        }
    }

    @Nullable
    private static JsonArray unwrapArray(JsonObject root) {
        if (root.has("error")) return null;

        for (String key : ARRAY_KEYS) {
            if (root.has(key) && root.get(key).isJsonArray()) return root.getAsJsonArray(key);
        }
        for (String key : root.keySet()) {
            if (root.get(key).isJsonArray()) return root.getAsJsonArray(key);
        }
        return null;
    }

    private static String fixTrailingComma(String json) {
        return json.endsWith(",]") ? json.substring(0, json.length() - 2) + "]" : json;
    }

    private static String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    public static String statusFor(List<ServerStorage> servers) {
        return servers.isEmpty() ? "No servers found." : servers.size() + " server(s) found.";
    }
}
