package com.hollower.screen;

import com.hollower.screen.widget.ColorButton;
import com.hollower.screen.widget.ValueSlider;
import com.hollower.utils.ClientCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.function.IntConsumer;

// Picks one colour with red/green/blue sliders and a hex field, previewing the result live. Alpha isn't
// offered since every colour Hollower renders has a fixed transparency; the original alpha just passes through.
@Environment(EnvType.CLIENT)
public final class ColorPickerScreen extends Screen {
    private static final int ROW = 24;

    private final Screen parent;
    private final String label;
    private final int alpha;
    private final IntConsumer onAccept;

    private int red;
    private int green;
    private int blue;

    private StringWidget preview;
    private ValueSlider redSlider;
    private ValueSlider greenSlider;
    private ValueSlider blueSlider;
    private EditBox hexField;
    // Guards the slider/hex-field responders from re-entering each other while syncing.
    private boolean syncing;

    public ColorPickerScreen(Screen parent, String label, int argb, IntConsumer onAccept) {
        super(Component.literal(label));
        this.parent = parent;
        this.label = label;
        this.alpha = argb & 0xFF000000;
        this.onAccept = onAccept;
        this.red = (argb >> 16) & 0xFF;
        this.green = (argb >> 8) & 0xFF;
        this.blue = argb & 0xFF;
    }

    private int rgb() {
        return (red << 16) | (green << 8) | blue;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(240, width - 40);
        int left = (width - panelWidth) / 2;
        int top = Math.max(30, height / 2 - 80);

        addRenderableWidget(new StringWidget(
                left, top, panelWidth, 20, Component.literal(label), font));

        preview = new StringWidget(left, top + ROW, panelWidth, 20, swatch(), font);
        addRenderableWidget(preview);

        redSlider = ValueSlider.ofInt(
                left, top + ROW * 2, panelWidth, "Red", 0, 255, red,
                value -> {
                    red = value;
                    onChannelChanged();
                });
        greenSlider = ValueSlider.ofInt(
                left, top + ROW * 3, panelWidth, "Green", 0, 255, green,
                value -> {
                    green = value;
                    onChannelChanged();
                });
        blueSlider = ValueSlider.ofInt(
                left, top + ROW * 4, panelWidth, "Blue", 0, 255, blue,
                value -> {
                    blue = value;
                    onChannelChanged();
                });
        addRenderableWidget(redSlider);
        addRenderableWidget(greenSlider);
        addRenderableWidget(blueSlider);

        hexField = new EditBox(font, left, top + ROW * 5, panelWidth, 20, Component.literal("Hex"));
        hexField.setMaxLength(7);
        hexField.setValue(hex());
        hexField.setResponder(this::onHexTyped);
        hexField.setTooltip(Tooltip.create(Component.literal(
                "Paste a hex colour such as #FF0000. The sliders follow once six digits are in.")));
        addRenderableWidget(hexField);

        int half = (panelWidth - 4) / 2;
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> accept())
                .bounds(left, top + ROW * 6 + 6, half, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(left + half + 4, top + ROW * 6 + 6, panelWidth - half - 4, 20)
                .build());
    }

    private Component swatch() {
        return Component.literal("████████████")
                .withStyle(Style.EMPTY.withColor(rgb()));
    }

    private String hex() {
        return String.format("#%06X", rgb());
    }

    private void onChannelChanged() {
        if (syncing) return;
        syncing = true;
        if (hexField != null) hexField.setValue(hex());
        syncing = false;
        refreshPreview();
    }

    // Accepts partial input silently; only applies once it parses as six hex digits.
    private void onHexTyped(String text) {
        if (syncing) return;
        String cleaned = text.startsWith("#") ? text.substring(1) : text;
        if (cleaned.length() != 6) return;
        int parsed;
        try {
            parsed = Integer.parseInt(cleaned, 16);
        } catch (NumberFormatException ignored) {
            return;
        }
        red = (parsed >> 16) & 0xFF;
        green = (parsed >> 8) & 0xFF;
        blue = parsed & 0xFF;
        // Move the handles rather than rebuilding the screen, which would steal focus mid-keystroke.
        syncing = true;
        redSlider.syncValue(red);
        greenSlider.syncValue(green);
        blueSlider.syncValue(blue);
        syncing = false;
        refreshPreview();
    }

    private void refreshPreview() {
        if (preview != null) preview.setMessage(swatch());
    }

    private void accept() {
        onAccept.accept(alpha | rgb());
        onClose();
    }

    @Override
    public void onClose() {
        ClientCompat.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
