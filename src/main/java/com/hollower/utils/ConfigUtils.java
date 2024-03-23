package com.hollower.utils;

import com.hollower.Hollower;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.math.Color;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ConfigUtils {
    public static ConfigBuilder createConfigBuilder() {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(null)
                .setTitle(Text.of("Hollower Config Menu"))
                .setTransparentBackground(true);

        builder.setSavingRunnable(ConfigUtils::saveConfig);

        ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Config Key"), Hollower.configKey)
                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_C, 0))
                .setTooltip(Text.of("Key to open config menu"))
                .setKeySaveConsumer((value) -> Hollower.configKey = value)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.of("Copy route to clipboard"), false)
                .setDefaultValue(false)
                .setTooltip(Text.of("Set this value to true then save and close config menu to copy route to clipboard"))
                .setSaveConsumer((value) -> {if (value) RouteUtils.copyRouteToClipboard();})
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.of("Import route from clipboard"), false)
                .setDefaultValue(false)
                .setTooltip(Text.of("Set this value to true then save and close config menu to import route from clipboard"))
                .setSaveConsumer((value) -> {if (value) RouteUtils.importRouteFromClipboard();})
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.of("Clear route"), false)
                .setDefaultValue(false)
                .setTooltip(Text.of("Set this value to true then save and close config menu to clear route"))
                .setSaveConsumer((value) -> {if (value) RouteUtils.clearRoute();})
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.of("Set blocks in route"), false)
                .setDefaultValue(false)
                .setTooltip(Text.of("Set this value to true then save and close config menu to set a bedrock block at each position in route"))
                .setSaveConsumer((value) -> {if (value) RouteUtils.setBlocksInRoute();})
                .build());
        general.addEntry(entryBuilder.startIntField(Text.of("Max Reach"), Hollower.maxReach)
                .setDefaultValue(25)
                .setMin(1)
                .setTooltip(Text.of("Max distance to raycast when creating, deleting, or selecting blocks for routes"))
                .setSaveConsumer((value) -> Hollower.maxReach = value)
                .build());
        general.addEntry(entryBuilder.startTextDescription(Text.of("All hotkeys below only work when holding a wooden pickaxe")).build());
        general.addEntry(entryBuilder.startTextDescription(Text.of("Creating, deleting, or selecting nodes can be changed in Minecraft controls\nthrough Attack/Destroy, Use Item/Place Block, and Pick Block respectively")).build());
        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Nudge Key"), Hollower.nudgeKey)
                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_LEFT_CONTROL, 0))
                .setTooltip(Text.of("Hold key while scrolling to nudge selected block in look direction"))
                .setKeySaveConsumer((value) -> Hollower.nudgeKey = value)
                .build());
        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Swap Order Key"), Hollower.swapOrderKey)
                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_LEFT_ALT, 0))
                .setTooltip(Text.of("Hold key while scrolling to rotate order of all blocks in route\nor hold key while selecting a new block to swap with currently selected block"))
                .setKeySaveConsumer((value) -> Hollower.swapOrderKey = value)
                .build());
        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Etherwarp Key"), Hollower.etherwarpKey)
                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_LEFT_SHIFT, 0))
                .setTooltip(Text.of("Hold key to teleport on top of block you are looking at"))
                .setKeySaveConsumer((value) -> Hollower.etherwarpKey = value)
                .build());

        ConfigCategory routeRender = builder.getOrCreateCategory(Text.of("Route Render"));
        routeRender.addEntry(entryBuilder.startColorField(Text.of("Route Line Color"), Hollower.routeLineColor)
                .setDefaultValue(Color.ofRGB(255, 0, 0).getColor() & 0x00ffffff)
                .setTooltip(Text.of("Color of lines connecting nodes in route"))
                .setSaveConsumer((value) -> Hollower.routeLineColor = Color.ofOpaque(value))
                .build());
        routeRender.addEntry(entryBuilder.startFloatField(Text.of("Route Line Width"), Hollower.routeLineWidth)
                .setDefaultValue(3.0f)
                .setMin(0.0f)
                .setTooltip(Text.of("Width of lines connecting nodes in route"))
                .setSaveConsumer((value) -> Hollower.routeLineWidth = value)
                .build());
        routeRender.addEntry(entryBuilder.startColorField(Text.of("Outline Block Color"), Hollower.outlineBlockColor)
                .setDefaultValue(Color.ofRGB(0, 255, 0).getColor() & 0x00ffffff)
                .setTooltip(Text.of("Color of outline around blocks in route"))
                .setSaveConsumer((value) -> Hollower.outlineBlockColor = Color.ofOpaque(value))
                .build());
        routeRender.addEntry(entryBuilder.startFloatField(Text.of("Outline Block Width"), Hollower.outlineBlockWidth)
                .setDefaultValue(2.0f)
                .setMin(0.0f)
                .setTooltip(Text.of("Width of outline around blocks in route"))
                .setSaveConsumer((value) -> Hollower.outlineBlockWidth = value)
                .build());
        routeRender.addEntry(entryBuilder.startColorField(Text.of("Select Block Color"), Hollower.selectBlockColor)
                .setDefaultValue(0x0000FF)
                .setTooltip(Text.of("Color of selected block in route"))
                .setSaveConsumer((value) -> Hollower.selectBlockColor = Color.ofTransparent(value | (0x40 << 24)))
                .build());
        routeRender.addEntry(entryBuilder.startColorField(Text.of("Etherwarp Block Color"), Hollower.etherwarpBlockColor)
                .setDefaultValue(0xFF00FF)
                .setTooltip(Text.of("Color of block to teleport to when holding etherwarp key"))
                .setSaveConsumer((value) -> Hollower.etherwarpBlockColor = Color.ofTransparent(value | (0x40 << 24)))
                .build());
        routeRender.addEntry(entryBuilder.startIntField(Text.of("Etherwarp Range"), Hollower.etherwarpRange)
                .setDefaultValue(61)
                .setMin(1)
                .setTooltip(Text.of("Max distance to teleport when holding etherwarp key"))
                .setSaveConsumer((value) -> Hollower.etherwarpRange = value)
                .build());
        routeRender.addEntry(entryBuilder.startFloatField(Text.of("Order Scale"), Hollower.orderScale)
                .setDefaultValue(0.04f)
                .setTooltip(Text.of("Scale of order text above blocks in route"))
                .setMin(0.0f)
                .setSaveConsumer((value) -> Hollower.orderScale = value)
                .build());

        ConfigCategory selectiveRender = builder.getOrCreateCategory(Text.of("Selective Render"));
        selectiveRender.addEntry(entryBuilder.startKeyCodeField(Text.of("Toggle Selective Render Key"), Hollower.toggleRenderKey)
                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_X, 0))
                .setTooltip(Text.of("Key to toggle selective render"))
                .setKeySaveConsumer((value) -> Hollower.toggleRenderKey = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startTextDescription(Text.of("Hide or show specific blocks from rendering\nEach additional block hidden will cause some lag when crossing chunk borders")).build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Ruby"), Hollower.hideRuby)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Ruby Gemstones"))
                .setSaveConsumer((value) -> Hollower.hideRuby = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Topaz"), Hollower.hideTopaz)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Topaz Gemstones"))
                .setSaveConsumer((value) -> Hollower.hideTopaz = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Sapphire"), Hollower.hideSapphire)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Sapphire Gemstones"))
                .setSaveConsumer((value) -> Hollower.hideSapphire = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Amethyst"), Hollower.hideAmethyst)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Amethyst Gemstones"))
                .setSaveConsumer((value) -> Hollower.hideAmethyst = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Jade"), Hollower.hideJade)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Jade Gemstones"))
                .setSaveConsumer((value) -> Hollower.hideJade = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Amber"), Hollower.hideAmber)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Amber Gemstones"))
                .setSaveConsumer((value) -> Hollower.hideAmber = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Mithril"), Hollower.hideMithril)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Mithril Ore"))
                .setSaveConsumer((value) -> Hollower.hideMithril = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Coal"), Hollower.hideCoal)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Coal Ore"))
                .setSaveConsumer((value) -> Hollower.hideCoal = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Iron"), Hollower.hideIron)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Iron Ore"))
                .setSaveConsumer((value) -> Hollower.hideIron = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Redstone"), Hollower.hideRedstone)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Redstone Ore"))
                .setSaveConsumer((value) -> Hollower.hideRedstone = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Gold"), Hollower.hideGold)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Gold Ore"))
                .setSaveConsumer((value) -> Hollower.hideGold = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Lapis"), Hollower.hideLapis)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Lapis Ore"))
                .setSaveConsumer((value) -> Hollower.hideLapis = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Diamond"), Hollower.hideDiamond)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Diamond Ore"))
                .setSaveConsumer((value) -> Hollower.hideDiamond = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Emerald"), Hollower.hideEmerald)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide Emerald Ore"))
                .setSaveConsumer((value) -> Hollower.hideEmerald = value)
                .build());
        selectiveRender.addEntry(entryBuilder.startBooleanToggle(Text.of("Hide Misc Blocks"), Hollower.hideMiscBlocks)
                .setDefaultValue(false)
                .setTooltip(Text.of("Hide every other block"))
                .setSaveConsumer((value) -> Hollower.hideMiscBlocks = value)
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
        if (Hollower.hideRuby) {
            block = "block.minecraft.red_stained_glass";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.red_stained_glass_pane";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.red_stained_glass";
            Hollower.renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.red_stained_glass_pane";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideTopaz) {
            block = "block.minecraft.yellow_stained_glass";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.yellow_stained_glass_pane";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.yellow_stained_glass";
            Hollower.renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.yellow_stained_glass_pane";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideSapphire) {
            block = "block.minecraft.light_blue_stained_glass";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.light_blue_stained_glass_pane";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.light_blue_stained_glass";
            Hollower.renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.light_blue_stained_glass_pane";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideAmethyst) {
            block = "block.minecraft.purple_stained_glass";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.purple_stained_glass_pane";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.purple_stained_glass";
            Hollower.renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.purple_stained_glass_pane";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideJade) {
            block = "block.minecraft.lime_stained_glass";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.lime_stained_glass_pane";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.lime_stained_glass";
            Hollower.renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.lime_stained_glass_pane";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideAmber) {
            block = "block.minecraft.orange_stained_glass";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.orange_stained_glass_pane";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.orange_stained_glass";
            Hollower.renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.orange_stained_glass_pane";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideMithril) {
            block = "block.minecraft.light_blue_wool";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.prismarine";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.prismarine_bricks";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
            block = "block.minecraft.dark_prismarine";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.light_blue_wool";
            Hollower.renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.prismarine";
            Hollower.renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.prismarine_bricks";
            Hollower.renderBlacklistID.remove(block.hashCode());
            block = "block.minecraft.dark_prismarine";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideCoal) {
            block = "block.minecraft.coal_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.coal_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideIron) {
            block = "block.minecraft.iron_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.iron_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideRedstone) {
            block = "block.minecraft.redstone_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.redstone_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideGold) {
            block = "block.minecraft.gold_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.gold_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideLapis) {
            block = "block.minecraft.lapis_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.lapis_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideDiamond) {
            block = "block.minecraft.diamond_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.diamond_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideEmerald) {
            block = "block.minecraft.emerald_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.emerald_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.hideMiscBlocks) {
            for (String b : miscBlocks) {
                Hollower.renderBlacklistID.put(b.hashCode(), b);
            }
        } else {
            for (String b : miscBlocks) {
                Hollower.renderBlacklistID.remove(b.hashCode());
            }
        }

        if (Hollower.prevRenderBlacklistID.size() != Hollower.renderBlacklistID.size()) {
            RenderTweaks.refreshRender();
            Hollower.prevRenderBlacklistID.addAll(Hollower.renderBlacklistID.values());
        } else {
            for (String b : Hollower.prevRenderBlacklistID) {
                if (!Hollower.renderBlacklistID.containsValue(b)) {
                    RenderTweaks.refreshRender();
                    Hollower.prevRenderBlacklistID.addAll(Hollower.renderBlacklistID.values());
                    break;
                }
            }
        }
    }
}
