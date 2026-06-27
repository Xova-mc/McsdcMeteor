package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.system.McsdcSystem;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public final class VanillaScreens {
    private VanillaScreens() {}

    public static void connectTo(String address) {
        connectTo(Minecraft.getInstance(), address);
    }

    public static void connectTo(Minecraft client, String address) {
        connect(client, ServerAddress.parseString(address), ServerListHelper.serverData(address));
    }

    public static void connectFromWorld(Minecraft client, String address) {
        client.disconnectFromWorld(Component.literal(""));
        connectTo(client, address);
    }

    public static void connectFromWorld(Minecraft client, ServerData info) {
        client.disconnectFromWorld(Component.literal(""));
        connect(client, ServerAddress.parseString(info.ip), info);
    }

    public static void connectNext(McsdcSystem system, Minecraft client, boolean disconnectFirst) {
        ServerStorage next = system.getNextServer();
        if (next != null) {
            if (disconnectFirst) client.disconnectFromWorld(Component.literal(""));
            connectTo(client, next.ip());
        } else {
            client.gui.setOverlayMessage(Component.literal("No more servers left."), false);
            client.setScreen(new TitleScreen());
        }
    }

    private static void connect(Minecraft client, ServerAddress address, ServerData info) {
        ConnectScreen.startConnecting(
            new JoinMultiplayerScreen(new TitleScreen()),
            client,
            address,
            info,
            false,
            null
        );
    }
}
