package com.hollower.config;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.text.Text;

public interface IKeybindController<T> extends Controller<T> {
    String getString();

    void setFromString(String value);

    @Override
    default Text formatValue() {
        return Text.literal(getString());
    }

    default boolean isInputValid(String value) {
        return true;
    }

    @Override
    default AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new KeybindControllerElement(this, screen, widgetDimension, true);
    }
}
