package com.hollower;

import com.hollower.utils.RenderTweaks;
import com.hollower.utils.PlayerUtils;
import com.hollower.utils.RouteUtils;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.math.Color;
import net.fabricmc.api.ClientModInitializer;

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

import com.hollower.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class Hollower implements ClientModInitializer {
    public static final String MOD_ID = "hollower";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    static MinecraftClient client = MinecraftClient.getInstance();
    public static long lastToolUseTick;
    public static List<BlockPos> positions = new ArrayList<>();
    public static int maxReach = 20;
    public static BlockPos selected;
    public static String lastCommand;
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
    public static boolean renderToggle = false;
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

    public static void onKeyEvent(int key, int action) {
        if (client.world == null && client.player == null) return;
        if (action == GLFW.GLFW_PRESS) {
            if (key == configKey.getCode()) {
                client.setScreen(createConfigBuilder().build());
                return;
            }
            if (key == toggleRenderKey.getCode()) {
                renderToggle = !renderToggle;
                client.player.sendMessage(Text.of("Toggled render to " + renderToggle), false);
                RenderTweaks.reloadRender();
                return;
            }
        }
    }

    public static boolean isKeyPressed(InputUtil.Key key) {
        return InputUtil.isKeyPressed(window, key.getCode());
    }

    public static ConfigBuilder createConfigBuilder() {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(null)
                .setTitle(Text.of("Hollower Config Menu"))
                .setTransparentBackground(true);

        builder.setSavingRunnable(Hollower::saveConfig);

        ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Config Key"), configKey)
                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_C, 0))
                .setTooltip(Text.of("Key to open config menu"))
                .setKeySaveConsumer((value) -> configKey = value)
                .build());
        general.addEntry(entryBuilder.startIntField(Text.of("Max Reach"), maxReach)
                .setDefaultValue(25)
                .setMin(1)
                .setTooltip(Text.of("Max distance to raycast when creating, deleting, or selecting blocks for routes"))
                .setSaveConsumer((value) -> maxReach = value)
                .build());
        general.addEntry(entryBuilder.startTextDescription(Text.of("All hotkeys below only work when holding a wooden pickaxe")).build());
        general.addEntry(entryBuilder.startTextDescription(Text.of("Creating, deleting, or selecting nodes can be changed in Minecraft controls\nthrough Attack/Destroy, Use Item/Place Block, and Pick Block respectively")).build());
        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Nudge Key"), nudgeKey)
                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_LEFT_CONTROL, 0))
                .setTooltip(Text.of("Hold key while scrolling to nudge selected block in look direction"))
                .setKeySaveConsumer((value) -> nudgeKey = value)
                .build());
        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Swap Order Key"), swapOrderKey)
                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_LEFT_ALT, 0))
                .setTooltip(Text.of("Hold key while scrolling to rotate order of all blocks in route\nor hold key while selecting a new block to swap with currently selected block"))
                .setKeySaveConsumer((value) -> swapOrderKey = value)
                .build());
        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Etherwarp Key"), etherwarpKey)
                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_LEFT_SHIFT, 0))
                .setTooltip(Text.of("Hold key to teleport on top of block you are looking at"))
                .setKeySaveConsumer((value) -> etherwarpKey = value)
                .build());

        ConfigCategory routeRender = builder.getOrCreateCategory(Text.of("Route Render"));
        routeRender.addEntry(entryBuilder.startColorField(Text.of("Route Line Color"), routeLineColor)
                .setDefaultValue(Color.ofRGB(255, 0, 0).getColor() & 0x00ffffff)
                .setTooltip(Text.of("Color of lines connecting nodes in route"))
                .setSaveConsumer((value) -> routeLineColor = Color.ofOpaque(value))
                .build());
        routeRender.addEntry(entryBuilder.startFloatField(Text.of("Route Line Width"), routeLineWidth)
                .setDefaultValue(3.0f)
                .setMin(0.0f)
                .setTooltip(Text.of("Width of lines connecting nodes in route"))
                .setSaveConsumer((value) -> routeLineWidth = value)
                .build());
        routeRender.addEntry(entryBuilder.startColorField(Text.of("Outline Block Color"), outlineBlockColor)
                .setDefaultValue(Color.ofRGB(0, 255, 0).getColor() & 0x00ffffff)
                .setTooltip(Text.of("Color of outline around blocks in route"))
                .setSaveConsumer((value) -> outlineBlockColor = Color.ofOpaque(value))
                .build());
        routeRender.addEntry(entryBuilder.startFloatField(Text.of("Outline Block Width"), outlineBlockWidth)
                .setDefaultValue(2.0f)
                .setMin(0.0f)
                .setTooltip(Text.of("Width of outline around blocks in route"))
                .setSaveConsumer((value) -> outlineBlockWidth = value)
                .build());
        routeRender.addEntry(entryBuilder.startColorField(Text.of("Select Block Color"), selectBlockColor)
                .setDefaultValue(0x0000FF)
                .setTooltip(Text.of("Color of selected block in route"))
                .setSaveConsumer((value) -> selectBlockColor = Color.ofTransparent(value | (0x40 << 24)))
                .build());
        routeRender.addEntry(entryBuilder.startColorField(Text.of("Etherwarp Block Color"), etherwarpBlockColor)
                .setDefaultValue(0xFF00FF)
                .setTooltip(Text.of("Color of block to teleport to when holding etherwarp key"))
                .setSaveConsumer((value) -> etherwarpBlockColor = Color.ofTransparent(value | (0x40 << 24)))
                .build());
        routeRender.addEntry(entryBuilder.startIntField(Text.of("Etherwarp Range"), etherwarpRange)
                .setDefaultValue(61)
                .setMin(1)
                .setTooltip(Text.of("Max distance to teleport when holding etherwarp key"))
                .setSaveConsumer((value) -> etherwarpRange = value)
                .build());
        routeRender.addEntry(entryBuilder.startFloatField(Text.of("Order Scale"), orderScale)
                .setDefaultValue(0.04f)
                .setTooltip(Text.of("Scale of order text above blocks in route"))
                .setMin(0.0f)
                .setSaveConsumer((value) -> orderScale = value)
                .build());

        ConfigCategory selectiveRender = builder.getOrCreateCategory(Text.of("Selective Render"));
        selectiveRender.addEntry(entryBuilder.startKeyCodeField(Text.of("Toggle Selective Render Key"), toggleRenderKey)
                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_X, 0))
                .setTooltip(Text.of("Key to toggle selective render"))
                .setKeySaveConsumer((value) -> toggleRenderKey = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startTextDescription(Text.of("Hide or show specific blocks from rendering\nEach additional block hidden will cause some lag when crossing chunk borders")).build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Ruby"), hideRuby)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Ruby Gemstones"))
                .setSaveConsumer((value) -> hideRuby = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Topaz"), hideTopaz)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Topaz Gemstones"))
                .setSaveConsumer((value) -> hideTopaz = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Sapphire"), hideSapphire)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Sapphire Gemstones"))
                .setSaveConsumer((value) -> hideSapphire = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Amethyst"), hideAmethyst)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Amethyst Gemstones"))
                .setSaveConsumer((value) -> hideAmethyst = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Jade"), hideJade)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Jade Gemstones"))
                .setSaveConsumer((value) -> hideJade = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Amber"), hideAmber)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Amber Gemstones"))
                .setSaveConsumer((value) -> hideAmber = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Mithril"), hideMithril)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Mithril Ore"))
                .setSaveConsumer((value) -> hideMithril = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Coal"), hideCoal)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Coal Ore"))
                .setSaveConsumer((value) -> hideCoal = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Iron"), hideIron)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Iron Ore"))
                .setSaveConsumer((value) -> hideIron = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Redstone"), hideRedstone)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Redstone Ore"))
                .setSaveConsumer((value) -> hideRedstone = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Gold"), hideGold)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Gold Ore"))
                .setSaveConsumer((value) -> hideGold = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Lapis"), hideLapis)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Lapis Ore"))
                .setSaveConsumer((value) -> hideLapis = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Diamond"), hideDiamond)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Diamond Ore"))
                .setSaveConsumer((value) -> hideDiamond = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Emerald"), hideEmerald)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Emerald Ore"))
                .setSaveConsumer((value) -> hideEmerald = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Misc Blocks"), hideMiscBlocks)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide every other block"))
                .setSaveConsumer((value) -> hideMiscBlocks = value)
                .build());

        return builder;
    }

    public static void saveConfig() {
        String block;
        String[] miscBlocks = {
                "block.minecraft.spruce_planks",
                "block.minecraft.brown_terracotta",
                "block.minecraft.spruce_fence",
                "block.minecraft.oak_trapdoor",
                "block.minecraft.polished_granite",
                "block.minecraft.white_wool",
                "block.minecraft.gravel",
                "block.minecraft.green_wool",
                "block.minecraft.quartz_stairs",
                "block.minecraft.jungle_planks",
                "block.minecraft.gray_wool",
                "block.minecraft.glass",
                "block.minecraft.cobblestone_slab",
                "block.minecraft.cyan_wool",
                "block.minecraft.stone_bricks",
                "block.minecraft.oak_fence",
                "block.minecraft.nether_brick_slab",
                "block.minecraft.cobblestone_stairs",
                "block.minecraft.infested_mossy_stone_bricks",
                "block.minecraft.cauldron",
                "block.minecraft.rail",
                "block.minecraft.azure_bluet",
                "block.minecraft.glowstone",
                "block.minecraft.black_wool",
                "block.minecraft.yellow_wool",
                "block.minecraft.oak_log",
                "block.minecraft.brown_mushroom",
                "block.minecraft.redstone_block",
                "block.minecraft.birch_stairs",
                "block.minecraft.vine",
                "block.minecraft.stone_brick_slab",
                "block.minecraft.grass",
                "block.minecraft.green_carpet",
                "block.minecraft.dark_oak_slab",
                "block.minecraft.diorite",
                "block.minecraft.torch",
                "block.minecraft.white_stained_glass",
                "block.minecraft.gray_terracotta",
                "block.minecraft.black_terracotta",
                "block.minecraft.smooth_stone",
                "block.minecraft.dark_oak_stairs",
                "block.minecraft.brown_wool",
                "block.minecraft.green_terracotta",
                "block.minecraft.granite",
                "block.minecraft.andesite",
                "block.minecraft.lime_terracotta",
                "block.minecraft.oak_sign",
                "block.minecraft.oak_stairs",
                "block.minecraft.obsidian",
                "block.minecraft.blue_orchid",
                "block.minecraft.gray_carpet",
                "block.minecraft.orange_wool",
                "block.minecraft.spruce_leaves",
                "block.minecraft.red_wool",
                "block.minecraft.gold_block",
                "block.minecraft.oak_wood",
                "block.minecraft.polished_diorite",
                "block.minecraft.polished_andesite",
                "block.minecraft.white_carpet",
                "block.minecraft.cobblestone",
                "block.minecraft.light_weighted_pressure_plate",
                "block.minecraft.smooth_stone_slab",
                "block.minecraft.quartz_block",
                "block.minecraft.red_sandstone",
                "block.minecraft.spruce_stairs",
                "block.minecraft.quartz_slab",
                "block.minecraft.chiseled_stone_bricks",
                "block.minecraft.cyan_terracotta",
                "block.minecraft.piston",
                "block.minecraft.diamond_block",
                "block.minecraft.cobblestone_wall",
                "block.minecraft.birch_slab",
                "block.minecraft.note_block",
                "block.minecraft.spruce_log",
                "block.minecraft.purple_wool",
                "block.minecraft.large_fern",
                "block.minecraft.skeleton_skull",
                "block.minecraft.oak_pressure_plate",
                "block.minecraft.stone_brick_stairs",
                "block.minecraft.lime_wool",
                "block.minecraft.white_terracotta",
                "block.minecraft.light_gray_wool",
                "block.minecraft.brewing_stand",
                "block.minecraft.light_gray_stained_glass",
                "block.minecraft.grass_block",
                "block.minecraft.oak_planks",
                "block.minecraft.tall_grass",
                "block.minecraft.dirt",
                "block.minecraft.light_gray_terracotta",
                "block.minecraft.fern",
                "block.minecraft.coarse_dirt",
                "block.minecraft.jungle_stairs",
                "block.minecraft.netherrack",
                "block.minecraft.spruce_slab",
                "block.minecraft.sea_lantern",
                "block.minecraft.birch_fence",
                "block.minecraft.iron_trapdoor",
                "block.minecraft.birch_planks",
                "block.minecraft.player_head",
                "block.minecraft.jungle_slab",
                "block.minecraft.red_mushroom",
                "block.minecraft.oak_slab",
                "block.minecraft.oak_leaves",
                "block.minecraft.cracked_stone_bricks",
                "block.minecraft.clay",
                "block.minecraft.light_gray_carpet",
                "block.minecraft.dark_oak_planks",
                "block.minecraft.ladder",
                "block.minecraft.fire",
                "block.minecraft.magenta_stained_glass_pane"
        };
        if (hideRuby) {
            block = "block.minecraft.red_stained_glass";
            renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.red_stained_glass_pane";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.red_stained_glass";
            renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.red_stained_glass_pane";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideTopaz) {
            block = "block.minecraft.yellow_stained_glass";
            renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.yellow_stained_glass_pane";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.yellow_stained_glass";
            renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.yellow_stained_glass_pane";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideSapphire) {
            block = "block.minecraft.light_blue_stained_glass";
            renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.light_blue_stained_glass_pane";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.light_blue_stained_glass";
            renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.light_blue_stained_glass_pane";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideAmethyst) {
            block = "block.minecraft.purple_stained_glass";
            renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.purple_stained_glass_pane";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.purple_stained_glass";
            renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.purple_stained_glass_pane";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideJade) {
            block = "block.minecraft.lime_stained_glass";
            renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.lime_stained_glass_pane";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.lime_stained_glass";
            renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.lime_stained_glass_pane";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideAmber) {
            block = "block.minecraft.orange_stained_glass";
            renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.orange_stained_glass_pane";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.orange_stained_glass";
            renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.orange_stained_glass_pane";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideMithril) {
            block = "block.minecraft.light_blue_wool";
            renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.prismarine";
            renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.prismarine_bricks";
            renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.dark_prismarine";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.light_blue_wool";
            renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.prismarine";
            renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.prismarine_bricks";
            renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.dark_prismarine";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideCoal) {
            block = "block.minecraft.coal_ore";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.coal_ore";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideIron) {
            block = "block.minecraft.iron_ore";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.iron_ore";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideRedstone) {
            block = "block.minecraft.redstone_ore";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.redstone_ore";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideGold) {
            block = "block.minecraft.gold_ore";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.gold_ore";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideLapis) {
            block = "block.minecraft.lapis_ore";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.lapis_ore";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideDiamond) {
            block = "block.minecraft.diamond_ore";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.diamond_ore";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideEmerald) {
            block = "block.minecraft.emerald_ore";
            renderBlacklistID.put(block.hashCode(), block);
        }
        else {
            block = "block.minecraft.emerald_ore";
            renderBlacklistID.remove(block.hashCode());
        }
        if (hideMiscBlocks) {
            for (String b : miscBlocks) {
                renderBlacklistID.put(b.hashCode(), b);
            }
        }
        else {
            for (String b : miscBlocks) {
                renderBlacklistID.remove(b.hashCode());
            }
        }

        if (prevRenderBlacklistID.size() != renderBlacklistID.size()) {
            RenderTweaks.refreshRender();
            prevRenderBlacklistID.addAll(renderBlacklistID.values());
        }
        else {
            for (String b : prevRenderBlacklistID) {
                if (!renderBlacklistID.containsValue(b)) {
                    RenderTweaks.refreshRender();
                    prevRenderBlacklistID.addAll(renderBlacklistID.values());
                    break;
                }
            }
        }
    }
}