package com.mcsdc.addon.mixin;

import com.mcsdc.addon.gui.EditFlagsScreen;
import com.mcsdc.addon.gui.ServerInfoScreen;
import com.mcsdc.addon.gui.vanilla.VanillaScreens;
import com.mcsdc.addon.system.McsdcSystem;
import com.mcsdc.addon.util.TicketIDGenerator;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(PauseScreen.class)
public abstract class GameMenuMixin extends Screen {

    protected GameMenuMixin(Component title) {
        super(title);
    }

    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;I)Lnet/minecraft/client/gui/layouts/LayoutElement;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void oninitWidgets(CallbackInfo ci, GridLayout gridLayout, GridLayout.RowHelper helper){
        if (mc.getConnection() == null) return;

        ServerData info = mc.getConnection().getServerData();
        if(info == null) return;

        helper.addChild(Button.builder(Component.literal("Reconnect"), (button) ->
            VanillaScreens.connectFromWorld(mc, info)
        ).width(204).build(), 2);

        McsdcSystem system = McsdcSystem.get();
        if (system.hasServerQueue()) {
            helper.addChild(Button.builder(Component.literal("Next Server").withStyle(ChatFormatting.AQUA), button ->
                VanillaScreens.connectNext(system, mc, true)
            ).width(204).build(), 2);
        }

        if (TicketIDGenerator.isValidIPv4WithPort(info.ip)){
            helper.addChild(Button.builder(Component.literal("Info"), (button) -> {
                mc.setScreen(new ServerInfoScreen(info.ip));
            }).width(204).build(), 2);

            helper.addChild(Button.builder(Component.literal("Edit Flags"), (button) -> {
                mc.setScreen(new EditFlagsScreen(info.ip));
            }).width(204).build(), 2);
        }
    }

}
