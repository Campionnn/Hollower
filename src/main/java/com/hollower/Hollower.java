package com.hollower;

import com.hollower.tweaks.RenderTweaks;
import com.hollower.utils.PlayerUtils;
import com.hollower.utils.RouteUtils;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
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
import java.util.concurrent.ConcurrentHashMap;


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
    public static BlockPos lastTeleportPos;
    public static Map<Integer, Boolean> keysHold = new HashMap<>();
    static {
        keysHold.put(nudgeKey, false);
        keysHold.put(swapOrderKey, false);
        keysHold.put(etherwarpKey, false);
    }
    public static int toggleRenderKey = GLFW.GLFW_KEY_X;
    public static Map<Integer, Boolean> keysToggle = new HashMap<>();
    static {
        keysToggle.put(toggleRenderKey, false);
    }
    public static ConcurrentHashMap<Long, BlockPos> renderBlacklist = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Long, BlockPos> renderBlacklistCache = new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        AttackBlockCallback.EVENT.register(new PlayerUtils());

        WorldRenderEvents.LAST.register((context) -> {
            RenderUtils.drawLines(Color.RED, 3.0F, false);
            RenderUtils.highlightBlocks(Color.GREEN, 2.0f, false);
            RenderUtils.selectBlock(new Color(0, 0, 255, 64), false);
            if (keysHold.get(etherwarpKey) && PlayerUtils.isHoldingTool()) {
                BlockPos etherwarpPos = RouteUtils.getRaycast(61);
                RenderUtils.selectBlock(etherwarpPos, new Color(255, 0, 255, 64), false);
            }

            RenderUtils.renderOrder();
        });
    }

    public static void onKeyEvent(int key, int scancode, int action, int modifiers) {
        if (keysHold.containsKey(key)) {
            keysHold.put(key, action != GLFW.GLFW_RELEASE);
            return;
        }
        if (keysToggle.containsKey(key) && action == GLFW.GLFW_PRESS) {
            keysToggle.put(key, !keysToggle.get(key));
            if (key == toggleRenderKey) {
                RenderTweaks.reloadSelective();
            }
            return;
        }
    }
}