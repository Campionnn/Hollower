package com.hollower.config;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

public interface KeybindControllerBuilder extends ControllerBuilder<String>  {
    static KeybindControllerBuilder create(Option<String> option) {
        return new KeybindControllerBuilderImpl(option);
    }
}
