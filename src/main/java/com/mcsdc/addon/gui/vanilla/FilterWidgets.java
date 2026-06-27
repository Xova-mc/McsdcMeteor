package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;

public final class FilterWidgets {
    private FilterWidgets() {}

    private static <T> Button cycle(
        String name,
        T initial,
        Function<T, T> next,
        Function<T, String> label,
        Consumer<T> onChange,
        int x,
        int y,
        int width
    ) {
        @SuppressWarnings("unchecked")
        T[] current = (T[]) new Object[]{initial};
        return Button.builder(Component.literal(name + ": " + label.apply(initial)), btn -> {
            current[0] = next.apply(current[0]);
            btn.setMessage(Component.literal(name + ": " + label.apply(current[0])));
            onChange.accept(current[0]);
        }).bounds(x, y, width, UiLayout.BUTTON_HEIGHT).build();
    }

    public static Button cycleFlag(String name, SearchFlag initial, Consumer<SearchFlag> onChange, int x, int y, int width) {
        return cycle(name, initial, flag -> flag.next(), flag -> flag.label(), onChange, x, y, width);
    }

    public static Button toggle(String name, boolean initial, Consumer<Boolean> onChange, int x, int y, int width) {
        return cycle(name, initial, b -> !b, b -> b ? "On" : "Off", onChange, x, y, width);
    }
}
