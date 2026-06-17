package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.system.McsdcSystem;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.List;

public class McsdcRecentScreen extends McsdcParentScreen {
    private McsdcServerListWidget serverList;
    private List<ServerStorage> servers = new ArrayList<>();
    private Button joinBtn;
    private Button addBtn;
    private Button infoBtn;
    private Button removeBtn;

    public McsdcRecentScreen(Screen parent) {
        super(Component.literal("Recent Servers"), parent);
    }

    @Override
    protected void init() {
        servers = new ArrayList<>(McsdcSystem.get().getRecentServers().reversed());

        int margin = UiLayout.margin(width);
        int top = UiLayout.CONTENT_TOP;
        int footerY = UiLayout.footerY(height, width);
        UiLayout.ButtonSlot back = UiLayout.backButton(width, height);

        serverList = new McsdcServerListWidget(margin, top, width - margin * 2, footerY - top);
        addRenderableWidget(serverList);

        addRenderableWidget(Button.builder(Component.literal("Clear all"), b -> {
            McsdcSystem.get().clearRecentServers();
            servers.clear();
            serverList.setServers(List.of());
        }).bounds(margin, footerY, 72, UiLayout.BUTTON_HEIGHT).build());

        joinBtn = addRenderableWidget(Button.builder(Component.literal("Join"), b -> ServerListActions.join(serverList)).build());
        addBtn = addRenderableWidget(Button.builder(Component.literal("Add"), b -> ServerListActions.add(serverList)).build());
        infoBtn = addRenderableWidget(Button.builder(Component.literal("Info"), b -> ServerListActions.info(minecraft, serverList)).build());
        removeBtn = addRenderableWidget(Button.builder(Component.literal("Remove"), b -> removeSelected()).build());

        UiLayout.placeFooterActions(margin + 76, back.x() - margin, footerY, List.of(joinBtn, addBtn, infoBtn, removeBtn));

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(back.x(), back.y(), back.width(), UiLayout.BUTTON_HEIGHT).build());

        serverList.setOnSelectionChanged(this::updateButtons);
        serverList.setServers(servers);
        updateButtons();
    }

    private void removeSelected() {
        ServerStorage s = serverList.getSelectedServer();
        if (s == null) return;
        McsdcSystem.get().removeRecentServer(s);
        servers.remove(s);
        serverList.setServers(servers);
    }

    private void updateButtons() {
        if (joinBtn == null) return;
        ServerListActions.setActive(serverList.getSelectedServer() != null, joinBtn, addBtn, infoBtn, removeBtn);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, UiLayout.HEADER_LABEL_Y, CommonColors.WHITE);
        if (servers.isEmpty()) {
            context.centeredText(font, "Recently joined servers will appear here.", width / 2, height / 2, CommonColors.GRAY);
        }
    }
}
