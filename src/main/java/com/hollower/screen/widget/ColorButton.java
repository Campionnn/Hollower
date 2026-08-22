package com.hollower.screen.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.function.IntSupplier;

/**
 * A colour control that reads {@code Label: ████}, the blocks drawn in the colour itself.
 * <p>
 * Using text glyphs for the swatch keeps this a plain {@link Button} — no custom rendering, and so no
 * per-Minecraft-version render code — while still showing the value inline instead of in a separate
 * widget across the row.
 */
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
