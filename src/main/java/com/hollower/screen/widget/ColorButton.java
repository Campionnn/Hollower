package com.hollower.screen.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.function.IntSupplier;

// A colour control that reads "Label: ████", the blocks drawn in the colour itself. Text glyphs for the
// swatch keep this a plain Button with no custom rendering needed.
@Environment(EnvType.CLIENT)
public final class ColorButton {
    private static final String SWATCH = "████";

    private ColorButton() {
    }

    public static Button create(
            int x, int y, int width,
            String label,
            IntSupplier getter,
            Runnable onPress
    ) {
        Button button = Button.builder(format(label, getter.getAsInt()), unused -> onPress.run())
                .bounds(x, y, width, 20)
                .build();
        return button;
    }

    public static Component format(String label, int argb) {
        return Component.literal(label + ": ")
                .append(Component.literal(SWATCH)
                        .withStyle(Style.EMPTY.withColor(argb & 0x00FFFFFF)));
    }
}
