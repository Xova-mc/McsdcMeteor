package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.gui.ServerInfoScreen;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ServerListActions {
    private ServerListActions() {}

    public record FooterButtons(Button join, Button add, Button info) {}

    public static FooterButtons createFooter(
        Minecraft mc,
        McsdcServerListWidget list,
        Runnable onSelectionChanged,
        Runnable addAction
    ) {
        list.setOnSelectionChanged(onSelectionChanged);
        return new FooterButtons(
            Button.builder(Component.literal("Join"), b -> join(list)).build(),
            Button.builder(Component.literal("Add"), b -> addAction.run()).build(),
            Button.builder(Component.literal("Info"), b -> info(mc, list)).build()
        );
    }

    public static void join(McsdcServerListWidget list) {
        ServerStorage server = list.getSelectedServer();
        if (server != null) VanillaScreens.connectTo(server.ip());
    }

    public static void add(McsdcServerListWidget list) {
        ServerStorage server = list.getSelectedServer();
        if (server != null) ServerListHelper.addMcsdcServer(server.ip());
    }

    public static void info(Minecraft minecraft, McsdcServerListWidget list) {
        ServerStorage server = list.getSelectedServer();
        if (server != null) minecraft.setScreen(new ServerInfoScreen(server.ip()));
    }

    public static void addAll(List<ServerStorage> servers) {
        if (servers.isEmpty()) return;
        ServerListHelper.addAllMcsdcServers(servers.stream().map(ServerStorage::ip).toList());
    }

    @Nullable
    public static String addAllMessage(List<ServerStorage> servers) {
        if (servers.isEmpty()) return null;
        addAll(servers);
        return "Added all servers.";
    }

    public static void setActive(boolean active, Button... buttons) {
        for (Button button : buttons) {
            if (button != null) button.active = active;
        }
    }
}
