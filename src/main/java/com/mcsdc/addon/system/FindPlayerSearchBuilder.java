package com.mcsdc.addon.system;

import com.google.gson.JsonObject;

public class FindPlayerSearchBuilder {
    public static JsonObject create(String player) {
        JsonObject rootJson = new JsonObject();
        JsonObject searchJson = new JsonObject();
        searchJson.addProperty("player", player);
        rootJson.add("search", searchJson);
        return rootJson;
    }
}
