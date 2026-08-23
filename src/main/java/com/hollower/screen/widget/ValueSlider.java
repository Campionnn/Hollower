package com.hollower.screen.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

// A numeric control that reads "Label: 20" with the fill showing where in its range it sits.
@Environment(EnvType.CLIENT)
public final class ValueSlider extends AbstractSliderButton {
    private final String label;
    private final double min;
    private final double max;
    private final int decimals;
    private final DoubleConsumer setter;

    private ValueSlider(
            int x, int y, int width,
            String label,
            double min, double max, double initial,
            int decimals,
            DoubleConsumer setter
    ) {
        super(x, y, width, 20, Component.empty(), fraction(initial, min, max));
        this.label = label;
        this.min = min;
        this.max = max;
        this.decimals = decimals;
        this.setter = setter;
        updateMessage();
    }

    // A slider over whole numbers; the value is rounded before it reaches the setter.
    public static ValueSlider ofInt(
            int x, int y, int width,
            String label,
            int min, int max, int initial,
            java.util.function.IntConsumer setter
    ) {
        return new ValueSlider(x, y, width, label, min, max, initial, 0,
                value -> setter.accept((int) Math.round(value)));
    }

    // A slider over a fractional value, shown to `decimals` places.
    public static ValueSlider ofFloat(
            int x, int y, int width,
            String label,
            float min, float max, float initial,
            int decimals,
            FloatConsumer setter
    ) {
        return new ValueSlider(x, y, width, label, min, max, initial, decimals,
                value -> setter.accept((float) value));
    }

    // The JDK has IntConsumer and DoubleConsumer but no float equivalent.
    @FunctionalInterface
    public interface FloatConsumer {
        void accept(float value);
    }

    private static double fraction(double value, double min, double max) {
        if (max <= min) return 0.0;
        return Math.clamp((value - min) / (max - min), 0.0, 1.0);
    }

    private double current() {
        return min + value * (max - min);
    }

    // Moves the handle to newValue without invoking the setter, for when something else already owns the change.
    public void syncValue(double newValue) {
        this.value = fraction(newValue, min, max);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        // AbstractSliderButton may call this from its own constructor, before our fields are assigned.
        if (label == null) return;
        double shown = current();
        String text = decimals == 0
                ? String.valueOf(Math.round(shown))
                : String.format("%." + decimals + "f", shown);
        setMessage(Component.literal(label + ": " + text));
    }

    @Override
    protected void applyValue() {
        setter.accept(current());
    }
}
