package com.hollower;

import com.hollower.tweaks.RenderTweaks;
import com.hollower.utils.PlayerUtils;
import com.hollower.utils.RouteUtils;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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
    static MinecraftClient client = MinecraftClient.getInstance();
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
    public static ConcurrentHashMap<Long, ChunkPos> renderBlacklistChunk = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, String> renderBlacklistID = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Long, ConcurrentHashMap<Long, BlockPos>> renderBlackListNew = new ConcurrentHashMap<>();

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

//        long[] renderList = new long[]{141012368511082L, 141012368506986L, 141012368502890L, 141012368506987L, 141012368502891L, 141012368511083L, 141012368506988L, 141012368502892L, 141012368511084L, 140737490595946L, 140737490600042L, 140737490604138L, 140737490595947L, 140737490600043L, 140737490604139L, 140737490595948L, 140737490600044L, 140737490604140L, 141287246409834L, 141287246413930L, 141287246418026L, 141287246409835L, 141287246413931L, 141287246418027L, 141287246409836L, 141287246413932L, 141287246418028L};
//        for (long key : renderList) {
//            renderBlacklist.put(key, BlockPos.fromLong(key));
//        }
    }

    public static void onKeyEvent(int key, int action) {
        if (keysHold.containsKey(key)) {
            keysHold.put(key, action != GLFW.GLFW_RELEASE);
            return;
        }
        if (keysToggle.containsKey(key) && action == GLFW.GLFW_PRESS) {
            keysToggle.put(key, !keysToggle.get(key));
            assert client.player != null;
            client.player.sendMessage(Text.of("Toggled render to " + Hollower.keysToggle.get(Hollower.toggleRenderKey)), false);
            if (key == toggleRenderKey) {
                RenderTweaks.reloadSelective();
            }
            return;
        }
        if (key == GLFW.GLFW_KEY_SEMICOLON) {
            if (action == GLFW.GLFW_PRESS) {
                String block = "block.minecraft.redstone_ore";
                renderBlacklistID.put(block.hashCode(), block);
                block = "block.minecraft.coal_ore";
                renderBlacklistID.put(block.hashCode(), block);
                block = "block.minecraft.iron_ore";
                renderBlacklistID.put(block.hashCode(), block);
                block = "block.minecraft.gold_ore";
                renderBlacklistID.put(block.hashCode(), block);
                block = "block.minecraft.lapis_ore";
                renderBlacklistID.put(block.hashCode(), block);
                block = "block.minecraft.diamond_ore";
                renderBlacklistID.put(block.hashCode(), block);
                block = "block.minecraft.emerald_ore";
                renderBlacklistID.put(block.hashCode(), block);
                RenderTweaks.findAndAddBlocks();
                if (keysToggle.get(toggleRenderKey)) {
                    RenderTweaks.reloadSelective();
                }
            }
        }
    }
}