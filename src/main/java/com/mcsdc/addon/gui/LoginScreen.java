package com.mcsdc.addon.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcsdc.addon.McsdcHttp;
import com.mcsdc.addon.gui.vanilla.GuiAsync;
import com.mcsdc.addon.gui.vanilla.McsdcHubScreen;
import com.mcsdc.addon.system.McsdcSystem;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import net.minecraft.client.gui.screens.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class LoginScreen extends WindowScreen {

    private final Screen parent;

    private final Settings settings = new Settings();
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<String> tokenSetting = sg.add(new StringSetting.Builder()
        .name("token")
        .description("The token to use for the API.")
        .defaultValue("")
        .build()
    );

    private WLabel status;

    public LoginScreen(Screen parent) {
        super(GuiThemes.get(), "Login with Token");
        this.parent = parent;
    }

    @Override
    public void initWidgets() {
        WContainer settingsContainer = add(theme.verticalList()).expandX().widget();
        settingsContainer.add(theme.settings(settings)).expandX();

        add(theme.button("Submit")).expandX().widget().action = this::submit;
    }

    private void submit() {
        reload();
        status = null; // reload() rebuilt the widgets, old label is gone

        String token = tokenSetting.get().trim();
        if (token.isEmpty()) {
            setStatus("Please enter a token to login.");
            return;
        }

        setStatus("Logging in...");
        GuiAsync.run(mc, () -> {
            String response = McsdcHttp.postPublic(McsdcHttp.authLogin(token));
            if (response == null) return null;

            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            if (jsonObject.has("error")) return null;

            return jsonObject.getAsJsonObject("data").get("name").getAsString();
        }, name -> {
            if (name == null) {
                setStatus("Invalid token.");
                return;
            }

            McsdcSystem.get().setToken(token);
            McsdcSystem.get().setUsername(name);

            if (mc.screen == this) mc.setScreen(new McsdcHubScreen(parent));
        }, err -> setStatus("Login failed: " + err));
    }

    private void setStatus(String text) {
        if (status == null) status = add(theme.label(text)).expandX().widget();
        else status.set(text);
    }
}
