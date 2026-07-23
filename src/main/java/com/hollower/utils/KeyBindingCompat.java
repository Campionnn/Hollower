package com.hollower.utils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class KeyBindingCompat {
    private KeyBindingCompat() {
    }

    public static KeyMapping register(KeyMapping mapping) {
        return mapping;
    }

    public static InputConstants.Key getBoundKey(KeyMapping mapping) {
        return InputConstants.getKey(mapping.saveString());
    }
}
