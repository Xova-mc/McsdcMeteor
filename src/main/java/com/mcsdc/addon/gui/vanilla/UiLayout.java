package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.List;

public final class UiLayout {
    public static final int BUTTON_HEIGHT = 20;
    public static final int MIN_BUTTON_WIDTH = 44;
    public static final int PREFERRED_BUTTON_WIDTH = 72;
    public static final int CONTENT_TOP = 36;
    public static final int HEADER_LABEL_Y = 20;

    private UiLayout() {}

    public static int margin(int screenWidth) {
        return Math.max(8, Math.min(16, screenWidth / 60));
    }

    public static int footerY(int screenHeight, int screenWidth) {
        return screenHeight - BUTTON_HEIGHT - margin(screenWidth);
    }

    public static int backButtonWidth(int screenWidth) {
        return Math.min(56, Math.max(44, screenWidth / 8));
    }

    public static ButtonSlot backButton(int screenWidth, int screenHeight) {
        int m = margin(screenWidth);
        int w = backButtonWidth(screenWidth);
        int y = footerY(screenHeight, screenWidth);
        int x = Math.max(m, screenWidth - m - w);
        w = Math.min(w, screenWidth - m - x);
        return new ButtonSlot(x, y, w);
    }

    public static ButtonSlot footerButton(int screenWidth, int screenHeight, int preferredWidth) {
        int m = margin(screenWidth);
        int w = Math.min(preferredWidth, screenWidth - m * 2);
        int y = footerY(screenHeight, screenWidth);
        return new ButtonSlot(screenWidth / 2 - w / 2, y, w);
    }

    public record ButtonSlot(int x, int y, int width) {}

    public record VerticalMenu(int itemHeight, int step) {}

    /**
     * Lays out a vertical stack of buttons within the available height.
     * Returns item height and the Y step (top of one button to top of the next).
     */
    public static VerticalMenu verticalMenu(int availableHeight, int count) {
        if (count <= 0) return new VerticalMenu(BUTTON_HEIGHT, BUTTON_HEIGHT + 4);

        int spacing = 4;
        int height = BUTTON_HEIGHT;
        int minSpacing = 2;
        int needed = count * height + (count - 1) * minSpacing;

        if (needed > availableHeight) {
            height = Math.max(18, (availableHeight - (count - 1) * minSpacing) / count);
            spacing = count > 1
                ? Math.max(minSpacing, (availableHeight - count * height) / (count - 1))
                : 0;
        } else {
            spacing = count > 1
                ? Math.min(spacing, minSpacing + (availableHeight - needed) / (count - 1))
                : 0;
        }

        return new VerticalMenu(height, height + spacing);
    }

    public static List<ButtonSlot> layoutHorizontalButtons(
        int startX,
        int endX,
        int y,
        int count,
        int preferredWidth
    ) {
        if (count <= 0) return List.of();

        int gap = 4;
        int available = endX - startX;
        int width = Math.min(preferredWidth, (available - gap * (count - 1)) / count);
        width = Math.max(MIN_BUTTON_WIDTH, width);

        List<ButtonSlot> slots = new ArrayList<>(count);
        int x = startX;
        for (int i = 0; i < count; i++) {
            slots.add(new ButtonSlot(x, y, width));
            x += width + gap;
        }
        return slots;
    }

    public static void applySlots(List<AbstractWidget> widgets, List<ButtonSlot> slots, int y) {
        int count = Math.min(widgets.size(), slots.size());
        for (int i = 0; i < count; i++) {
            ButtonSlot slot = slots.get(i);
            AbstractWidget widget = widgets.get(i);
            widget.setX(slot.x());
            widget.setY(y);
            widget.setWidth(slot.width());
            widget.setHeight(BUTTON_HEIGHT);
        }
    }

    public static void placeFooterActions(int startX, int endX, int y, List<AbstractWidget> widgets) {
        applySlots(widgets, layoutHorizontalButtons(startX, endX, y, widgets.size(), PREFERRED_BUTTON_WIDTH), y);
    }
}
