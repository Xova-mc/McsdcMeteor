package com.mcsdc.addon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcsdc.addon.gui.vanilla.GuiAsync;

import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class AddressQuery {
    private AddressQuery() {}

    public static void load(String ip, Consumer<JsonObject> onOk, Runnable onInvalid) {
        GuiAsync.run(mc, () -> {
            String response = McsdcHttp.postAddressQuery(ip);
            if (response == null || response.isEmpty()) return null;
            JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
            return obj.has("error") ? null : obj;
        }, obj -> {
            if (obj == null) onInvalid.run();
            else onOk.accept(obj);
        }, err -> onInvalid.run());
    }
}
