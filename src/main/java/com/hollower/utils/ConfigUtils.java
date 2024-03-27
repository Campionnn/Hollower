package com.hollower.utils;

import com.hollower.Hollower;
import com.hollower.config.keybind.KeybindControllerBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ConfigUtils {
    private static final Map<String, Integer> keyMap = new HashMap<>();

    public static YetAnotherConfigLib createConfigBuilder() {
        return YetAnotherConfigLib.createBuilder()
                .title(Text.of("Hollower Menu"))
                .save(ConfigUtils::updateBlacklist)
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Config - General"))
                        .tooltip(Text.of("Change general configs"))
                        .option(Option.<String>createBuilder()
                                .name(Text.of("Config Key"))
                                .description(OptionDescription.of(Text.of("Key to open config menu")))
                                .binding("C", () -> Hollower.config.configKey.getLocalizedText().getString().toUpperCase(), newVal -> Hollower.config.configKey = InputUtil.fromKeyCode(getKeyCode(newVal), 0))
                                .controller(KeybindControllerBuilder::create)
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Text.of("Copy route to clipboard"))
                                .text(Text.of(""))
                                .action(((yaclScreen, buttonOption) -> RouteUtils.copyRouteToClipboard()))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.of("Import route from clipboard"))
                                .description(OptionDescription.of(Text.of("Set this value to true then save config to import route from clipboard")))
                                .binding(false, () -> false, newVal -> {if (newVal) RouteUtils.importRouteFromClipboard();})
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .formatValue(value -> Text.of(value ? "Yes" : "No"))
                                        .coloured(true))
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Text.of("Set blocks in route"))
                                .text(Text.of(""))
                                .action(((yaclScreen, buttonOption) -> RouteUtils.setBlocksInRoute("minecraft:bedrock")))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.of("Clear route"))
                                .description(OptionDescription.of(Text.of("Set this value to true then save config to clear route")))
                                .binding(false, () -> false, newVal -> {if (newVal) RouteUtils.clearRoute();})
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .formatValue(value -> Text.of(value ? "Yes" : "No"))
                                        .coloured(true))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.of("Max Reach"))
                                .description(OptionDescription.of(Text.of("Max distance to raycast when creating, deleting, or selecting blocks for routes. Use arrow keys for fine adjustment")))
                                .binding(25, () -> Hollower.config.maxReach, newVal -> Hollower.config.maxReach = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(1, 100)
                                        .step(1)
                                        .formatValue(value -> Text.of(value + " blocks")))
                                .build())
                        .option(LabelOption.createBuilder()
                                .line(Text.of("All hotkeys below only work when holding a wooden pickaxe"))
                                .line(Text.of("Creating, deleting, or selecting nodes can be changed in Minecraft controls"))
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.of("Nudge Key"))
                                .description(OptionDescription.of(Text.of("Hold key while scrolling to nudge selected block in look direction")))
                                .binding("LEFT CONTROL", () -> Hollower.config.nudgeKey.getLocalizedText().getString().toUpperCase(), newVal -> Hollower.config.nudgeKey = InputUtil.fromKeyCode(getKeyCode(newVal), 0))
                                .controller(KeybindControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.of("Swap Order Key"))
                                .description(OptionDescription.of(Text.of("Hold key while scrolling to rotate order of all blocks in route\nor hold key while selecting a new block to swap with currently selected block")))
                                .binding("LEFT ALT", () -> Hollower.config.swapOrderKey.getLocalizedText().getString().toUpperCase(), newVal -> Hollower.config.swapOrderKey = InputUtil.fromKeyCode(getKeyCode(newVal), 0))
                                .controller(KeybindControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.of("Etherwarp Key"))
                                .description(OptionDescription.of(Text.of("Hold key to teleport on top of block you are looking at")))
                                .binding("LEFT SHIFT", () -> Hollower.config.etherwarpKey.getLocalizedText().getString().toUpperCase(), newVal -> Hollower.config.etherwarpKey = InputUtil.fromKeyCode(getKeyCode(newVal), 0))
                                .controller(KeybindControllerBuilder::create)
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.of("Etherwarp Settings"))
                                .description(OptionDescription.of(Text.of("Teleport to block you are looking at when holding key")))
                                .collapsed(false)
                                .option(Option.<Color>createBuilder()
                                        .name(Text.of("Etherwarp Block Color"))
                                        .description(OptionDescription.of(Text.of("Color of block to teleport to when holding etherwarp key")))
                                        .binding(new Color(255, 0, 255), () -> new Color(Hollower.config.etherwarpBlockColor.getRGB()), newVal -> Hollower.config.etherwarpBlockColor = new Color(newVal.getRed(), newVal.getGreen(), newVal.getBlue(), 64))
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Text.of("Etherwarp Range"))
                                        .description(OptionDescription.of(Text.of("Max distance to teleport when holding etherwarp key. Use arrow keys for fine adjustment")))
                                        .binding(61, () -> Hollower.config.etherwarpRange, newVal -> Hollower.config.etherwarpRange = newVal)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(57, 100)
                                                .step(1)
                                                .formatValue(value -> Text.of(value + " blocks")))
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Config - Route Render"))
                        .tooltip(Text.of("Change rendering configs"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.of("Route Line Settings"))
                                .description(OptionDescription.of(Text.of("Settings for lines connecting nodes in route")))
                                .collapsed(false)
                                .option(Option.<Color>createBuilder()
                                        .name(Text.of("Color"))
                                        .description(OptionDescription.of(Text.of("Color of lines connecting nodes in route")))
                                        .binding(new Color(255, 0, 0), () -> new Color(Hollower.config.routeLineColor.getRGB()), newVal -> Hollower.config.routeLineColor = new Color(newVal.getRed(), newVal.getGreen(), newVal.getBlue(), 255))
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Text.of("Width"))
                                        .description(OptionDescription.of(Text.of("Width of lines connecting nodes in route")))
                                        .binding(3.0f, () -> Hollower.config.routeLineWidth, newVal -> Hollower.config.routeLineWidth = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .range(1.0f, 10.0f)
                                                .step(1.0f))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.of("Outline Block Settings"))
                                .description(OptionDescription.of(Text.of("Settings for outline around blocks in route")))
                                .collapsed(false)
                                .option(Option.<Color>createBuilder()
                                        .name(Text.of("Color"))
                                        .description(OptionDescription.of(Text.of("Color of outline around blocks in route")))
                                        .binding(new Color(0, 255, 0), () -> new Color(Hollower.config.outlineBlockColor.getRGB()), newVal -> Hollower.config.outlineBlockColor = new Color(newVal.getRed(), newVal.getGreen(), newVal.getBlue(), 255))
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Float>createBuilder()
                                        .name(Text.of("Width"))
                                        .description(OptionDescription.of(Text.of("Width of outline around blocks in route")))
                                        .binding(2.0f, () -> Hollower.config.outlineBlockWidth, newVal -> Hollower.config.outlineBlockWidth = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .range(1.0f, 10.0f)
                                                .step(1.0f))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.of("Select Block Settings"))
                                .description(OptionDescription.of(Text.of("Settings for selected block in route")))
                                .collapsed(false)
                                .option(Option.<Color>createBuilder()
                                        .name(Text.of("Color"))
                                        .description(OptionDescription.of(Text.of("Color of selected block in route")))
                                        .binding(new Color(0, 0, 255, 64), () -> new Color(Hollower.config.selectBlockColor.getRGB()), newVal -> Hollower.config.selectBlockColor = new Color(newVal.getRed(), newVal.getGreen(), newVal.getBlue(), 64))
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.of("Order Text Settings"))
                                .description(OptionDescription.of(Text.of("Settings for order text above blocks in route")))
                                .collapsed(false)
                                .option(Option.<Float>createBuilder()
                                        .name(Text.of("Scale"))
                                        .description(OptionDescription.of(Text.of("Scale of text above blocks in route")))
                                        .binding(0.04f, () -> Hollower.config.orderScale, newVal -> Hollower.config.orderScale = newVal)
                                        .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                                .range(0.0f, 1.0f)
                                                .step(0.01f))
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.of("Foreground Color"))
                                        .description(OptionDescription.of(Text.of("Color of text")))
                                        .binding(new Color(255, 255, 255), () -> new Color(Hollower.config.orderForegroundColor.getRGB()), newVal -> Hollower.config.orderForegroundColor = new Color(newVal.getRed(), newVal.getGreen(), newVal.getBlue(), 255))
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .option(Option.<Color>createBuilder()
                                        .name(Text.of("Background Color"))
                                        .description(OptionDescription.of(Text.of("Color of background behind text")))
                                        .binding(new Color(0, 0, 0, 70), () -> new Color(Hollower.config.orderBackgroundColor.getRGB()), newVal -> Hollower.config.orderBackgroundColor = new Color(newVal.getRed(), newVal.getGreen(), newVal.getBlue(), 70))
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Config - Selective Render"))
                        .tooltip(Text.of("Change selective render configs"))
                        .option(Option.<String>createBuilder()
                                .name(Text.of("Toggle Selective Render Key"))
                                .description(OptionDescription.of(Text.of("Key to toggle selective render")))
                                .binding("X", () -> Hollower.config.toggleRenderKey.getLocalizedText().getString().toUpperCase(), newVal -> Hollower.config.toggleRenderKey = InputUtil.fromKeyCode(getKeyCode(newVal), 0))
                                .controller(KeybindControllerBuilder::create)
                                .build())
                        .option(LabelOption.createBuilder()
                                .line(Text.of("Hide or show specific blocks from rendering"))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.of("Blocks to hide"))
                                .description(OptionDescription.of(Text.of("Hide or show specific blocks from rendering\nEach additional block hidden will cause some lag when crossing chunk borders")))
                                .collapsed(false)
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Ruby"))
                                        .description(OptionDescription.of(Text.of("Hide Ruby Gemstones")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideRuby, newVal -> Hollower.config.hideBlocks.hideRuby = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Topaz"))
                                        .description(OptionDescription.of(Text.of("Hide Topaz Gemstones")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideTopaz, newVal -> Hollower.config.hideBlocks.hideTopaz = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Sapphire"))
                                        .description(OptionDescription.of(Text.of("Hide Sapphire Gemstones")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideSapphire, newVal -> Hollower.config.hideBlocks.hideSapphire = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Amethyst"))
                                        .description(OptionDescription.of(Text.of("Hide Amethyst Gemstones")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideAmethyst, newVal -> Hollower.config.hideBlocks.hideAmethyst = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Jade"))
                                        .description(OptionDescription.of(Text.of("Hide Jade Gemstones")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideJade, newVal -> Hollower.config.hideBlocks.hideJade = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Amber"))
                                        .description(OptionDescription.of(Text.of("Hide Amber Gemstones")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideAmber, newVal -> Hollower.config.hideBlocks.hideAmber = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Mithril"))
                                        .description(OptionDescription.of(Text.of("Hide Mithril Ore")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideMithril, newVal -> Hollower.config.hideBlocks.hideMithril = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Coal"))
                                        .description(OptionDescription.of(Text.of("Hide Coal Ore")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideCoal, newVal -> Hollower.config.hideBlocks.hideCoal = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Iron"))
                                        .description(OptionDescription.of(Text.of("Hide Iron Ore")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideIron, newVal -> Hollower.config.hideBlocks.hideIron = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Redstone"))
                                        .description(OptionDescription.of(Text.of("Hide Redstone Ore")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideRedstone, newVal -> Hollower.config.hideBlocks.hideRedstone = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Gold"))
                                        .description(OptionDescription.of(Text.of("Hide Gold Ore")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideGold, newVal -> Hollower.config.hideBlocks.hideGold = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Lapis"))
                                        .description(OptionDescription.of(Text.of("Hide Lapis Ore")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideLapis, newVal -> Hollower.config.hideBlocks.hideLapis = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Diamond"))
                                        .description(OptionDescription.of(Text.of("Hide Diamond Ore")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideDiamond, newVal -> Hollower.config.hideBlocks.hideDiamond = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Emerald"))
                                        .description(OptionDescription.of(Text.of("Hide Emerald Ore")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideEmerald, newVal -> Hollower.config.hideBlocks.hideEmerald = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.of("Hide Misc Blocks"))
                                        .description(OptionDescription.of(Text.of("Hide every other block")))
                                        .binding(false, () -> Hollower.config.hideBlocks.hideMiscBlocks, newVal -> Hollower.config.hideBlocks.hideMiscBlocks = newVal)
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .coloured(true))
                                        .build())
                                .build())
                        .build())
                .build();
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

    public static void updateBlacklist() {
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
