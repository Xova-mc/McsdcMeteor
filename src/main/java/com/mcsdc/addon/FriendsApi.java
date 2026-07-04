package com.mcsdc.addon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcsdc.addon.system.McsdcSystem;
import meteordevelopment.meteorclient.utils.network.Http;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

// so, friends/location stay on api2, but separate from v1 sqlite search
public final class FriendsApi {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .build();

    private FriendsApi() {}

    public record JsonResult(@Nullable String body, @Nullable String error) {
        public boolean ok() { return error == null; }
    }

    private static String url(String path) {
        return Main.friendsApiBase + (path.startsWith("/") ? path : "/" + path);
    }

    @Nullable
    private static String bearerHeader() {
        String token = McsdcSystem.get().getToken();
        return token.isEmpty() ? null : "Bearer " + token;
    }

    private static void applyAuth(HttpRequest.Builder req) {
        String auth = bearerHeader();
        if (auth != null) req.header("Authorization", auth);
    }

    private static Http.Request withAuth(Http.Request req) {
        String auth = bearerHeader();
        if (auth != null) req.header("Authorization", auth);
        return req;
    }

    public static Http.Request get(String path) {
        return withAuth(Http.get(url(path)));
    }

    public static Http.Request post(String path, @Nullable JsonObject body) {
        Http.Request req = Http.post(url(path));
        if (body != null) req.bodyJson(body);
        return withAuth(req);
    }

    public static JsonResult requestGet(String path) {
        return parseResponse(get(path).sendStringResponse());
    }

    public static JsonResult requestPost(String path, @Nullable JsonObject body) {
        return parseResponse(post(path, body).sendStringResponse());
    }

    public static void delete(String path) {
        HttpRequest.Builder req = HttpRequest.newBuilder().uri(URI.create(url(path))).DELETE();
        applyAuth(req);
        try {
            HTTP.send(req.build(), HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {}
    }

    private static JsonResult parseResponse(@Nullable HttpResponse<String> response) {
        if (response == null) return new JsonResult(null, "No response.");

        String body = response.body();
        String err = errorFrom(body);
        if (err != null) return new JsonResult(body, err);
        if (response.statusCode() != Http.SUCCESS) {
            return new JsonResult(body, "request failed (" + response.statusCode() + ")");
        }
        return new JsonResult(body, null);
    }

    @Nullable
    public static String errorFrom(@Nullable JsonObject obj) {
        if (obj != null && obj.has("error") && obj.get("error").isJsonPrimitive()) {
            return obj.get("error").getAsString();
        }
        return null;
    }

    @Nullable
    public static String errorFrom(@Nullable String body) {
        if (body == null || body.isBlank()) return "No response.";
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed.isJsonObject()) return errorFrom(parsed.getAsJsonObject());
        } catch (Exception ignored) {
            // not JSON at all; treating it as success would crash callers that parse the body
            return "Invalid response.";
        }
        return null;
    }

    public static String jsonString(JsonObject o, String key) {
        return jsonString(o, key, "");
    }

    public static String jsonString(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }
}
