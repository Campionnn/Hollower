package com.hollower.screen.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A keybind control that reads {@code Nudge Key: LEFT CTRL}. Clicking arms it — the label becomes
 * {@code > press a key <} — and the owning screen feeds it the next key pressed.
 * <p>
 * Capture lives on the screen rather than here because a widget only receives key events while focused,
 * which is not reliable enough for a control whose whole job is catching the very next keystroke. The
 * screen decides which control is armed and calls {@link #bind}; this class decides how it looks.
 * <p>
 * It wraps a {@link Button} rather than extending one: {@code Button}'s render method is package-private,
 * so a subclass outside {@code net.minecraft.client.gui.components} cannot satisfy it.
 */
@Environment(EnvType.CLIENT)
public final class KeyBindControl {
    private final String label;
    private final Supplier<InputConstants.Key> getter;
    private final Consumer<InputConstants.Key> setter;
    private final Button button;
    private boolean armed;

    public KeyBindControl(
            String label,
            Supplier<InputConstants.Key> getter,
            Consumer<InputConstants.Key> setter,
            Consumer<KeyBindControl> onArm
    ) {
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.button = Button.builder(Component.empty(), unused -> onArm.accept(this))
                .bounds(0, 0, 0, 20)
                .build();
        refresh();
    }

    public Button widget() {
        return button;
    }

    public void setArmed(boolean value) {
        armed = value;
        refresh();
    }

    /** Binds {@code key} and disarms. Backing out without rebinding is the caller's call, not ours. */
    public void bind(InputConstants.Key key) {
        setter.accept(key);
        armed = false;
        refresh();
    }

    private void refresh() {
        if (armed) {
            button.setMessage(Component.literal("> press a key <").withStyle(ChatFormatting.YELLOW));
            return;
        }
        button.setMessage(Component.literal(label + ": ")
                .append(getter.get().getDisplayName().copy().withStyle(ChatFormatting.AQUA)));
    }
}
