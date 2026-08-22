package com.hollower.screen.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * An on/off control that reads {@code Label: ON}.
 * <p>
 * The state lives inside the button's own label rather than in a separate widget pinned to the far side
 * of the row, which is the whole point: there is no gap between the name and the value to track across,
 * so a column of these stays readable at any window width.
 */
@Environment(EnvType.CLIENT)
public final class ToggleButton {
    private ToggleButton() {
    }

    public static Button create(
            int x, int y, int width,
            String label,
            BooleanSupplier getter,
            Consumer<Boolean> setter
    ) {
        return Button.builder(format(label, getter.getAsBoolean()), button -> {
                    setter.accept(!getter.getAsBoolean());
                    button.setMessage(format(label, getter.getAsBoolean()));
                })
                .bounds(x, y, width, 20)
                .build();
    }

    /** {@code Label: } in plain white, the state in green or grey so it reads at a glance. */
    public static Component format(String label, boolean on) {
        return Component.literal(label + ": ")
                .append(Component.literal(on ? "ON" : "OFF")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }
}
