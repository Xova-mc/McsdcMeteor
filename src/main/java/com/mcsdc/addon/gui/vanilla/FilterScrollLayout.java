package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.List;

public final class FilterScrollLayout {
    private static final int SCROLL_STEP = 22;

    private final List<Entry> entries = new ArrayList<>();
    private int x;
    private int y;
    private int width;
    private int height;
    private int gap = 2;
    private double scrollY;

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        relayout();
    }

    public void setGap(int gap) {
        this.gap = gap;
        relayout();
    }

    public void clear() {
        entries.clear();
        scrollY = 0;
    }

    public void add(AbstractWidget widget) {
        entries.add(new Entry(List.of(widget)));
        relayout();
    }

    public void addRow(AbstractWidget left, AbstractWidget right) {
        entries.add(new Entry(List.of(left, right)));
        relayout();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return false;
        scrollY = Math.clamp(scrollY - vertical * SCROLL_STEP, 0, maxScroll());
        relayout();
        return true;
    }

    private int contentHeight() {
        if (entries.isEmpty()) return 0;

        int total = 0;
        for (int i = 0; i < entries.size(); i++) {
            total += entries.get(i).height();
            if (i < entries.size() - 1) total += gap;
        }
        return total;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    private void relayout() {
        scrollY = Math.clamp(scrollY, 0, maxScroll());

        int currentY = y - (int) scrollY;
        for (Entry entry : entries) {
            int rowH = entry.height();
            for (AbstractWidget widget : entry.widgets()) {
                widget.setY(currentY);
            }

            int bottom = currentY + rowH;
            boolean inView = bottom > y && currentY < y + height;
            boolean fullyInView = currentY >= y && bottom <= y + height;
            for (AbstractWidget widget : entry.widgets()) {
                widget.visible = inView;
                widget.active = fullyInView;
            }

            currentY += rowH + gap;
        }
    }

    private record Entry(List<AbstractWidget> widgets) {
        int height() {
            int max = 0;
            for (AbstractWidget widget : widgets) {
                max = Math.max(max, widget.getHeight());
            }
            return max;
        }
    }
}
