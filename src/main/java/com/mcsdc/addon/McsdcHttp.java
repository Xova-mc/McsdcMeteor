package com.mcsdc.addon;

import com.google.gson.JsonObject;
import com.mcsdc.addon.system.McsdcSystem;
import meteordevelopment.meteorclient.utils.network.Http;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpResponse;

public final class McsdcHttp {
    private McsdcHttp() {}

    @Nullable
    public static String post(JsonObject body) {
        HttpResponse<String> response = postResponse(body);
        return response != null ? response.body() : null;
    }

    @Nullable
    public static String postAddressQuery(String address) {
        return post(addressSearch(address));
    }

    public static HttpResponse<String> postResponse(JsonObject body) {
        return Http.post(Main.mainEndpoint)
            .bodyJson(body)
            .header("authorization", "Bearer " + McsdcSystem.get().getToken())
            .sendStringResponse();
    }

    @Nullable
    public static String postPublic(JsonObject body) {
        return Http.post(Main.mainEndpoint).bodyJson(body).sendString();
    }

    public static JsonObject addressSearch(String address) {
        JsonObject search = new JsonObject();
        search.addProperty("address", address);
        JsonObject root = new JsonObject();
        root.add("search", search);
        return root;
    }

    public static JsonObject authLogin(String token) {
        JsonObject auth = new JsonObject();
        auth.addProperty("login", token);
        JsonObject root = new JsonObject();
        root.add("auth", auth);
        return root;
    }
}
