package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.ServerListHelper;
import com.mcsdc.addon.gui.LoginScreen;
import com.mcsdc.addon.system.McsdcSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class McsdcHubScreen extends McsdcParentScreen {
    public McsdcHubScreen(Screen parent) {
        super(Component.literal("MCSDC"), parent);
    }

    @Override
    protected void init() {
        if (McsdcSystem.get().getToken().isEmpty()) {
            minecraft.setScreen(new LoginScreen(parent));
            return;
        }

        int cx = width / 2;
        int footerY = UiLayout.footerY(height, width);
        int bw = Math.min(200, width - UiLayout.margin(width) * 2);
        int buttonCount = 7;
        int startY = UiLayout.CONTENT_TOP + 12;
        int available = footerY - startY - 8;
        UiLayout.VerticalMenu menu = UiLayout.verticalMenu(available, buttonCount);
        int y = startY;

        addMenuButton("Find Servers", y, bw, menu.itemHeight(), b -> minecraft.setScreen(new McsdcBrowseScreen(this)));
        y += menu.step();

        addMenuButton("Friends", y, bw, menu.itemHeight(), b -> minecraft.setScreen(new McsdcFriendsScreen(this)));
        y += menu.step();

        addMenuButton("Recent Servers", y, bw, menu.itemHeight(), b -> minecraft.setScreen(new McsdcRecentScreen(this)));
        y += menu.step();

        addMenuButton("Find Player", y, bw, menu.itemHeight(), b -> minecraft.setScreen(new McsdcFindPlayerScreen(this)));
        y += menu.step();

        addMenuButton("Ticket ID", y, bw, menu.itemHeight(), b -> minecraft.setScreen(new McsdcTicketScreen(this)));
        y += menu.step();

        addMenuButton("Clear MCSDC Servers", y, bw, menu.itemHeight(), b -> ServerListHelper.removeMcsdcServers());
        y += menu.step();

        addMenuButton(shareLocationLabel(), y, bw, menu.itemHeight(), b -> {
            McsdcSystem system = McsdcSystem.get();
            system.setShareLocation(!system.isShareLocation());
            b.setMessage(Component.literal(shareLocationLabel()));
        });

        addRenderableWidget(Button.builder(Component.literal("Logout"), b -> {
            McsdcSystem.get().setToken("");
            McsdcSystem.get().setUsername("");
            minecraft.setScreen(new LoginScreen(parent));
        }).bounds(cx - bw / 2 - 52, footerY, 98, UiLayout.BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(cx - bw / 2 + 54, footerY, 98, UiLayout.BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        drawTitle(context);
        context.centeredText(font, "Logged in as: " + McsdcSystem.get().getUsername(), width / 2, UiLayout.CONTENT_TOP, CommonColors.LIGHT_GRAY);
    }

    private static String shareLocationLabel() {
        return "Share Location with Friends: " + (McsdcSystem.get().isShareLocation() ? "On" : "Off");
    }

    private void addMenuButton(String label, int y, int width, int height, Button.OnPress action) {
        addRenderableWidget(Button.builder(Component.literal(label), action)
            .bounds(this.width / 2 - width / 2, y, width, height).build());
    }
}
