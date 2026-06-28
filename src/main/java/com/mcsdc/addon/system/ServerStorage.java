package com.mcsdc.addon.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ServerStorage(String ip, String version, @Nullable Long lastScanned, @Nullable Long lastSeen) {
    private static final long STALE_MS = 40 * 60 * 1000L;

    public boolean isStale() {
        if (lastScanned == null || lastSeen == null) return false;
        return lastScanned - lastSeen > STALE_MS;
    }

    public static List<ServerStorage> fromJsonArray(JsonArray array) {
        List<ServerStorage> list = new ArrayList<>();
        for (JsonElement node : array) {
            ServerStorage server = fromJsonElement(node);
            if (server != null) list.add(server);
        }
        return list;
    }

    @Nullable
    private static ServerStorage fromJsonElement(JsonElement node) {
        if (!node.isJsonObject()) return null;
        JsonObject obj = node.getAsJsonObject();
        if (!obj.has("address") || obj.get("address").isJsonNull()) return null;

        return new ServerStorage(
            obj.get("address").getAsString(),
            readVersion(obj),
            readTime(obj, "last_scanned", "scanned"),
            readTime(obj, "last_seen_online", "last_online")
        );
    }

    private static String readVersion(JsonObject obj) {
        if (!obj.has("version") || obj.get("version").isJsonNull()) return "";
        JsonElement version = obj.get("version");
        if (version.isJsonPrimitive()) return version.getAsString();
        if (version.isJsonObject()) {
            JsonObject versionObject = version.getAsJsonObject();
            if (versionObject.has("name") && !versionObject.get("name").isJsonNull()) {
                return versionObject.get("name").getAsString();
            }
            if (versionObject.has("protocol") && !versionObject.get("protocol").isJsonNull()) {
                return versionObject.get("protocol").getAsString();
            }
        }
        return "";
    }

    @Nullable
    private static Long readTime(JsonObject obj, String primary, String fallback) {
        Long value = readTime(obj, primary);
        return value != null && value > 0 ? value : readTime(obj, fallback);
    }

    @Nullable
    private static Long readTime(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        JsonElement element = obj.get(key);
        if (!element.isJsonPrimitive()) return null;

        try {
            if (element.getAsJsonPrimitive().isNumber()) return element.getAsLong();
            String text = element.getAsString().trim();
            return text.isEmpty() ? null : Long.parseLong(text);
        } catch (Exception ignored) {
            return null;
        }
    }
}
