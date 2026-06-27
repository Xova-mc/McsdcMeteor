package com.mcsdc.addon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class AddressQuery {
    private AddressQuery() {}

    public static void load(String ip, Consumer<JsonObject> onOk, Runnable onInvalid) {
        CompletableFuture.supplyAsync(() -> McsdcHttp.postAddressQuery(ip)).thenAccept(response -> {
            if (response == null || response.isEmpty()) {
                mc.execute(onInvalid);
                return;
            }
            mc.execute(() -> {
                JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                if (obj.has("error")) onInvalid.run();
                else onOk.accept(obj);
            });
        });
    }
}
