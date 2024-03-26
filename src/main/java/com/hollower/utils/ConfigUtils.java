package com.hollower.utils;

import com.hollower.Hollower;
import com.hollower.config.keybind.KeybindControllerBuilder;
import dev.isxander.yacl3.api.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ConfigUtils {
    private static final Map<String, Integer> keyMap = new HashMap<>();

    public static YetAnotherConfigLib createConfigBuilder() {
        YetAnotherConfigLib builder = YetAnotherConfigLib.createBuilder()
                .title(Text.of("Hollower Menu"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Config - General"))
                        .tooltip(Text.of("Change general configs for Hollower"))
                        .option(Option.<String>createBuilder()
                                .name(Text.of("Config Key"))
                                .description(OptionDescription.of(Text.of("Key to open config menu")))
                                .binding("C", () -> Hollower.config.configKey.getLocalizedText().getString().toUpperCase(), newVal -> Hollower.config.configKey = InputUtil.fromKeyCode(getKeyCode(newVal), 0))
                                .controller(KeybindControllerBuilder::create)
                                .build())
                        .build())
                .build();
        return builder;
//        ConfigBuilder builder = ConfigBuilder.create()
//                .setParentScreen(null)
//                .setTitle(Text.of("Hollower Config Menu"))
//                .setTransparentBackground(true);
//
//        builder.setSavingRunnable(ConfigUtils::saveConfig);
//
//        ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));
//        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
//        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Config Key"), Hollower.config.configKey)
//                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_C, 0))
//                .setTooltip(Text.of("Key to open config menu"))
//                .setKeySaveConsumer((value) -> Hollower.config.configKey = value)
//                .build());
//        general.addEntry(entryBuilder.startBooleanToggle(Text.of("Copy route to clipboard"), false)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Set this value to true then save and close config menu to copy route to clipboard"))
//                .setSaveConsumer((value) -> {if (value) RouteUtils.copyRouteToClipboard();})
//                .build());
//        general.addEntry(entryBuilder.startBooleanToggle(Text.of("Import route from clipboard"), false)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Set this value to true then save and close config menu to import route from clipboard"))
//                .setSaveConsumer((value) -> {if (value) RouteUtils.importRouteFromClipboard();})
//                .build());
//        general.addEntry(entryBuilder.startBooleanToggle(Text.of("Clear route"), false)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Set this value to true then save and close config menu to clear route"))
//                .setSaveConsumer((value) -> {if (value) RouteUtils.clearRoute();})
//                .build());
//        general.addEntry(entryBuilder.startBooleanToggle(Text.of("Set blocks in route"), false)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Set this value to true then save and close config menu to set a bedrock block at each position in route"))
//                .setSaveConsumer((value) -> {if (value) RouteUtils.setBlocksInRoute();})
//                .build());
//        general.addEntry(entryBuilder.startIntField(Text.of("Max Reach"), Hollower.config.maxReach)
//                .setDefaultValue(25)
//                .setMin(1)
//                .setTooltip(Text.of("Max distance to raycast when creating, deleting, or selecting blocks for routes"))
//                .setSaveConsumer((value) -> Hollower.config.maxReach = value)
//                .build());
//        general.addEntry(entryBuilder.startTextDescription(Text.of("All hotkeys below only work when holding a wooden pickaxe")).build());
//        general.addEntry(entryBuilder.startTextDescription(Text.of("Creating, deleting, or selecting nodes can be changed in Minecraft controls\nthrough Attack/Destroy, Use Item/Place Block, and Pick Block respectively")).build());
//        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Nudge Key"), Hollower.config.nudgeKey)
//                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_LEFT_CONTROL, 0))
//                .setTooltip(Text.of("Hold key while scrolling to nudge selected block in look direction"))
//                .setKeySaveConsumer((value) -> Hollower.config.nudgeKey = value)
//                .build());
//        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Swap Order Key"), Hollower.config.swapOrderKey)
//                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_LEFT_ALT, 0))
//                .setTooltip(Text.of("Hold key while scrolling to rotate order of all blocks in route\nor hold key while selecting a new block to swap with currently selected block"))
//                .setKeySaveConsumer((value) -> Hollower.config.swapOrderKey = value)
//                .build());
//        general.addEntry(entryBuilder.startKeyCodeField(Text.of("Etherwarp Key"), Hollower.config.etherwarpKey)
//                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_LEFT_SHIFT, 0))
//                .setTooltip(Text.of("Hold key to teleport on top of block you are looking at"))
//                .setKeySaveConsumer((value) -> Hollower.config.etherwarpKey = value)
//                .build());
//        general.addEntry(entryBuilder.startColorField(Text.of("Etherwarp Block Color"), Hollower.config.etherwarpBlockColor)
//                .setDefaultValue(0xFF00FF)
//                .setTooltip(Text.of("Color of block to teleport to when holding etherwarp key"))
//                .setSaveConsumer((value) -> Hollower.config.etherwarpBlockColor = Color.ofTransparent(value | (0x40 << 24)))
//                .build());
//        general.addEntry(entryBuilder.startIntField(Text.of("Etherwarp Range"), Hollower.config.etherwarpRange)
//                .setDefaultValue(61)
//                .setMin(1)
//                .setTooltip(Text.of("Max distance to teleport when holding etherwarp key"))
//                .setSaveConsumer((value) -> Hollower.config.etherwarpRange = value)
//                .build());
//
//        ConfigCategory routeRender = builder.getOrCreateCategory(Text.of("Route Render"));
//        routeRender.addEntry(entryBuilder.startColorField(Text.of("Route Line Color"), Hollower.config.routeLineColor)
//                .setDefaultValue(Color.ofRGB(255, 0, 0).getColor() & 0x00ffffff)
//                .setTooltip(Text.of("Color of lines connecting nodes in route"))
//                .setSaveConsumer((value) -> Hollower.config.routeLineColor = Color.ofOpaque(value))
//                .build());
//        routeRender.addEntry(entryBuilder.startFloatField(Text.of("Route Line Width"), Hollower.config.routeLineWidth)
//                .setDefaultValue(3.0f)
//                .setMin(0.0f)
//                .setTooltip(Text.of("Width of lines connecting nodes in route"))
//                .setSaveConsumer((value) -> Hollower.config.routeLineWidth = value)
//                .build());
//        routeRender.addEntry(entryBuilder.startColorField(Text.of("Outline Block Color"), Hollower.config.outlineBlockColor)
//                .setDefaultValue(Color.ofRGB(0, 255, 0).getColor() & 0x00ffffff)
//                .setTooltip(Text.of("Color of outline around blocks in route"))
//                .setSaveConsumer((value) -> Hollower.config.outlineBlockColor = Color.ofOpaque(value))
//                .build());
//        routeRender.addEntry(entryBuilder.startFloatField(Text.of("Outline Block Width"), Hollower.config.outlineBlockWidth)
//                .setDefaultValue(2.0f)
//                .setMin(0.0f)
//                .setTooltip(Text.of("Width of outline around blocks in route"))
//                .setSaveConsumer((value) -> Hollower.config.outlineBlockWidth = value)
//                .build());
//        routeRender.addEntry(entryBuilder.startColorField(Text.of("Select Block Color"), Hollower.config.selectBlockColor)
//                .setDefaultValue(0x0000FF)
//                .setTooltip(Text.of("Color of selected block in route"))
//                .setSaveConsumer((value) -> Hollower.config.selectBlockColor = Color.ofTransparent(value | (0x40 << 24)))
//                .build());
//        routeRender.addEntry(entryBuilder.startFloatField(Text.of("Order Scale"), Hollower.config.orderScale)
//                .setDefaultValue(0.04f)
//                .setTooltip(Text.of("Scale of order text above blocks in route"))
//                .setMin(0.0f)
//                .setSaveConsumer((value) -> Hollower.config.orderScale = value)
//                .build());
//
//        ConfigCategory selectiveRender = builder.getOrCreateCategory(Text.of("Selective Render"));
//        selectiveRender.addEntry(entryBuilder.startKeyCodeField(Text.of("Toggle Selective Render Key"), Hollower.config.toggleRenderKey)
//                .setDefaultValue(InputUtil.fromKeyCode(GLFW.GLFW_KEY_X, 0))
//                .setTooltip(Text.of("Key to toggle selective render"))
//                .setKeySaveConsumer((value) -> Hollower.config.toggleRenderKey = value)
//                .build());
//        selectiveRender.addEntry(entryBuilder.startTextDescription(Text.of("Hide or show specific blocks from rendering\nEach additional block hidden will cause some lag when crossing chunk borders")).build());
//        SubCategoryBuilder hideBlocks = entryBuilder.startSubCategory(Text.of("Blocks to hide"));
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Ruby"), Hollower.config.hideBlocks.hideRuby)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Ruby Gemstones"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideRuby = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Topaz"), Hollower.config.hideBlocks.hideTopaz)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Topaz Gemstones"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideTopaz = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Sapphire"), Hollower.config.hideBlocks.hideSapphire)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Sapphire Gemstones"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideSapphire = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Amethyst"), Hollower.config.hideBlocks.hideAmethyst)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Amethyst Gemstones"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideAmethyst = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Jade"), Hollower.config.hideBlocks.hideJade)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Jade Gemstones"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideJade = value)
//                .build());
//
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Amber"), Hollower.config.hideBlocks.hideAmber)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Amber Gemstones"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideAmber = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Mithril"), Hollower.config.hideBlocks.hideMithril)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Mithril Ore"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideMithril = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Coal"), Hollower.config.hideBlocks.hideCoal)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Coal Ore"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideCoal = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Iron"), Hollower.config.hideBlocks.hideIron)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Iron Ore"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideIron = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Redstone"), Hollower.config.hideBlocks.hideRedstone)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Redstone Ore"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideRedstone = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Gold"), Hollower.config.hideBlocks.hideGold)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Gold Ore"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideGold = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Lapis"), Hollower.config.hideBlocks.hideLapis)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Lapis Ore"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideLapis = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Diamond"), Hollower.config.hideBlocks.hideDiamond)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Diamond Ore"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideDiamond = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Emerald"), Hollower.config.hideBlocks.hideEmerald)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide Emerald Ore"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideEmerald = value)
//                .build());
//        hideBlocks.add(entryBuilder.startBooleanToggle(Text.of("Hide Misc Blocks"), Hollower.config.hideBlocks.hideMiscBlocks)
//                .setDefaultValue(false)
//                .setTooltip(Text.of("Hide every other block"))
//                .setSaveConsumer((value) -> Hollower.config.hideBlocks.hideMiscBlocks = value)
//                .build());
//        selectiveRender.addEntry(hideBlocks.build());
//
//        return builder;
    }

    static {
        keyMap.put("0", 48);
        keyMap.put("1", 49);
        keyMap.put("2", 50);
        keyMap.put("3", 51);
        keyMap.put("4", 52);
        keyMap.put("5", 53);
        keyMap.put("6", 54);
        keyMap.put("7", 55);
        keyMap.put("8", 56);
        keyMap.put("9", 57);
        keyMap.put("A", 65);
        keyMap.put("B", 66);
        keyMap.put("C", 67);
        keyMap.put("D", 68);
        keyMap.put("E", 69);
        keyMap.put("F", 70);
        keyMap.put("G", 71);
        keyMap.put("H", 72);
        keyMap.put("I", 73);
        keyMap.put("J", 74);
        keyMap.put("K", 75);
        keyMap.put("L", 76);
        keyMap.put("M", 77);
        keyMap.put("N", 78);
        keyMap.put("O", 79);
        keyMap.put("P", 80);
        keyMap.put("Q", 81);
        keyMap.put("R", 82);
        keyMap.put("S", 83);
        keyMap.put("T", 84);
        keyMap.put("U", 85);
        keyMap.put("V", 86);
        keyMap.put("W", 87);
        keyMap.put("X", 88);
        keyMap.put("Y", 89);
        keyMap.put("Z", 90);
        keyMap.put("F1", 290);
        keyMap.put("F2", 291);
        keyMap.put("F3", 292);
        keyMap.put("F4", 293);
        keyMap.put("F5", 294);
        keyMap.put("F6", 295);
        keyMap.put("F7", 296);
        keyMap.put("F8", 297);
        keyMap.put("F9", 298);
        keyMap.put("F10", 299);
        keyMap.put("F11", 300);
        keyMap.put("F12", 301);
        keyMap.put("F13", 302);
        keyMap.put("F14", 303);
        keyMap.put("F15", 304);
        keyMap.put("F16", 305);
        keyMap.put("F17", 306);
        keyMap.put("F18", 307);
        keyMap.put("F19", 308);
        keyMap.put("F20", 309);
        keyMap.put("F21", 310);
        keyMap.put("F22", 311);
        keyMap.put("F23", 312);
        keyMap.put("F24", 313);
        keyMap.put("F25", 314);
        keyMap.put("NUM LOCK", 282);
        keyMap.put("KEYPAD 0", 320);
        keyMap.put("KEYPAD 1", 321);
        keyMap.put("KEYPAD 2", 322);
        keyMap.put("KEYPAD 3", 323);
        keyMap.put("KEYPAD 4", 324);
        keyMap.put("KEYPAD 5", 325);
        keyMap.put("KEYPAD 6", 326);
        keyMap.put("KEYPAD 7", 327);
        keyMap.put("KEYPAD 8", 328);
        keyMap.put("KEYPAD 9", 329);
        keyMap.put("KEYPAD DECIMAL", 330);
        keyMap.put("KEYPAD ENTER", 335);
        keyMap.put("KEYPAD EQUAL", 336);
        keyMap.put("DOWN ARROW", 264);
        keyMap.put("LEFT ARROW", 263);
        keyMap.put("RIGHT ARROW", 262);
        keyMap.put("UP ARROW", 265);
        keyMap.put("KEYPAD ADD", 334);
        keyMap.put("'", 39);
        keyMap.put("\\", 92);
        keyMap.put(",", 44);
        keyMap.put("=", 61);
        keyMap.put("`", 96);
        keyMap.put("[", 91);
        keyMap.put("MINUS", 45);
        keyMap.put("KEYPAD MULTIPLY", 332);
        keyMap.put(".", 46);
        keyMap.put("]", 93);
        keyMap.put(";", 59);
        keyMap.put("/", 47);
        keyMap.put("SPACE", 32);
        keyMap.put("TAB", 258);
        keyMap.put("LEFT ALT", 342);
        keyMap.put("LEFT CONTROL", 341);
        keyMap.put("LEFT SHIFT", 340);
        keyMap.put("LEFT SUPER", 343);
        keyMap.put("RIGHT ALT", 346);
        keyMap.put("RIGHT CONTROL", 345);
        keyMap.put("RIGHT SHIFT", 344);
        keyMap.put("RIGHT SUPER", 347);
        keyMap.put("ENTER", 257);
        keyMap.put("ESCAPE", 256);
        keyMap.put("BACKSPACE", 259);
        keyMap.put("DELETE", 261);
        keyMap.put("END", 269);
        keyMap.put("HOME", 268);
        keyMap.put("INSERT", 260);
        keyMap.put("PAGE DOWN", 267);
        keyMap.put("PAGE UP", 266);
        keyMap.put("CAPS LOCK", 280);
        keyMap.put("PAUSE", 284);
        keyMap.put("SCROLL LOCK", 281);
        keyMap.put("PRINT SCREEN", 283);
    }

    private static int getKeyCode(String key) {
            Integer keyCode = keyMap.get(key);
            return keyCode != null ? keyCode : -1;
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
        if (Hollower.config.hideBlocks.hideRuby) {
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
        if (Hollower.config.hideBlocks.hideTopaz) {
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
        if (Hollower.config.hideBlocks.hideSapphire) {
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
        if (Hollower.config.hideBlocks.hideAmethyst) {
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
        if (Hollower.config.hideBlocks.hideJade) {
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
        if (Hollower.config.hideBlocks.hideAmber) {
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
        if (Hollower.config.hideBlocks.hideMithril) {
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
        if (Hollower.config.hideBlocks.hideCoal) {
            block = "block.minecraft.coal_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.coal_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.config.hideBlocks.hideIron) {
            block = "block.minecraft.iron_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.iron_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.config.hideBlocks.hideRedstone) {
            block = "block.minecraft.redstone_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.redstone_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.config.hideBlocks.hideGold) {
            block = "block.minecraft.gold_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.gold_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.config.hideBlocks.hideLapis) {
            block = "block.minecraft.lapis_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.lapis_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.config.hideBlocks.hideDiamond) {
            block = "block.minecraft.diamond_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.diamond_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.config.hideBlocks.hideEmerald) {
            block = "block.minecraft.emerald_ore";
            Hollower.renderBlacklistID.put(block.hashCode(), block);
        } else {
            block = "block.minecraft.emerald_ore";
            Hollower.renderBlacklistID.remove(block.hashCode());
        }
        if (Hollower.config.hideBlocks.hideMiscBlocks) {
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
