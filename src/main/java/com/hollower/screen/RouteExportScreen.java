package com.hollower.screen;

import com.hollower.utils.ClientCompat;
import com.hollower.utils.RouteExportCodec;
import com.hollower.utils.RouteUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Picks the format a route is copied to the clipboard in. Opened straight from the config screen's
// "Export Route" button and returns to it when done.
@Environment(EnvType.CLIENT)
public final class RouteExportScreen extends Screen {
    private final Screen parent;

    public RouteExportScreen(Screen parent) {
        super(Component.literal("Export Route"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(220, width - 40);
        int left = (width - buttonWidth) / 2;
        int top = Math.max(40, height / 2 - 72);

        addRenderableWidget(new StringWidget(
                left, top - 30, buttonWidth, 20,
                Component.literal("Copy route to clipboard as").withStyle(ChatFormatting.YELLOW), font));
        addTargetButton(left, top, buttonWidth, "Waypointer (Recommended)", RouteExportCodec.Target.WAYPOINTER);
        addTargetButton(left, top + 24, buttonWidth, "SkyHanni", RouteExportCodec.Target.SKYHANNI);
        addTargetButton(left, top + 48, buttonWidth, "Skyblocker", RouteExportCodec.Target.SKYBLOCKER);
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(left, top + 84, buttonWidth, 20)
                .build());
    }

    private void addTargetButton(int x, int y, int width, String label, RouteExportCodec.Target target) {
        addRenderableWidget(Button.builder(Component.literal(label), button -> {
                    if (RouteUtils.copyRouteToClipboard(target)) onClose();
                })
                .bounds(x, y, width, 20)
                .build());
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
