package com.hollower.utils;

import com.hollower.Hollower;
import net.minecraft.client.Minecraft;

public final class FullBrightController {
    private FullBrightController() {
    }

    public static boolean isActive() {
        Minecraft client = Minecraft.getInstance();
        return Hollower.fullBright
                && client.hasSingleplayerServer()
                && client.getSingleplayerServer() != null
                && client.level != null
                && client.player != null;
    }
}
