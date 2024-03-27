package com.hollower;

import com.hollower.utils.*;
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
    public static HollowerConfig config = new HollowerConfig();
    public static long lastToolUseTick;
    public static List<BlockPos> positions = new ArrayList<>();
    public static BlockPos selected;
    public static ArrayList<String> lastCommands = new ArrayList<>();
    public static long window;
    public static ConcurrentHashMap<Long, ConcurrentHashMap<Long, BlockPos>> renderBlacklist = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Long, BlockState> renderBlacklistState = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, String> renderBlacklistID = new ConcurrentHashMap<>();
    public static ArrayList<String> prevRenderBlacklistID = new ArrayList<>();
    public static boolean renderToggle = false;

    @Override
    public void onInitializeClient() {
        AttackBlockCallback.EVENT.register(new PlayerUtils());

        WorldRenderEvents.LAST.register((context) -> {
            RenderUtils.drawLines(config.routeLineColor, config.routeLineWidth, false);
            RenderUtils.outlineBlocks(config.outlineBlockColor, config.outlineBlockWidth, false);
            RenderUtils.selectBlock(config.selectBlockColor, false);
            if (isKeyPressed(config.etherwarpKey) && PlayerUtils.isHoldingTool()) {
                BlockPos etherwarpPos = RouteUtils.getRaycast(config.etherwarpRange);
                RenderUtils.selectBlock(etherwarpPos, config.etherwarpBlockColor, false);
            }
            RenderUtils.renderOrder(config.orderScale, config.orderForegroundColor, config.orderBackgroundColor);
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
            if (isKeyPressed(config.configKey)) {
                client.setScreen(ConfigUtils.createConfigBuilder().generateScreen(client.currentScreen));
                return;
            }
            if (isKeyPressed(config.toggleRenderKey)) {
                renderToggle = !renderToggle;
                sendChatMessage("Render " + (renderToggle ? "enabled" : "disabled"));
                RenderTweaks.reloadRender();
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