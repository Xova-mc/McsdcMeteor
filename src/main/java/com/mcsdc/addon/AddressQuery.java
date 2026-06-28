package com.mcsdc.addon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcsdc.addon.gui.vanilla.GuiAsync;

import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class AddressQuery {
    private AddressQuery() {}

    public static void load(String ip, Consumer<JsonObject> onOk, Runnable onInvalid) {
        GuiAsync.run(mc, () -> McsdcHttp.postAddressQuery(ip), response -> {
            if (response == null || response.isEmpty()) {
                onInvalid.run();
                return;
            }
            JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
            if (obj.has("error")) onInvalid.run();
            else onOk.accept(obj);
        }, err -> onInvalid.run());
    }
}
