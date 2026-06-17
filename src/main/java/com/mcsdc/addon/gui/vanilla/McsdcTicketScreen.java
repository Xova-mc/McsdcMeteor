package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.gui.ServerInfoScreen;
import com.mcsdc.addon.util.TicketIDGenerator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class McsdcTicketScreen extends McsdcParentScreen {
    private EditBox ticketField;
    private String status = "";

    public McsdcTicketScreen(Screen parent) {
        super(Component.literal("Ticket ID"), parent);
    }

    @Override
    protected void init() {
        int margin = UiLayout.margin(width);
        int footerY = UiLayout.footerY(height, width);
        int fieldW = Math.min(200, width - margin * 2);

        ticketField = new EditBox(font, width / 2 - fieldW / 2, height / 2 - 10, fieldW, UiLayout.BUTTON_HEIGHT, Component.literal("Ticket ID"));
        ticketField.setMaxLength(128);
        addRenderableWidget(ticketField);

        addRenderableWidget(Button.builder(Component.literal("Search"), b -> search())
            .bounds(width / 2 - 50, height / 2 + 16, 100, UiLayout.BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(width / 2 - 50, Math.min(height / 2 + 44, footerY), 100, UiLayout.BUTTON_HEIGHT).build());
    }

    private void search() {
        String id = ticketField.getValue().trim();
        if (id.isEmpty()) {
            status = "Enter a Ticket ID.";
            return;
        }
        try {
            String ip = TicketIDGenerator.decodeTicketID(id);
            if (!TicketIDGenerator.isValidIPv4WithPort(ip)) {
                status = "Invalid Ticket ID.";
                return;
            }
            minecraft.setScreen(new ServerInfoScreen(ip));
        } catch (Exception e) {
            status = "Error decoding Ticket ID.";
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, height / 2 - 36, CommonColors.WHITE);
        if (!status.isEmpty()) {
            context.centeredText(font, status, width / 2, height / 2 + 70, CommonColors.SOFT_RED);
        }
    }
}
