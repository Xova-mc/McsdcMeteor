package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class McsdcFilterScreen extends McsdcParentScreen {
    private record FilterDef(String name, Supplier<SearchFlag> get, Consumer<SearchFlag> set) {}

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

        addFlagSection(new FilterDef[] {
            new FilterDef("Visited", () -> state.visited, f -> state.visited = f),
            new FilterDef("Modded", () -> state.modded, f -> state.modded = f),
            new FilterDef("Whitelist", () -> state.whitelist, f -> state.whitelist = f),
            new FilterDef("Cracked", () -> state.cracked, f -> state.cracked = f),
            new FilterDef("Griefed", () -> state.griefed, f -> state.griefed = f),
            new FilterDef("Saved", () -> state.saved, f -> state.saved = f),
            new FilterDef("Active", () -> state.active, f -> state.active = f),
            new FilterDef("History", () -> state.hasHistory, f -> state.hasHistory = f),
            new FilterDef("Notes", () -> state.hasNotes, f -> state.hasNotes = f),
        }, margin, colW, colGap, panelW, twoColumns);

        addRow(FilterWidgets.toggle("Hide offline", () -> state.hideOffline, v -> state.hideOffline = v, margin, 0, panelW));

        addRow(Button.builder(Component.literal("Version: " + state.version.version), b ->
            minecraft.setScreen(new McsdcVersionSelectScreen(this, state.version, v -> {
                state.version = v;
                rebuildUi();
            })))
            .bounds(margin, 0, panelW, UiLayout.BUTTON_HEIGHT).build());

        addRow(FilterWidgets.toggle("Advanced MOTD", () -> state.advancedMotd, v -> {
            state.advancedMotd = v;
            rebuildUi();
        }, margin, 0, panelW));

        if (state.advancedMotd) {
            addFlagSection(new FilterDef[] {
                new FilterDef("Default", () -> state.defaultMotd, f -> state.defaultMotd = f),
                new FilterDef("Community", () -> state.communityMotd, f -> state.communityMotd = f),
                new FilterDef("Creative", () -> state.creativeMotd, f -> state.creativeMotd = f),
                new FilterDef("Bigotry", () -> state.bigotryMotd, f -> state.bigotryMotd = f),
                new FilterDef("Furry", () -> state.furryMotd, f -> state.furryMotd = f),
                new FilterDef("LGBT", () -> state.lgbtMotd, f -> state.lgbtMotd = f),
            }, margin, colW, colGap, panelW, twoColumns);
        }

        UiLayout.ButtonSlot done = UiLayout.footerButton(width, height, 100);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(done.x(), done.y(), done.width(), UiLayout.BUTTON_HEIGHT).build());
    }

    private void addFlagSection(FilterDef[] defs, int margin, int colW, int colGap, int panelW, boolean twoColumns) {
        for (int i = 0; i < defs.length; ) {
            if (twoColumns && i + 1 < defs.length) {
                FilterDef left = defs[i];
                FilterDef right = defs[i + 1];
                addPair(
                    FilterWidgets.cycleFlag(left.name(), left.get, left.set, margin, 0, colW),
                    FilterWidgets.cycleFlag(right.name(), right.get, right.set, margin + colW + colGap, 0, colW)
                );
                i += 2;
            } else {
                FilterDef def = defs[i];
                addRow(FilterWidgets.cycleFlag(def.name(), def.get, def.set, margin, 0, panelW));
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (scroll.mouseScrolled(mouseX, mouseY, vertical)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        drawTitle(context);
        context.centeredText(font, "Click to cycle · scroll for more", width / 2, UiLayout.CONTENT_TOP - 2, CommonColors.GRAY);
    }
}
