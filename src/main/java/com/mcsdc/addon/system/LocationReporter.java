package com.mcsdc.addon.system;

import com.google.gson.JsonObject;
import com.mcsdc.addon.FriendsApi;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class LocationReporter {
    private static final LocationReporter INSTANCE = new LocationReporter();

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(INSTANCE);
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        McsdcSystem system = McsdcSystem.get();
        if (system.getToken().isEmpty() || !system.isShareLocation()) return;
        String server = playSessionServer();
        if (server == null) return;
        // build on the game thread (mc.player can go null mid-flight), send off-thread
        JsonObject payload = buildPayload(server);
        CompletableFuture.runAsync(() -> FriendsApi.post("/my/location", payload).ignoreExceptions().send());
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (McsdcSystem.get().getToken().isEmpty()) return;
        CompletableFuture.runAsync(() -> FriendsApi.delete("/my/location"));
    }

    @Nullable
    private static String playSessionServer() {
        if (mc.player == null || mc.level == null) return null;
        // never report local worlds
        if (mc.isSingleplayer()) return null;
        ClientPacketListener handler = mc.getConnection();
        if (handler == null) return null;
        ServerData info = handler.getServerData();
        if (info == null || info.ip.isBlank()) return null;
        return info.ip;
    }

    private static JsonObject buildPayload(String server) {
        JsonObject body = new JsonObject();
        body.addProperty("server", server);
        body.addProperty("name", mc.player.getName().getString());
        body.addProperty("uuid", mc.player.getStringUUID());

        int ping = 0;
        ClientPacketListener handler = mc.getConnection();
        if (handler != null) {
            PlayerInfo entry = handler.getPlayerInfo(mc.player.getUUID());
            if (entry != null) ping = entry.getLatency();
        }
        body.addProperty("ping", ping);
        return body;
    }
}
