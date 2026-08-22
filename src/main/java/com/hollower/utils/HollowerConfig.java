package com.hollower.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.hollower.Hollower;
import com.hollower.render.SelectiveRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Persists the {@link Hollower} settings that should survive a game restart: keybinds, max reach,
 * fullbright, etherwarp options, route render options, and which selective-render groups are hidden.
 * <p>
 * Runtime state is deliberately left out — {@code renderToggle} and {@code noClip} are per-session, so
 * writing them would silently re-enable noclip on the next launch.
 */
@Environment(EnvType.CLIENT)
public final class HollowerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("hollower.json");

    private HollowerConfig() {
    }

    private static final class Data {
        int configKey = GLFW.GLFW_KEY_C;
        int nudgeKey = GLFW.GLFW_KEY_LEFT_CONTROL;
        int swapOrderKey = GLFW.GLFW_KEY_LEFT_ALT;
        int etherwarpKey = GLFW.GLFW_KEY_LEFT_SHIFT;
        int toggleRenderKey = GLFW.GLFW_KEY_X;
        int noClipKey = GLFW.GLFW_KEY_N;

        int maxReach = 20;
        boolean fullBright = true;

        int etherwarpBlockColor = 0x40FF00FF;
        int etherwarpRange = 61;

        int routeLineColor = 0xFFFF0000;
        float routeLineWidth = 3.0f;
        int outlineBlockColor = 0xFF00FF00;
        float outlineBlockWidth = 2.0f;
        int selectBlockColor = 0x400000FF;
        float orderScale = 0.04f;

        /** Enum names of the hidden {@code HiddenBlockGroup}s — names, so reordering the enum is safe. */
        List<String> hiddenGroups = List.of();
    }

    /** Loads persisted values into {@link Hollower}'s static fields. Call before keybindings are registered. */
    public static void load() {
        if (!Files.isRegularFile(PATH)) return;

        Data data;
        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            data = GSON.fromJson(reader, Data.class);
        } catch (IOException | JsonSyntaxException e) {
            Hollower.LOGGER.warn("Failed to read Hollower config, using defaults", e);
            return;
        }
        if (data == null) return;

        Hollower.configKey = Hollower.key(data.configKey);
        Hollower.nudgeKey = Hollower.key(data.nudgeKey);
        Hollower.swapOrderKey = Hollower.key(data.swapOrderKey);
        Hollower.etherwarpKey = Hollower.key(data.etherwarpKey);
        Hollower.toggleRenderKey = Hollower.key(data.toggleRenderKey);
        Hollower.noClipKey = Hollower.key(data.noClipKey);

        Hollower.maxReach = data.maxReach;
        Hollower.fullBright = data.fullBright;

        Hollower.etherwarpBlockColor = data.etherwarpBlockColor;
        Hollower.etherwarpRange = data.etherwarpRange;

        Hollower.routeLineColor = data.routeLineColor;
        Hollower.routeLineWidth = data.routeLineWidth;
        Hollower.outlineBlockColor = data.outlineBlockColor;
        Hollower.outlineBlockWidth = data.outlineBlockWidth;
        Hollower.selectBlockColor = data.selectBlockColor;
        Hollower.orderScale = data.orderScale;

        SelectiveRender.loadState(data.hiddenGroups);
    }

    /** Writes the current values of the persisted fields to disk. */
    public static void save() {
        Data data = new Data();
        data.configKey = Hollower.getConfigKey().getValue();
        data.nudgeKey = Hollower.nudgeKey.getValue();
        data.swapOrderKey = Hollower.swapOrderKey.getValue();
        data.etherwarpKey = Hollower.etherwarpKey.getValue();
        data.toggleRenderKey = Hollower.getToggleRenderKey().getValue();
        data.noClipKey = Hollower.getNoClipKey().getValue();

        data.maxReach = Hollower.maxReach;
        data.fullBright = Hollower.fullBright;

        data.etherwarpBlockColor = Hollower.etherwarpBlockColor;
        data.etherwarpRange = Hollower.etherwarpRange;

        data.routeLineColor = Hollower.routeLineColor;
        data.routeLineWidth = Hollower.routeLineWidth;
        data.outlineBlockColor = Hollower.outlineBlockColor;
        data.outlineBlockWidth = Hollower.outlineBlockWidth;
        data.selectBlockColor = Hollower.selectBlockColor;
        data.orderScale = Hollower.orderScale;

        data.hiddenGroups = SelectiveRender.saveState();

        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            Hollower.LOGGER.warn("Failed to save Hollower config", e);
        }
    }
}
