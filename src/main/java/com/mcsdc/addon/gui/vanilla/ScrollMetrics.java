package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.GuiGraphicsExtractor;

final class ScrollMetrics {
    private double scrollY;
    private boolean dragging;
    private double dragOffset;

    void reset() {
        scrollY = 0;
        dragging = false;
    }

    int scrollY() {
        return (int) scrollY;
    }

    void scrollByWheel(double vertical, int rowHeight, int contentHeight, int viewHeight) {
        scrollY = Math.clamp(scrollY - vertical * rowHeight, 0, maxScroll(contentHeight, viewHeight));
    }

    void draw(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int contentHeight) {
        if (!hasScrollbar(contentHeight, h)) return;
        int barX = x + w - 6;
        int thumbH = thumbHeight(h, contentHeight);
        int thumbY = thumbY(y, h, contentHeight);
        ctx.fill(barX, y, barX + 5, y + h, 0x40FFFFFF);
        ctx.fill(barX, thumbY, barX + 5, thumbY + thumbH, 0xFFFFFFFF);
    }

    boolean startDrag(double mouseX, double mouseY, int x, int y, int w, int h, int contentHeight) {
        if (!hasScrollbar(contentHeight, h) || !isOverThumb(mouseX, mouseY, x, y, w, h, contentHeight)) return false;
        dragging = true;
        dragOffset = mouseY - thumbY(y, h, contentHeight);
        setFromThumbY(mouseY - dragOffset, y, h, contentHeight);
        return true;
    }

    boolean drag(double mouseY, int y, int h, int contentHeight) {
        if (!dragging) return false;
        setFromThumbY(mouseY - dragOffset, y, h, contentHeight);
        return true;
    }

    boolean releaseDrag() {
        if (!dragging) return false;
        dragging = false;
        return true;
    }

    private boolean hasScrollbar(int contentHeight, int viewHeight) {
        return contentHeight > viewHeight;
    }

    private int maxScroll(int contentHeight, int viewHeight) {
        return Math.max(0, contentHeight - viewHeight);
    }

    private int thumbHeight(int viewHeight, int contentHeight) {
        return Math.max(16, viewHeight * viewHeight / contentHeight);
    }

    private int thumbY(int y, int viewHeight, int contentHeight) {
        int max = maxScroll(contentHeight, viewHeight);
        if (max <= 0) return y;
        int trackH = viewHeight - thumbHeight(viewHeight, contentHeight);
        return y + (int) ((scrollY / max) * trackH);
    }

    private boolean isOverThumb(double mouseX, double mouseY, int x, int y, int w, int h, int contentHeight) {
        int barX = x + w - 6;
        int thumbY = thumbY(y, h, contentHeight);
        int thumbH = thumbHeight(h, contentHeight);
        return mouseX >= barX && mouseX < barX + 6 && mouseY >= thumbY && mouseY < thumbY + thumbH;
    }

    private void setFromThumbY(double thumbY, int y, int viewHeight, int contentHeight) {
        int max = maxScroll(contentHeight, viewHeight);
        int trackH = viewHeight - thumbHeight(viewHeight, contentHeight);
        if (max <= 0 || trackH <= 0) {
            scrollY = 0;
            return;
        }
        double relative = Math.clamp(thumbY - y, 0, trackH);
        scrollY = (relative / trackH) * max;
    }
}
