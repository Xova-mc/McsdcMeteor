package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class McsdcServerListWidget extends AbstractWidget {
    public static final int ROW_HEIGHT = 18;

    private List<ServerStorage> servers = List.of();
    private int selected = -1;
    private final ScrollMetrics scroll = new ScrollMetrics();
    @Nullable private Runnable onSelectionChanged;

    public McsdcServerListWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public void setOnSelectionChanged(@Nullable Runnable onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public void setServers(List<ServerStorage> servers) {
        this.servers = List.copyOf(servers);
        this.selected = -1;
        scroll.reset();
        notifySelectionChanged();
    }

    @Nullable
    public ServerStorage getSelectedServer() {
        if (selected < 0 || selected >= servers.size()) return null;
        return servers.get(selected);
    }

    @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int contentH = contentHeight();

        ctx.fill(x, y, x + w, y + h, 0xC0101010);
        ctx.enableScissor(x, y, x + w, y + h);

        var tr = Minecraft.getInstance().font;
        for (int i = 0; i < servers.size(); i++) {
            int rowY = y + 1 + i * ROW_HEIGHT - scroll.scrollY();
            if (rowY + ROW_HEIGHT < y) continue;
            if (rowY > y + h) break;

            ServerStorage server = servers.get(i);
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean sel = i == selected;

            if (sel) ctx.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x80808080);
            else if (hovered) ctx.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x40404040);

            int color = sel || hovered ? CommonColors.WHITE : CommonColors.LIGHT_GRAY;
            ctx.text(tr, server.ip(), x + 4, rowY + 4, color, true);
            String version = server.version() != null ? server.version() : "?";
            ctx.text(tr, version, x + w / 2, rowY + 4, color, true);
        }

        ctx.disableScissor();
        scroll.draw(ctx, x, y, w, h, contentH);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!active || !visible) return false;
        if (!isMouseOver(event.x(), event.y())) return false;

        int contentH = contentHeight();
        if (scroll.startDrag(event.x(), event.y(), getX(), getY(), getWidth(), getHeight(), contentH)) return true;
        if (selectIndex(indexAtClick(event.y()))) return true;
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return scroll.drag(event.y(), getY(), getHeight(), contentHeight());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return scroll.releaseDrag();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        scroll.scrollByWheel(vertical, ROW_HEIGHT, contentHeight(), getHeight());
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }

    private int contentHeight() {
        return servers.size() * ROW_HEIGHT;
    }

    private int indexAtClick(double clickY) {
        return ((int) clickY - getY() + scroll.scrollY()) / ROW_HEIGHT;
    }

    private boolean selectIndex(int idx) {
        if (idx < 0 || idx >= servers.size()) return false;
        selected = idx;
        notifySelectionChanged();
        return true;
    }

    private void notifySelectionChanged() {
        if (onSelectionChanged != null) onSelectionChanged.run();
    }
}
