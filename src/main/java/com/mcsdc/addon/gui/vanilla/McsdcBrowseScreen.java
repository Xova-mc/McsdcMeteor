package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonObject;
import com.mcsdc.addon.McsdcHttp;
import com.mcsdc.addon.Main;
import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.system.MOTD;
import com.mcsdc.addon.system.McsdcSystem;
import com.mcsdc.addon.system.ServerSearchBuilder;
import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

        joinBtn = addRenderableWidget(Button.builder(Component.literal("Join"), b -> ServerListActions.join(serverList)).build());
        addBtn = addRenderableWidget(Button.builder(Component.literal("Add"), b -> addSelected()).build());
        infoBtn = addRenderableWidget(Button.builder(Component.literal("Info"), b -> ServerListActions.info(minecraft, serverList)).build());
        addAllBtn = addRenderableWidget(Button.builder(Component.literal("Add all"), b -> addAll()).build());
        shuffleBtn = addRenderableWidget(Button.builder(Component.literal("Shuffle"), b -> shuffle()).build());

        UiLayout.placeFooterActions(margin, back.x() - margin, footerY, List.of(joinBtn, addBtn, infoBtn, addAllBtn, shuffleBtn));

        serverList.setOnSelectionChanged(this::updateActionButtons);
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

        CompletableFuture.supplyAsync(() -> {
            Object ver = state.resolveVersion();
            if (ver instanceof String s && s.isEmpty()) return null;

            HashMap<MOTD, Boolean> motds = null;
            if (state.advancedMotd) {
                motds = new HashMap<>();
                motds.put(MOTD.DEFAULT, state.defaultMotd.bool);
                motds.put(MOTD.COMMUNITY, state.communityMotd.bool);
                motds.put(MOTD.CREATIVE, state.creativeMotd.bool);
                motds.put(MOTD.BIGOTRY, state.bigotryMotd.bool);
                motds.put(MOTD.FURRY, state.furryMotd.bool);
                motds.put(MOTD.LGBT, state.lgbtMotd.bool);
            }

            ServerSearchBuilder.Extra extra = new ServerSearchBuilder.Extra(state.hasHistory.bool, state.hasNotes.bool, motds);
            ServerSearchBuilder.Flags flags = new ServerSearchBuilder.Flags(
                state.visited.bool, state.griefed.bool, state.modded.bool, state.saved.bool,
                state.whitelist.bool, state.active.bool, state.cracked.bool
            );
            ServerSearchBuilder.Search search = new ServerSearchBuilder.Search(
                new ServerSearchBuilder.Version(ver), flags, extra
            );
            JsonObject json = ServerSearchBuilder.createJson(search);
            Main.LOG.info(json.toString());
            return McsdcHttp.post(json);
        }).thenAccept(response -> minecraft.execute(() -> {
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
        })).exceptionally(ex -> {
            Main.LOG.error("Failed to search", ex);
            minecraft.execute(() -> {
                searching = false;
                Throwable root = ex.getCause() != null ? ex.getCause() : ex;
                String msg = root.getMessage();
                state.statusMessage = "Error: " + (msg != null ? msg : "Unknown error");
                updateActionButtons();
            });
            return null;
        });
    }

    private void addSelected() {
        ServerStorage s = serverList.getSelectedServer();
        if (s == null) return;
        ServerListHelper.addMcsdcServer(s.ip());
        state.statusMessage = "Added " + s.ip();
    }

    private void addAll() {
        if (state.results.isEmpty()) return;
        ServerListHelper.addAllMcsdcServers(state.results.stream().map(s -> s.ip()).toList());
        state.statusMessage = "Added all servers.";
    }

    private void shuffle() {
        if (state.results.isEmpty()) return;
        Collections.shuffle(state.results);
        serverList.setServers(state.results);
    }

    private void updateActionButtons() {
        if (joinBtn == null) return;
        boolean sel = serverList.getSelectedServer() != null;
        ServerListActions.setActive(sel, joinBtn, addBtn, infoBtn);
        boolean hasResults = !state.results.isEmpty();
        if (addAllBtn != null) addAllBtn.active = hasResults;
        if (shuffleBtn != null) shuffleBtn.active = hasResults;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int margin = UiLayout.margin(width);
        context.centeredText(font, title, width / 2, UiLayout.HEADER_LABEL_Y, CommonColors.WHITE);
        if (!state.statusMessage.isEmpty()) {
            context.text(font, state.statusMessage, statusX, statusY, CommonColors.YELLOW, true);
        }
        context.text(font, state.summary(), margin, summaryY, CommonColors.LIGHT_GRAY, true);

        if (serverList != null) {
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
    }

    private void rememberSearch(BrowseSearchState submitted, String status) {
        state.statusMessage = status;
        submitted.statusMessage = status;
        BrowseSearchState.LAST.copyFrom(submitted);
    }
}
