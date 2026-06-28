package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FilterWidgets {
    private FilterWidgets() {}

    private static <T> Button cycle(
        String name,
        Supplier<T> get,
        Function<T, T> next,
        Function<T, String> label,
        Consumer<T> set,
        int x,
        int y,
        int width
    ) {
        return Button.builder(Component.literal(name + ": " + label.apply(get.get())), btn -> {
            T value = next.apply(get.get());
            set.accept(value);
            btn.setMessage(Component.literal(name + ": " + label.apply(value)));
        }).bounds(x, y, width, UiLayout.BUTTON_HEIGHT).build();
    }

    public static Button cycleFlag(String name, Supplier<SearchFlag> get, Consumer<SearchFlag> set, int x, int y, int width) {
        return cycle(name, get, SearchFlag::next, SearchFlag::label, set, x, y, width);
    }

    public static Button toggle(String name, Supplier<Boolean> get, Consumer<Boolean> set, int x, int y, int width) {
        return cycle(name, get, b -> !b, b -> b ? "On" : "Off", set, x, y, width);
    }
}
