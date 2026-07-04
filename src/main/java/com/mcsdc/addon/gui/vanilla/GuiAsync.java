package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GuiAsync {
    private GuiAsync() {}

    public static <T> void run(Minecraft mc, Supplier<T> work, Consumer<T> ok, Consumer<String> err) {
        CompletableFuture.supplyAsync(work)
            .thenAccept(result -> mc.execute(() -> {
                // ok runs on the render thread; an uncaught throw here (e.g. on a malformed
                // API response) would crash the game instead of showing an error
                try {
                    ok.accept(result);
                } catch (Exception ex) {
                    err.accept(errorMessage(ex));
                }
            }))
            .exceptionally(ex -> {
                mc.execute(() -> err.accept(errorMessage(ex)));
                return null;
            });
    }

    public static String errorMessage(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String msg = cause.getMessage();
        return msg != null && !msg.isBlank() ? msg : "Unknown error";
    }
}
