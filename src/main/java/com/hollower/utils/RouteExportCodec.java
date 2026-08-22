package com.hollower.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class RouteExportCodec {
    public enum Target {
        WAYPOINTER("Waypointer"),
        SKYHANNI("SkyHanni"),
        SKYBLOCKER("Skyblocker");

        private final String displayName;

        Target(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private RouteExportCodec() {
    }

    public static String encode(List<BlockPos> positions, Target target) {
        return switch (target) {
            case WAYPOINTER -> WaypointerV8Codec.encode(positions);
            case SKYHANNI -> encodeSkyHanni(positions);
            case SKYBLOCKER -> encodeSkyblocker(positions);
        };
    }

    private static String encodeSkyHanni(List<BlockPos> positions) {
        JsonArray waypoints = new JsonArray();
        for (int index = 0; index < positions.size(); index++) {
            BlockPos pos = positions.get(index);
            JsonObject waypoint = new JsonObject();
            waypoint.addProperty("x", pos.getX());
            waypoint.addProperty("y", pos.getY());
            waypoint.addProperty("z", pos.getZ());
            waypoint.addProperty("r", 0);
            waypoint.addProperty("g", 1);
            waypoint.addProperty("b", 0);

            JsonObject options = new JsonObject();
            options.addProperty("name", Integer.toString(index + 1));
            waypoint.add("options", options);
            waypoints.add(waypoint);
        }
        return waypoints.toString();
    }

    private static String encodeSkyblocker(List<BlockPos> positions) {
        JsonObject group = new JsonObject();
        group.addProperty("name", "Hollower Route");
        group.addProperty("island", "crystal_hollows");
        group.addProperty("ordered", true);
        group.addProperty("renderThroughWalls", true);

        JsonArray waypoints = new JsonArray();
        for (int index = 0; index < positions.size(); index++) {
            BlockPos pos = positions.get(index);
            JsonObject waypoint = new JsonObject();
            JsonArray position = new JsonArray();
            position.add(pos.getX());
            position.add(pos.getY());
            position.add(pos.getZ());
            waypoint.add("pos", position);
            waypoint.addProperty("name", Integer.toString(index + 1));

            JsonArray color = new JsonArray();
            color.add(0.0);
            color.add(1.0);
            color.add(0.0);
            waypoint.add("colorComponents", color);
            waypoint.addProperty("alpha", 0.5);
            waypoint.addProperty("shouldRender", true);
            waypoints.add(waypoint);
        }
        group.add("waypoints", waypoints);

        JsonArray root = new JsonArray();
        root.add(group);
        return "[Skyblocker-Waypoint-Data-V1]"
                + Base64.getEncoder().encodeToString(gzip(root.toString()));
    }

    public static List<BlockPos> decode(String data) {
        String trimmed = data == null ? "" : data.trim();
        if (trimmed.startsWith("WP:")) {
            return WaypointerV8Codec.decode(trimmed.substring(3));
        }
        if (trimmed.startsWith("[Skyblocker-Waypoint-Data-V1]")) {
            return decodeSkyblocker(trimmed.substring("[Skyblocker-Waypoint-Data-V1]".length()));
        }
        if (trimmed.startsWith("[")) {
            return decodeSkyHanni(trimmed);
        }
        throw new IllegalArgumentException("Unrecognized route format");
    }

    private static List<BlockPos> decodeSkyHanni(String json) {
        List<BlockPos> positions = new ArrayList<>();
        JsonArray waypoints = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement element : waypoints) {
            JsonObject waypoint = element.getAsJsonObject();
            positions.add(new BlockPos(
                    waypoint.get("x").getAsInt(),
                    waypoint.get("y").getAsInt(),
                    waypoint.get("z").getAsInt()));
        }
        return positions;
    }

    private static List<BlockPos> decodeSkyblocker(String base64) {
        List<BlockPos> positions = new ArrayList<>();
        String json = gunzip(Base64.getDecoder().decode(base64));
        JsonArray groups = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement groupElement : groups) {
            JsonObject group = groupElement.getAsJsonObject();
            if (!group.has("waypoints")) continue;
            for (JsonElement waypointElement : group.getAsJsonArray("waypoints")) {
                JsonArray pos = waypointElement.getAsJsonObject().getAsJsonArray("pos");
                positions.add(new BlockPos(
                        pos.get(0).getAsInt(),
                        pos.get(1).getAsInt(),
                        pos.get(2).getAsInt()));
            }
        }
        return positions;
    }

    private static String gunzip(byte[] input) {
        try (GZIPInputStream gzip = new GZIPInputStream(new java.io.ByteArrayInputStream(input))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalArgumentException("Could not decompress Skyblocker route", error);
        }
    }

    private static byte[] gzip(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(input.getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            throw new IllegalStateException("Skyblocker route export failed", error);
        }
        return output.toByteArray();
    }
}
