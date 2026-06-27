package com.mcsdc.addon.gui.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcsdc.addon.FriendsApi;
import com.mcsdc.addon.system.McsdcSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class McsdcFriendsScreen extends McsdcParentScreen {
    private boolean locationsTab;
    private final TabData list = new TabData();
    private final TabData locations = new TabData();
    @Nullable private String busy;
    private EditBox nameField;
    private McsdcFriendListWidget listWidget;
    private String status = "";
    private Button actionBtn;

    public McsdcFriendsScreen(Screen parent) {
        super(Component.literal("Friends"), parent);
    }

    @Override
    protected void init() {
        int margin = UiLayout.margin(width);
        int top = UiLayout.CONTENT_TOP + 24;
        int footerY = UiLayout.footerY(height, width);
        UiLayout.ButtonSlot back = UiLayout.backButton(width, height);
        int listH = footerY - top - (locationsTab ? 0 : 28);

        addRenderableWidget(Button.builder(Component.literal("List"), b -> switchTab(false))
            .bounds(margin, 28, 80, UiLayout.BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal("On a server"), b -> switchTab(true))
            .bounds(margin + 84, 28, 100, UiLayout.BUTTON_HEIGHT).build());

        listWidget = new McsdcFriendListWidget(margin, top, width - margin * 2, listH);
        addRenderableWidget(listWidget);

        if (!locationsTab) {
            nameField = new EditBox(font, margin, footerY - 24, width - margin * 2 - 58, UiLayout.BUTTON_HEIGHT, Component.literal("username"));
            nameField.setMaxLength(32);
            addRenderableWidget(nameField);
            addRenderableWidget(Button.builder(Component.literal("Add"), b -> addFriend())
                .bounds(width - margin - 54, footerY - 24, 54, UiLayout.BUTTON_HEIGHT).build());
        }

        actionBtn = addRenderableWidget(Button.builder(Component.literal(locationsTab ? "Join" : "Remove"), b -> runAction())
            .bounds(margin, footerY, 80, UiLayout.BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(back.x(), back.y(), back.width(), UiLayout.BUTTON_HEIGHT).build());

        loadTab();
        updateActionBtn();
    }

    private TabData activeTab() {
        return locationsTab ? locations : list;
    }

    private void switchTab(boolean showLocations) {
        locationsTab = showLocations;
        activeTab().invalidate();
        rebuildUi();
    }

    private void rebuildUi() {
        clearWidgets();
        init();
    }

    private void loadTab() {
        TabData tab = activeTab();
        if (!tab.loaded && !tab.loading) {
            status = "Loading...";
            tab.loading = true;
            String path = locationsTab ? "/my/friends/locations" : "/my/friends";
            CompletableFuture.supplyAsync(() -> FriendsApi.requestGet(path))
                .thenAccept(r -> minecraft.execute(() -> {
                    if (r.ok()) tab.ok(FriendsApi.unwrapArray(r.body()));
                    else tab.fail(r.error());
                    if (tab != activeTab()) return;
                    status = tab.error.isEmpty() ? "" : tab.error;
                    populateList();
                    updateActionBtn();
                }))
                .exceptionally(e -> {
                    minecraft.execute(() -> {
                        tab.fail(e.getMessage());
                        if (tab != activeTab()) return;
                        status = tab.error;
                        populateList();
                    });
                    return null;
                });
        } else if (tab.loaded) {
            populateList();
        }
    }

    private void populateList() {
        TabData tab = activeTab();
        if (!tab.error.isEmpty()) status = tab.error;
        if (tab.items.isEmpty() && tab.loaded) {
            status = locationsTab ? "No friends on a server right now." : "No friends yet.";
            listWidget.setRows(List.of());
            return;
        }
        List<McsdcFriendListWidget.Row> rows = new ArrayList<>();
        for (JsonElement el : tab.items) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            if (locationsTab) {
                rows.add(new McsdcFriendListWidget.Row(
                    FriendsApi.jsonString(obj, "name"),
                    FriendsApi.jsonString(obj, "server"),
                    ""
                ));
            } else {
                String stage = FriendsApi.jsonString(obj, "stage");
                rows.add(new McsdcFriendListWidget.Row(
                    FriendsApi.jsonString(obj, "name"),
                    FriendsApi.jsonString(obj, "role", "user"),
                    stage.isEmpty() ? "—" : stage
                ));
            }
        }
        listWidget.setRows(rows);
    }

    private void addFriend() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) return;
        if (name.equals(McsdcSystem.get().getUsername())) {
            status = "cant add yourself lol";
            return;
        }
        busy = "add";
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        CompletableFuture.supplyAsync(() -> FriendsApi.requestPost("/my/friends", body))
            .thenAccept(r -> minecraft.execute(() -> {
                busy = null;
                if (!r.ok()) status = r.error();
                else {
                    nameField.setValue("");
                    list.invalidate();
                    rebuildUi();
                }
            }))
            .exceptionally(e -> {
                minecraft.execute(() -> {
                    busy = null;
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String msg = cause.getMessage();
                    status = msg != null && !msg.isBlank() ? msg : "request failed";
                    updateActionBtn();
                });
                return null;
            });
    }

    private void runAction() {
        McsdcFriendListWidget.Row row = listWidget.getSelectedRow();
        if (row == null) return;
        if (locationsTab) {
            if (canJoin(row.col2())) join(row.col2());
        } else {
            removeFriend(row.name());
        }
    }

    private void removeFriend(String name) {
        busy = name;
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        CompletableFuture.supplyAsync(() -> FriendsApi.requestPost("/my/friends/deny", body))
            .thenAccept(r -> minecraft.execute(() -> {
                busy = null;
                if (!r.ok()) status = r.error();
                else {
                    list.invalidate();
                    rebuildUi();
                }
            }))
            .exceptionally(e -> {
                minecraft.execute(() -> {
                    busy = null;
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String msg = cause.getMessage();
                    status = msg != null && !msg.isBlank() ? msg : "request failed";
                    updateActionBtn();
                });
                return null;
            });
    }

    private void join(String address) {
        if (!canJoin(address)) return;
        if (minecraft.level != null) minecraft.level.disconnect(Component.literal(""));
        VanillaScreens.connectTo(minecraft, address);
    }

    private void updateActionBtn() {
        if (actionBtn == null) return;
        McsdcFriendListWidget.Row row = listWidget != null ? listWidget.getSelectedRow() : null;
        if (locationsTab) {
            actionBtn.setMessage(Component.literal("Join"));
            actionBtn.active = row != null && canJoin(row.col2());
        } else {
            actionBtn.setMessage(Component.literal("Remove"));
            actionBtn.active = row != null && busy == null;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, UiLayout.HEADER_LABEL_Y, CommonColors.WHITE);
        if (locationsTab) {
            context.text(font, "Name", UiLayout.margin(width) + 4, UiLayout.CONTENT_TOP + 16, CommonColors.GRAY, true);
            context.text(font, "Server", 136, UiLayout.CONTENT_TOP + 16, CommonColors.GRAY, true);
        } else {
            context.text(font, "Name", UiLayout.margin(width) + 4, UiLayout.CONTENT_TOP + 16, CommonColors.GRAY, true);
            context.text(font, "Role", 136, UiLayout.CONTENT_TOP + 16, CommonColors.GRAY, true);
            context.text(font, "Stage", 236, UiLayout.CONTENT_TOP + 16, CommonColors.GRAY, true);
        }
        if (!status.isEmpty()) {
            context.centeredText(font, status, width / 2, UiLayout.footerY(height, width) - 38, CommonColors.YELLOW);
        }
        updateActionBtn();
    }

    private static boolean canJoin(String server) {
        return !server.isBlank() && !server.equalsIgnoreCase("singleplayer");
    }

    private static final class TabData {
        boolean loading;
        boolean loaded;
        String error = "";
        JsonArray items = new JsonArray();

        void invalidate() {
            loading = false;
            loaded = false;
            error = "";
            items = new JsonArray();
        }

        void ok(JsonArray data) {
            loading = false;
            loaded = true;
            error = "";
            items = data;
        }

        void fail(@Nullable String message) {
            loading = false;
            loaded = true;
            error = message != null && !message.isBlank() ? message : "request failed";
            items = new JsonArray();
        }
    }
}
