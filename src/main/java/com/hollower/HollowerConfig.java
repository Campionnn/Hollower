package com.hollower;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class HollowerConfig {
    public InputUtil.Key configKey = InputUtil.fromKeyCode(GLFW.GLFW_KEY_C, 0);
    public InputUtil.Key nudgeKey = InputUtil.fromKeyCode(InputUtil.GLFW_KEY_LEFT_CONTROL, 0);
    public InputUtil.Key swapOrderKey = InputUtil.fromKeyCode(InputUtil.GLFW_KEY_LEFT_ALT, 0);
    public InputUtil.Key etherwarpKey = InputUtil.fromKeyCode(InputUtil.GLFW_KEY_LEFT_SHIFT, 0);
    public InputUtil.Key toggleRenderKey = InputUtil.fromKeyCode(InputUtil.GLFW_KEY_X, 0);
    public HideBlocks hideBlocks = new HideBlocks();
    public Color routeLineColor = new Color(255, 0, 0, 255);
    public float routeLineWidth = 3.0f;
    public Color outlineBlockColor = new Color(0, 255, 0, 255);
    public float outlineBlockWidth = 2.0f;
    public Color selectBlockColor = new Color(0, 0, 255, 64);
    public Color etherwarpBlockColor = new Color(255, 0, 255, 64);
    public int etherwarpRange = 61;
    public float orderScale = 0.04f;
    public Color orderForegroundColor = new Color(255, 255, 255, 255);
    public Color orderBackgroundColor = new Color(0, 0, 0, 70);
    public int maxReach = 20;

    public static class HideBlocks {
        public boolean hideRuby = false;
        public boolean hideTopaz = false;
        public boolean hideSapphire = false;
        public boolean hideAmethyst = false;
        public boolean hideJade = false;
        public boolean hideMithril = false;
        public boolean hideAmber = false;
        public boolean hideCoal = false;
        public boolean hideIron = false;
        public boolean hideRedstone = false;
        public boolean hideGold = false;
        public boolean hideLapis = false;
        public boolean hideDiamond = false;
        public boolean hideEmerald = false;
        public boolean hideMiscBlocks = false;
    }
}
