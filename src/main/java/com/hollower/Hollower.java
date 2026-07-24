package com.hollower;

import com.hollower.screen.RouteExportScreen;
import com.hollower.utils.ClientCompat;
import com.hollower.utils.ConfigUtils;
import com.hollower.utils.KeyBindingCompat;
import com.hollower.utils.NoClipController;
import com.hollower.utils.PlayerUtils;
import com.hollower.utils.RenderTweaks;
import com.hollower.utils.RenderUtils;
import com.hollower.utils.RouteUtils;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.math.Color;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
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
    public static final Minecraft client = Minecraft.getInstance();

    public static long lastToolUseTick;
    public static final List<BlockPos> positions = new ArrayList<>();
    public static int maxReach = 20;
    public static BlockPos selected;
    public static final ArrayList<String> lastCommands = new ArrayList<>();
    public static final ConcurrentHashMap<Long, ConcurrentHashMap<Long, BlockPos>> renderBlacklist =
            new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Long, BlockState> renderBlacklistState = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, String> renderBlacklistID = new ConcurrentHashMap<>();
    public static final ArrayList<String> prevRenderBlacklistID = new ArrayList<>();

    public static InputConstants.Key configKey = key(GLFW.GLFW_KEY_C);
    public static InputConstants.Key nudgeKey = key(GLFW.GLFW_KEY_LEFT_CONTROL);
    public static InputConstants.Key swapOrderKey = key(GLFW.GLFW_KEY_LEFT_ALT);
    public static InputConstants.Key etherwarpKey = key(GLFW.GLFW_KEY_LEFT_SHIFT);
    public static InputConstants.Key toggleRenderKey = key(GLFW.GLFW_KEY_X);
    public static InputConstants.Key noClipKey = key(GLFW.GLFW_KEY_N);
    private static KeyMapping configKeyBinding;
    private static KeyMapping toggleRenderKeyBinding;
    private static KeyMapping noClipKeyBinding;

    public static boolean renderToggle;
    public static volatile boolean noClip;
    public static boolean fullBright = true;
    public static boolean hideRuby;
    public static boolean hideTopaz;
    public static boolean hideSapphire;
    public static boolean hideAmethyst;
    public static boolean hideJade;
    public static boolean hideMithril;
    public static boolean hideAmber;
    public static boolean hideCoal;
    public static boolean hideIron;
    public static boolean hideRedstone;
    public static boolean hideGold;
    public static boolean hideLapis;
    public static boolean hideDiamond;
    public static boolean hideEmerald;
    public static boolean hideMiscBlocks;

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
        LevelRenderEvents.COLLECT_SUBMITS.register(RenderUtils::render);
        RenderTweaks.initialize();
        NoClipController.initialize();
        initializeKeyBindings();

        FogRenderer.toggleFog();
        prevRenderBlacklistID.addAll(renderBlacklistID.values());
    }

    public static void sendChatMessage(String message) {
        ClientCompat.sendChatMessage(client, Component.literal(message));
    }

    private static void initializeKeyBindings() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "controls"));
        configKeyBinding = KeyBindingCompat.register(new KeyMapping(
                "key.hollower.config", InputConstants.Type.KEYSYM, configKey.getValue(), category));
        toggleRenderKeyBinding = KeyBindingCompat.register(new KeyMapping(
                "key.hollower.selective_render", InputConstants.Type.KEYSYM, toggleRenderKey.getValue(), category));
        noClipKeyBinding = KeyBindingCompat.register(new KeyMapping(
                "key.hollower.noclip", InputConstants.Type.KEYSYM, noClipKey.getValue(), category));
        ClientTickEvents.END_CLIENT_TICK.register(Hollower::handleKeyBindings);
    }

    private static void handleKeyBindings(Minecraft client) {
        RouteExportScreen.openIfRequested(client);
        boolean canHandle = client.level != null
                && client.player != null
                && ClientCompat.currentScreen(client) == null;
        while (configKeyBinding.consumeClick()) {
            if (canHandle) ClientCompat.setScreen(client, ConfigUtils.createConfigBuilder().build());
        }
        while (toggleRenderKeyBinding.consumeClick()) {
            if (canHandle) toggleSelectiveRender();
        }
        while (noClipKeyBinding.consumeClick()) {
            if (canHandle) toggleNoClip();
        }
    }

    private static void toggleSelectiveRender() {
        renderToggle = !renderToggle;
        sendChatMessage("Selective rendering " + (renderToggle ? "enabled" : "disabled"));
        RenderTweaks.reloadRender();
    }

    private static void toggleNoClip() {
        if (!noClip && !NoClipController.isAvailable(client)) {
            sendChatMessage("§cNoclip is only available in local worlds");
            return;
        }
        if (noClip && !NoClipController.canDisable(client)) {
            sendChatMessage("§cMove outside blocks before disabling noclip");
            return;
        }
        noClip = !noClip;
        sendChatMessage("Noclip " + (noClip ? "enabled" : "disabled"));
        if (!noClip) NoClipController.reset();
    }

    public static InputConstants.Key getConfigKey() {
        return configKeyBinding == null ? configKey : KeyBindingCompat.getBoundKey(configKeyBinding);
    }

    public static InputConstants.Key getToggleRenderKey() {
        return toggleRenderKeyBinding == null
                ? toggleRenderKey
                : KeyBindingCompat.getBoundKey(toggleRenderKeyBinding);
    }

    public static InputConstants.Key getNoClipKey() {
        return noClipKeyBinding == null ? noClipKey : KeyBindingCompat.getBoundKey(noClipKeyBinding);
    }

    public static void setConfigKey(InputConstants.Key value) {
        configKey = value;
        updateKeyBinding(configKeyBinding, value);
    }

    public static void setToggleRenderKey(InputConstants.Key value) {
        toggleRenderKey = value;
        updateKeyBinding(toggleRenderKeyBinding, value);
    }

    public static void setNoClipKey(InputConstants.Key value) {
        noClipKey = value;
        updateKeyBinding(noClipKeyBinding, value);
    }

    private static void updateKeyBinding(KeyMapping binding, InputConstants.Key value) {
        if (binding == null) return;
        binding.setKey(value);
        KeyMapping.resetMapping();
    }

    public static boolean isKeyPressed(InputConstants.Key key) {
        return InputConstants.isKeyDown(client.getWindow(), key.getValue())
                && ClientCompat.currentScreen(client) == null;
    }

    public static void copyToClipboard(String text) {
        client.keyboardHandler.setClipboard(text);
    }

    public static String getClipboard() {
        return client.keyboardHandler.getClipboard();
    }

    public static InputConstants.Key key(int glfwKey) {
        return InputConstants.Type.KEYSYM.getOrCreate(glfwKey);
    }
}
