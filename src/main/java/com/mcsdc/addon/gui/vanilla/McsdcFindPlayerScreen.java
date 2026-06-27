package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonObject;
import com.mcsdc.addon.McsdcHttp;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.List;

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
        addRenderableWidget(serverList);

        ServerListActions.FooterButtons footer = ServerListActions.createFooter(
            minecraft, serverList, this::updateButtons, () -> ServerListActions.add(serverList));
        joinBtn = addRenderableWidget(footer.join());
        addBtn = addRenderableWidget(footer.add());
        infoBtn = addRenderableWidget(footer.info());
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
        GuiAsync.run(minecraft, () -> {
            JsonObject search = new JsonObject();
            search.addProperty("player", query);
            JsonObject body = new JsonObject();
            body.add("search", search);
            return McsdcHttp.post(body);
        }, response -> {
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
        }, err -> {
            searching = false;
            status = "Error: " + err;
            updateButtons();
        });
    }

    private void addAll() {
        String msg = ServerListActions.addAllMessage(results);
        if (msg != null) status = msg;
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
