package com.mcsdc.addon.gui;

import com.google.gson.JsonObject;
import com.mcsdc.addon.AddressQuery;
import com.mcsdc.addon.McsdcHttp;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.settings.*;

import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EditFlagsScreen extends WindowScreen {

    private final String ip;

    private final Settings settings = new Settings();
    private final SettingGroup sg = settings.createGroup("Flags");

    private final Setting<String> notesSetting = sg.add(new StringSetting.Builder()
        .name("notes")
        .description("")
        .defaultValue("")
        .build()
    );

    private final Setting<Boolean> griefedSetting = sg.add(new BoolSetting.Builder()
        .name("griefed")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> savedSetting = sg.add(new BoolSetting.Builder()
        .name("saved")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> visitedSetting = sg.add(new BoolSetting.Builder()
        .name("visited")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> moddedSetting = sg.add(new BoolSetting.Builder()
        .name("modded")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> whitelistSetting = sg.add(new BoolSetting.Builder()
        .name("whitelist")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> bannedSetting = sg.add(new BoolSetting.Builder()
        .name("banned")
        .description("")
        .defaultValue(false)
        .build()
    );

    public EditFlagsScreen(String ip) {
        super(GuiThemes.get(), "Edit Flags");
        this.ip = ip;
    }

    @Override
    public void initWidgets() {
        AddressQuery.load(this.ip, jsonObject -> {
            WTable table = add(theme.table()).widget();
            table.minWidth = 300;

            if (jsonObject.has("notes")) {
                notesSetting.set(jsonObject.get("notes").getAsString());
            }

            JsonObject status = jsonObject.getAsJsonObject("status");
            griefedSetting.set(status.get("griefed").getAsBoolean());
            savedSetting.set(status.get("save_for_later").getAsBoolean());
            visitedSetting.set(status.get("visited").getAsBoolean());
            moddedSetting.set(status.get("modded").getAsBoolean());
            whitelistSetting.set(status.get("whitelist").getAsBoolean());
            bannedSetting.set(status.get("banned").getAsBoolean());
            table.add(theme.settings(settings)).expandX();
            table.row();
            table.add(theme.button("Save")).expandX().widget().action = this::setMarked;
            table.row();
        }, () -> add(theme.label("Not Valid")));
    }

    public void setMarked(){
        JsonObject mainJson = new JsonObject();
        JsonObject innerJson = new JsonObject();
        JsonObject flagJson = new JsonObject();

        flagJson.addProperty("visited", visitedSetting.get());
        flagJson.addProperty("griefed", griefedSetting.get());
        flagJson.addProperty("modded", moddedSetting.get());
        flagJson.addProperty("save_for_later", savedSetting.get());
        flagJson.addProperty("whitelist", whitelistSetting.get());
        flagJson.addProperty("banned", bannedSetting.get());

        innerJson.addProperty("address", this.ip);
        innerJson.addProperty("notes", notesSetting.get());
        innerJson.add("flags", flagJson);
        innerJson.addProperty("joined", true);
        mainJson.add("update", innerJson);

        CompletableFuture.runAsync(() -> McsdcHttp.post(mainJson));
    }
}
