package com.hollower;

import com.hollower.utils.PlayerUtils;
import com.hollower.utils.RouteUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hollower.utils.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Hollower implements ClientModInitializer {
    public static final String MOD_ID = "hollower";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static long lastToolUseTick;
    public static List<BlockPos> positions = new ArrayList<>();
    public static int maxReach = 20;
    public static BlockPos selected;
    public static int nudgeKey = GLFW.GLFW_KEY_LEFT_CONTROL;
    public static int swapOrderKey = GLFW.GLFW_KEY_LEFT_ALT;
    public static int etherwarpKey = GLFW.GLFW_KEY_LEFT_SHIFT;
    public static Map<Integer, Boolean> keys = new HashMap<>();
    static {
        keys.put(nudgeKey, false);
        keys.put(swapOrderKey, false);
        keys.put(etherwarpKey, false);
    }

    @Override
    public void onInitializeClient() {
        AttackBlockCallback.EVENT.register(new PlayerUtils());

        WorldRenderEvents.LAST.register((context) -> {
            RenderUtils.drawLines(positions, Color.RED, 3.0F, false);
            RenderUtils.highlightBlocks(positions, Color.GREEN, 2.0f, false);
            RenderUtils.selectBlock(selected, new Color(0, 0, 255, 64), false);
            if (keys.get(etherwarpKey) && PlayerUtils.isHoldingTool()) {
                BlockPos etherwarpPos = RouteUtils.getRaycast(61);
                RenderUtils.selectBlock(etherwarpPos, new Color(255, 0, 255, 64), false);
            }

            RenderUtils.renderOrder(positions);
        });
    }

    public static void onKeyEvent(int key, int scancode, int action, int modifiers) {
        if (keys.containsKey(key)) {
            keys.put(key, action != GLFW.GLFW_RELEASE);
        }
    }
}