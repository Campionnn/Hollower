package com.hollower.config.keybind;

import dev.isxander.yacl3.api.Option;

public class KeybindController implements IKeybindController<String> {
    private final Option<String> option;

    public KeybindController(Option<String> option) {
        this.option = option;
    }

    @Override
    public Option<String> option() {
        return option;
    }

    @Override
    public String getString() {
        return option.pendingValue();
    }

    @Override
    public void setFromString(String value) {
        option().requestSet(value);
    }
}
