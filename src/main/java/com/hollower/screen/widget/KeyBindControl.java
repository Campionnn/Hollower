package com.hollower.screen.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

// A keybind control that reads "Nudge Key: LEFT CTRL". Clicking arms it and the owning screen feeds it
// the next key pressed; it wraps a Button rather than extending one since Button's render is package-private.
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

    // Binds the key and disarms.
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
