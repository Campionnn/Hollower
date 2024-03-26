package com.hollower.config;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.impl.controller.AbstractControllerBuilderImpl;

public class KeybindControllerBuilderImpl extends AbstractControllerBuilderImpl<String> implements KeybindControllerBuilder {
    public KeybindControllerBuilderImpl(Option<String> option) {
        super(option);
    }

    @Override
    public Controller<String> build() {
        return new KeybindController(option);
    }
}
