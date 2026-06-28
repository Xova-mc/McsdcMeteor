package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonObject;
import com.mcsdc.addon.Main;
import com.mcsdc.addon.McsdcHttp;
import com.mcsdc.addon.system.McsdcSystem;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class McsdcBrowseScreen extends McsdcParentScreen {
    private final BrowseSearchState state = BrowseSearchState.LAST;

    private McsdcServerListWidget serverList;
    private boolean searching;

    private Button joinBtn;
    private Button addBtn;
    private Button infoBtn;
    private Button addAllBtn;
    private Button shuffleBtn;

    private int statusX;
    private int statusY;
    private int summaryY;
    private int listHeaderY;

    public McsdcBrowseScreen(Screen parent) {
        super(Component.literal("Find Servers"), parent);
    }

    @Override
    protected void init() {
        int margin = UiLayout.margin(width);
        int toolbarY = UiLayout.CONTENT_TOP + 4;
        int toolbarBtnW = Math.min(90, (width - margin * 2 - 8) / 2);
        statusX = margin + toolbarBtnW + 8 + toolbarBtnW + 8;
        statusY = toolbarY + (UiLayout.BUTTON_HEIGHT - font.lineHeight) / 2;
        summaryY = toolbarY + UiLayout.BUTTON_HEIGHT + 6;
        listHeaderY = summaryY + 12;
        int listTop = listHeaderY + 12;
        int footerY = UiLayout.footerY(height, width);
        int listH = footerY - listTop - 4;

        addRenderableWidget(Button.builder(Component.literal("Filter"), b ->
            minecraft.setScreen(new McsdcFilterScreen(this, state)))
            .bounds(margin, toolbarY, toolbarBtnW, UiLayout.BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Search"), b -> runSearch())
            .bounds(margin + toolbarBtnW + 8, toolbarY, toolbarBtnW, UiLayout.BUTTON_HEIGHT).build());

        serverList = new McsdcServerListWidget(margin, listTop, width - margin * 2, listH);
        addRenderableWidget(serverList);

        UiLayout.ButtonSlot back = UiLayout.backButton(width, height);
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(back.x(), back.y(), back.width(), UiLayout.BUTTON_HEIGHT).build());

        ServerListActions.FooterButtons footer = ServerListActions.createFooter(
            minecraft, serverList, this::updateActionButtons, this::addSelected);
        joinBtn = addRenderableWidget(footer.join());
        addBtn = addRenderableWidget(footer.add());
        infoBtn = addRenderableWidget(footer.info());
        addAllBtn = addRenderableWidget(Button.builder(Component.literal("Add all"), b -> addAll()).build());
        shuffleBtn = addRenderableWidget(Button.builder(Component.literal("Shuffle"), b -> shuffle()).build());

        UiLayout.placeFooterActions(margin, back.x() - margin, footerY, List.of(joinBtn, addBtn, infoBtn, addAllBtn, shuffleBtn));

        if (!state.results.isEmpty()) serverList.setServers(state.results);
        updateActionButtons();
    }

    private void runSearch() {
        if (searching) return;
        if (!state.advancedMotd && state.allCoreFlagsAny()) {
            state.statusMessage = "Pick at least one filter (not all Any).";
            return;
        }

        searching = true;
        state.statusMessage = "Searching...";
        BrowseSearchState submittedSearch = state.copy();

        GuiAsync.run(minecraft, () -> {
            JsonObject json = state.toSearchJson();
            if (json == null) return null;
            Main.LOG.info(json.toString());
            return McsdcHttp.post(json);
        }, response -> {
            searching = false;
            if (response == null) {
                rememberSearch(submittedSearch, "Enter a version string.");
                return;
            }
            ServerSearchResults.ParseResult parsed = ServerSearchResults.parse(response);
            if (!parsed.ok()) {
                rememberSearch(submittedSearch, parsed.error());
                return;
            }
            List<ServerStorage> results = new ArrayList<>(parsed.serversOrEmpty());
            if (submittedSearch.hideOffline) results.removeIf(s -> s.isStale());
            state.results = results;
            if (results.isEmpty()) {
                state.statusMessage = "No servers found.";
                serverList.setServers(List.of());
            } else {
                state.statusMessage = ServerSearchResults.statusFor(results);
                McsdcSystem.get().setServerQueue(results);
                serverList.setServers(results);
            }
            submittedSearch.results = new ArrayList<>(results);
            rememberSearch(submittedSearch, state.statusMessage);
            updateActionButtons();
        }, err -> {
            Main.LOG.error("Failed to search: {}", err);
            searching = false;
            state.statusMessage = "Error: " + err;
            updateActionButtons();
        });
    }

    private void addSelected() {
        ServerStorage s = serverList.getSelectedServer();
        if (s == null) return;
        ServerListActions.add(serverList);
        state.statusMessage = "Added " + s.ip();
    }

    private void addAll() {
        String msg = ServerListActions.addAllMessage(state.results);
        if (msg != null) state.statusMessage = msg;
    }

    private void shuffle() {
        if (state.results.isEmpty()) return;
        Collections.shuffle(state.results);
        serverList.setServers(state.results);
    }

    private void updateActionButtons() {
        boolean sel = serverList.getSelectedServer() != null;
        ServerListActions.setActive(sel, joinBtn, addBtn, infoBtn);
        boolean hasResults = !state.results.isEmpty();
        addAllBtn.active = hasResults;
        shuffleBtn.active = hasResults;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int margin = UiLayout.margin(width);
        drawTitle(context);
        if (!state.statusMessage.isEmpty()) {
            context.text(font, state.statusMessage, statusX, statusY, CommonColors.YELLOW, true);
        }
        context.text(font, state.summary(), margin, summaryY, CommonColors.LIGHT_GRAY, true);

        int lx = serverList.getX();
        int lw = serverList.getWidth();
        int ly = serverList.getY();
        int lh = serverList.getHeight();
        context.text(font, "Address", lx + 4, listHeaderY, CommonColors.GRAY, true);
        context.text(font, "Version", lx + lw / 2, listHeaderY, CommonColors.GRAY, true);
        if (state.results.isEmpty() && state.statusMessage.isEmpty()) {
            context.centeredText(font, "Set filters and hit Search", lx + lw / 2, ly + lh / 2, CommonColors.DARK_GRAY);
        }
    }

    private void rememberSearch(BrowseSearchState submitted, String status) {
        state.statusMessage = status;
        submitted.statusMessage = status;
        BrowseSearchState.LAST.copyFrom(submitted);
    }
}
