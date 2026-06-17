package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.system.FindPlayerSearchBuilder;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class McsdcFindPlayerScreen extends McsdcParentScreen {
    private EditBox playerField;
    private McsdcServerListWidget serverList;
    private List<ServerStorage> results = new ArrayList<>();
    private String status = "";
    private boolean searching = false;
    private Button joinBtn;
    private Button addBtn;
    private Button infoBtn;
    private Button addAllBtn;

    public McsdcFindPlayerScreen(Screen parent) {
        super(Component.literal("Find Player"), parent);
    }

    @Override
    protected void init() {
        int margin = UiLayout.margin(width);
        int footerY = UiLayout.footerY(height, width);
        UiLayout.ButtonSlot back = UiLayout.backButton(width, height);
        int searchW = Math.min(56, Math.max(44, width / 10));

        playerField = new EditBox(font, width / 2 - 100, 28, Math.min(140, width - searchW - margin * 2 - 8), UiLayout.BUTTON_HEIGHT, Component.literal("name/uuid"));
        playerField.setMaxLength(64);
        playerField.setValue("popbob");
        addRenderableWidget(playerField);

        addRenderableWidget(Button.builder(Component.literal("Search"), b -> runSearch())
            .bounds(playerField.getX() + playerField.getWidth() + 4, 28, searchW, UiLayout.BUTTON_HEIGHT).build());

        int top = UiLayout.CONTENT_TOP + 28;
        serverList = new McsdcServerListWidget(margin, top, width - margin * 2, footerY - top);
        serverList.setOnSelectionChanged(this::updateButtons);
        addRenderableWidget(serverList);

        joinBtn = addRenderableWidget(Button.builder(Component.literal("Join"), b -> ServerListActions.join(serverList)).build());
        addBtn = addRenderableWidget(Button.builder(Component.literal("Add"), b -> ServerListActions.add(serverList)).build());
        infoBtn = addRenderableWidget(Button.builder(Component.literal("Info"), b -> ServerListActions.info(minecraft, serverList)).build());
        addAllBtn = addRenderableWidget(Button.builder(Component.literal("Add all"), b -> addAll()).build());

        UiLayout.placeFooterActions(margin, back.x() - margin, footerY, List.of(joinBtn, addBtn, infoBtn, addAllBtn));

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(back.x(), back.y(), back.width(), UiLayout.BUTTON_HEIGHT).build());

        updateButtons();
    }

    private void runSearch() {
        if (searching) return;
        String query = playerField.getValue().trim();
        if (query.isEmpty()) {
            status = "Enter a name or UUID.";
            return;
        }
        searching = true;
        status = "Searching...";
        CompletableFuture.supplyAsync(() -> {
            JsonObject body = FindPlayerSearchBuilder.create(query);
            return Api.postJson("/search/player", body);
        }).thenAccept(response -> minecraft.execute(() -> {
            searching = false;
            ServerSearchResults.ParseResult parsed = ServerSearchResults.parse(response);
            if (!parsed.ok()) {
                status = parsed.error();
                return;
            }
            results = new ArrayList<>(parsed.serversOrEmpty());
            status = ServerSearchResults.statusFor(results);
            serverList.setServers(results);
            updateButtons();
        })).exceptionally(ex -> {
            minecraft.execute(() -> {
                searching = false;
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                status = "Error: " + (cause.getMessage() != null ? cause.getMessage() : "Unknown error");
                updateButtons();
            });
            return null;
        });
    }

    private void addAll() {
        if (results.isEmpty()) return;
        ServerListHelper.addAllMcsdcServers(results.stream().map(ServerStorage::ip).toList());
        status = "Added all servers.";
    }

    private void updateButtons() {
        if (joinBtn == null) return;
        boolean sel = serverList.getSelectedServer() != null;
        ServerListActions.setActive(sel, joinBtn, addBtn, infoBtn);
        if (addAllBtn != null) addAllBtn.active = !results.isEmpty();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, UiLayout.HEADER_LABEL_Y, CommonColors.WHITE);
        if (!status.isEmpty()) {
            context.centeredText(font, status, width / 2, 52, CommonColors.YELLOW);
        }
    }
}
