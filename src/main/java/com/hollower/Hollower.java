package com.hollower;

import com.hollower.utils.*;
import me.shedaniel.math.Color;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


@Environment(EnvType.CLIENT)
public class Hollower implements ClientModInitializer {
    public static final String MOD_ID = "hollower";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static MinecraftClient client = MinecraftClient.getInstance();
    public static long lastToolUseTick;
    public static List<BlockPos> positions = new ArrayList<>();
    public static int maxReach = 20;
    public static BlockPos selected;
    public static ArrayList<String> lastCommands = new ArrayList<>();
    public static long window;
    public static ConcurrentHashMap<Long, ConcurrentHashMap<Long, BlockPos>> renderBlacklist = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Long, BlockState> renderBlacklistState = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, String> renderBlacklistID = new ConcurrentHashMap<>();
    public static ArrayList<String> prevRenderBlacklistID = new ArrayList<>();
    public static InputUtil.Key configKey = InputUtil.fromKeyCode(GLFW.GLFW_KEY_C, 0);
    public static InputUtil.Key nudgeKey = InputUtil.fromKeyCode(InputUtil.GLFW_KEY_LEFT_CONTROL, 0);
    public static InputUtil.Key swapOrderKey = InputUtil.fromKeyCode(InputUtil.GLFW_KEY_LEFT_ALT, 0);
    public static InputUtil.Key etherwarpKey = InputUtil.fromKeyCode(InputUtil.GLFW_KEY_LEFT_SHIFT, 0);
    public static InputUtil.Key toggleRenderKey = InputUtil.fromKeyCode(InputUtil.GLFW_KEY_X, 0);
    public static InputUtil.Key noClipKey = InputUtil.fromKeyCode(InputUtil.GLFW_KEY_N, 0);
    public static boolean renderToggle = false;
    public static boolean noClip = false;
    public static boolean hideRuby = false;
    public static boolean hideTopaz = false;
    public static boolean hideSapphire = false;
    public static boolean hideAmethyst = false;
    public static boolean hideJade = false;
    public static boolean hideMithril = false;
    public static boolean hideAmber = false;
    public static boolean hideCoal = false;
    public static boolean hideIron = false;
    public static boolean hideRedstone = false;
    public static boolean hideGold = false;
    public static boolean hideLapis = false;
    public static boolean hideDiamond = false;
    public static boolean hideEmerald = false;
    public static boolean hideMiscBlocks = false;
    public static Color routeLineColor = Color.ofRGBA(255, 0, 0, 255);
    public static float routeLineWidth = 3.0f;
    public static Color outlineBlockColor = Color.ofRGBA(0, 255, 0, 255);
    public static float outlineBlockWidth = 2.0f;
    public static Color selectBlockColor = Color.ofRGBA(0, 0, 255, 64);
    public static Color etherwarpBlockColor = Color.ofRGBA(255, 0, 255, 64);
    public static int etherwarpRange = 61;
    public static float orderScale = 0.04f;

    @Override
    public void onInitializeClient() {
        AttackBlockCallback.EVENT.register(new PlayerUtils());

        WorldRenderEvents.LAST.register((context) -> {
            RenderUtils.drawLines(routeLineColor, routeLineWidth, false);
            RenderUtils.outlineBlocks(outlineBlockColor, outlineBlockWidth, false);
            RenderUtils.selectBlock(selectBlockColor, false);
            if (isKeyPressed(etherwarpKey) && PlayerUtils.isHoldingTool()) {
                BlockPos etherwarpPos = RouteUtils.getRaycast(etherwarpRange);
                RenderUtils.selectBlock(etherwarpPos, etherwarpBlockColor, false);
            }
            RenderUtils.renderOrder(orderScale);
        });

        prevRenderBlacklistID.addAll(renderBlacklistID.values());
    }

    public static void sendChatMessage(String message) {
        if (client.player == null) return;
        client.player.sendMessage(Text.of(message), false);
    }

    public static void onKeyEvent(int action) {
        if (client.world == null && client.player == null) return;
        if (action == GLFW.GLFW_PRESS) {
            if (isKeyPressed(configKey)) {
                client.setScreen(ConfigUtils.createConfigBuilder().build());
                return;
            }
            if (isKeyPressed(toggleRenderKey)) {
                renderToggle = !renderToggle;
                sendChatMessage("Render " + (renderToggle ? "enabled" : "disabled"));
                RenderTweaks.reloadRender();
                return;
            }
        }
    }

    public static boolean isKeyPressed(InputUtil.Key key) {
        return InputUtil.isKeyPressed(window, key.getCode()) && client.currentScreen == null;
    }

    public static void copyToClipboard(String text) {
        client.keyboard.setClipboard(text);
    }

    public static String getClipboard() {
        return client.keyboard.getClipboard();
    }
}