package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public abstract class McsdcParentScreen extends Screen {
    protected final Screen parent;

    protected McsdcParentScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    protected void rebuildUi() {
        clearWidgets();
        init();
    }

    protected void drawTitle(GuiGraphicsExtractor context) {
        context.centeredText(font, title, width / 2, UiLayout.HEADER_LABEL_Y, CommonColors.WHITE);
    }
}
