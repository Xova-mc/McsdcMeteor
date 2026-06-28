package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.function.Consumer;

public class McsdcVersionSelectScreen extends McsdcParentScreen {
    private final SearchVersion selected;
    private final Consumer<SearchVersion> onSelect;

    public McsdcVersionSelectScreen(Screen parent, SearchVersion selected, Consumer<SearchVersion> onSelect) {
        super(Component.literal("Select Version"), parent);
        this.selected = selected;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        int margin = UiLayout.margin(width);
        int listW = Math.min(240, width - margin * 2);
        int listX = width / 2 - listW / 2;
        int top = UiLayout.CONTENT_TOP;
        int footerY = UiLayout.footerY(height, width);

        addRenderableWidget(new VersionListWidget(listX, top, listW, footerY - top - 8, selected, version -> {
            onSelect.accept(version);
            onClose();
        }));

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(width / 2 - 50, footerY, 100, UiLayout.BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        drawTitle(context);
    }

    private static final class VersionListWidget extends AbstractWidget {
        private static final int ROW_HEIGHT = 20;

        private final SearchVersion selected;
        private final Consumer<SearchVersion> onSelect;
        private final ScrollMetrics scroll = new ScrollMetrics();

        private VersionListWidget(int x, int y, int width, int height, SearchVersion selected, Consumer<SearchVersion> onSelect) {
            super(x, y, width, height, Component.empty());
            this.selected = selected;
            this.onSelect = onSelect;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int contentH = contentHeight();

            context.fill(x, y, x + w, y + h, 0xC0101010);
            context.enableScissor(x, y, x + w, y + h);

            var font = Minecraft.getInstance().font;
            SearchVersion[] versions = SearchVersion.values();
            for (int i = 0; i < versions.length; i++) {
                int rowY = y + 1 + i * ROW_HEIGHT - scroll.scrollY();
                if (rowY + ROW_HEIGHT < y) continue;
                if (rowY > y + h) break;

                SearchVersion version = versions[i];
                boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                boolean current = version == selected;

                if (current) context.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x80606060);
                else if (hovered) context.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x40404040);

                int color = current ? CommonColors.YELLOW : hovered ? CommonColors.WHITE : CommonColors.LIGHT_GRAY;
                context.text(font, version.version, x + 8, rowY + 6, color, true);
            }

            context.disableScissor();
            scroll.draw(context, x, y, w, h, contentH);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!active || !visible) return false;
            if (!isMouseOver(event.x(), event.y())) return false;

            int contentH = contentHeight();
            if (scroll.startDrag(event.x(), event.y(), getX(), getY(), getWidth(), getHeight(), contentH)) return true;

            int index = ((int) event.y() - getY() + scroll.scrollY()) / ROW_HEIGHT;
            SearchVersion[] versions = SearchVersion.values();
            if (index >= 0 && index < versions.length) {
                onSelect.accept(versions[index]);
                return true;
            }
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
            return SearchVersion.values().length * ROW_HEIGHT;
        }
    }
}
