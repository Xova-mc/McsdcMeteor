package com.mcsdc.addon.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcsdc.addon.McsdcHttp;
import com.mcsdc.addon.system.McsdcSystem;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class LoginScreen extends WindowScreen {

    WindowScreen parent;

    private final Settings settings = new Settings();
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<String> tokenSetting = sg.add(new StringSetting.Builder()
        .name("token")
        .description("The token to use for the API.")
        .defaultValue("")
        .build()
    );

    public LoginScreen(WindowScreen parent) {
        super(GuiThemes.get(), "Login with Token");
        this.parent = parent;
    }
    WContainer settingsContainer;

    @Override
    public void initWidgets() {
        WContainer settingsContainer = add(theme.verticalList()).expandX().widget();
        settingsContainer.add(theme.settings(settings)).expandX();

        this.settingsContainer = settingsContainer;

        add(theme.button("Submit")).expandX().widget().action = () -> {
            reload();

            if (tokenSetting.get().isEmpty()){
                add(theme.label("Please enter a token to login."));
                return;
            }

            CompletableFuture.supplyAsync(() -> {
                String response = McsdcHttp.postPublic(McsdcHttp.authLogin(tokenSetting.get()));
                if (response == null) return null;

                JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                if (jsonObject.has("error")) return null;

                JsonObject data = jsonObject.getAsJsonObject("data");
                return Map.entry(data.get("name").getAsString(), data.get("perms").getAsInt());
            }).thenAccept(response -> {
                mc.execute(() -> {
                    if (response == null) {
                        add(theme.label("Invalid token."));
                        return;
                    }

                    McsdcSystem.get().setToken(tokenSetting.get());
                    McsdcSystem.get().setUsername(response.getKey());
                    McsdcSystem.get().setLevel(response.getValue());

                    mc.setScreen(this.parent);
                    this.parent.reload();
                });
            });
        };
    }
}
