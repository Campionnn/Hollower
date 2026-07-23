package com.hollower.utils;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

public final class ClientCompat {
    private ClientCompat() {
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }

    public static Screen currentScreen(Minecraft client) {
        return client.screen;
    }

    public static Camera mainCamera(GameRenderer renderer) {
        return renderer.getMainCamera();
    }

    public static void sendChatMessage(Minecraft client, Component message) {
        client.gui.getChat().addServerSystemMessage(message);
    }
}
