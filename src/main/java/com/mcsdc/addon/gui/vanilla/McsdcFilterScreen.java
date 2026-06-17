package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.List;
import java.util.function.BiFunction;

public class McsdcFilterScreen extends McsdcParentScreen {
    @FunctionalInterface
    private interface WidgetFactory extends BiFunction<Integer, Integer, AbstractWidget> {}

    private final BrowseSearchState state;
    private final FilterScrollLayout scroll = new FilterScrollLayout();

    public McsdcFilterScreen(Screen parent, BrowseSearchState state) {
        super(Component.literal("Search Filters"), parent);
        this.state = state;
    }

    @Override
    protected void init() {
        int margin = UiLayout.margin(width);
        int panelW = width - margin * 2;
        int top = UiLayout.CONTENT_TOP + 8;
        int footerY = UiLayout.footerY(height, width);
        int panelH = footerY - top - 8;
        boolean twoColumns = panelW >= 340;
        int colGap = 8;
        int colW = twoColumns ? (panelW - colGap) / 2 : panelW;

        scroll.clear();
        scroll.setBounds(margin, top, panelW, panelH);
        scroll.setGap(twoColumns ? 4 : 2);

        addSection(List.of(
            flag("Visited", () -> state.visited, f -> state.visited = f),
            flag("Modded", () -> state.modded, f -> state.modded = f),
            flag("Whitelist", () -> state.whitelist, f -> state.whitelist = f),
            flag("Cracked", () -> state.cracked, f -> state.cracked = f),
            flag("Griefed", () -> state.griefed, f -> state.griefed = f),
            flag("Saved", () -> state.saved, f -> state.saved = f),
            flag("Active", () -> state.active, f -> state.active = f),
            flag("History", () -> state.hasHistory, f -> state.hasHistory = f),
            flag("Notes", () -> state.hasNotes, f -> state.hasNotes = f),
            toggle("Hide offline", () -> state.hideOffline, v -> state.hideOffline = v)
        ), margin, colW, colGap, panelW, twoColumns);

        addRow(Button.builder(Component.literal("Version: " + state.version.version), b ->
            minecraft.setScreen(new McsdcVersionSelectScreen(this, state.version, v -> {
                state.version = v;
                rebuildUi();
            })))
            .bounds(margin, 0, panelW, UiLayout.BUTTON_HEIGHT).build());

        addRow(FilterWidgets.toggle("Advanced MOTD", state.advancedMotd, v -> {
            state.advancedMotd = v;
            rebuildUi();
        }, margin, 0, panelW));

        if (state.advancedMotd) {
            addSection(List.of(
                flag("Default", () -> state.defaultMotd, f -> state.defaultMotd = f),
                flag("Community", () -> state.communityMotd, f -> state.communityMotd = f),
                flag("Creative", () -> state.creativeMotd, f -> state.creativeMotd = f),
                flag("Bigotry", () -> state.bigotryMotd, f -> state.bigotryMotd = f),
                flag("Furry", () -> state.furryMotd, f -> state.furryMotd = f),
                flag("LGBT", () -> state.lgbtMotd, f -> state.lgbtMotd = f)
            ), margin, colW, colGap, panelW, twoColumns);
        }

        UiLayout.ButtonSlot done = UiLayout.footerButton(width, height, 100);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(done.x(), done.y(), done.width(), UiLayout.BUTTON_HEIGHT).build());
    }

    private WidgetFactory flag(String name, java.util.function.Supplier<SearchFlag> get, java.util.function.Consumer<SearchFlag> set) {
        return (x, w) -> FilterWidgets.cycleFlag(name, get.get(), set, x, 0, w);
    }

    private WidgetFactory toggle(String name, java.util.function.Supplier<Boolean> get, java.util.function.Consumer<Boolean> set) {
        return (x, w) -> FilterWidgets.toggle(name, get.get(), set, x, 0, w);
    }

    private void addSection(
        List<WidgetFactory> factories,
        int margin,
        int colW,
        int colGap,
        int panelW,
        boolean twoColumns
    ) {
        for (int i = 0; i < factories.size(); ) {
            if (twoColumns && i + 1 < factories.size()) {
                addPair(
                    factories.get(i).apply(margin, colW),
                    factories.get(i + 1).apply(margin + colW + colGap, colW)
                );
                i += 2;
            } else {
                addRow(factories.get(i).apply(margin, panelW));
                i += 1;
            }
        }
    }

    private void addRow(AbstractWidget widget) {
        addRenderableWidget(widget);
        scroll.add(widget);
    }

    private void addPair(AbstractWidget left, AbstractWidget right) {
        addRenderableWidget(left);
        addRenderableWidget(right);
        scroll.addRow(left, right);
    }

    private void rebuildUi() {
        clearWidgets();
        init();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (scroll.mouseScrolled(mouseX, mouseY, vertical)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, UiLayout.HEADER_LABEL_Y, CommonColors.WHITE);
        context.centeredText(font, "Click to cycle · scroll for more", width / 2, UiLayout.CONTENT_TOP - 2, CommonColors.GRAY);
    }
}
