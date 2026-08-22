package com.hollower.utils;

import com.hollower.Hollower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class RouteUtils {
    private RouteUtils() {
    }

    public static void addPosition(BlockPos pos) {
        if (pos == null || Hollower.positions.contains(pos)) return;
        if (Hollower.selected == null) {
            Hollower.positions.add(pos);
        } else {
            Hollower.positions.add(Hollower.positions.indexOf(Hollower.selected) + 1, pos);
        }
    }

    public static void removePosition(BlockPos pos) {
        if (pos == null) return;
        if (pos.equals(Hollower.selected)) Hollower.selected = null;
        Hollower.positions.remove(pos);
    }

    public static void selectPosition(BlockPos pos) {
        Hollower.selected = pos;
    }

    public static void swapPositions(BlockPos pos) {
        if (pos == null || Hollower.selected == null || pos.equals(Hollower.selected)) return;
        int selectedIndex = Hollower.positions.indexOf(Hollower.selected);
        int positionIndex = Hollower.positions.indexOf(pos);
        if (selectedIndex < 0 || positionIndex < 0) return;

        Hollower.positions.set(selectedIndex, pos);
        Hollower.positions.set(positionIndex, Hollower.selected);
        Hollower.selected = pos;
    }

    public static BlockPos getNodeRaycast(List<BlockPos> positions, int maxReach) {
        Minecraft client = Minecraft.getInstance();
        Entity camera = client.getCameraEntity();
        if (camera == null) return null;

        Vec3 cameraPos = camera.getEyePosition();
        Vec3 look = camera.getLookAngle();
        double inverseX = safeInverse(look.x);
        double inverseY = safeInverse(look.y);
        double inverseZ = safeInverse(look.z);
        double minDistance = Double.MAX_VALUE;
        BlockPos minPos = null;

        for (BlockPos pos : positions) {
            Vec3 min = Vec3.atLowerCornerOf(pos);
            Vec3 max = min.add(1.0, 1.0, 1.0);
            double tx1 = (min.x - cameraPos.x) * inverseX;
            double tx2 = (max.x - cameraPos.x) * inverseX;
            double tMin = Math.min(tx1, tx2);
            double tMax = Math.max(tx1, tx2);

            double ty1 = (min.y - cameraPos.y) * inverseY;
            double ty2 = (max.y - cameraPos.y) * inverseY;
            tMin = Math.max(tMin, Math.min(ty1, ty2));
            tMax = Math.min(tMax, Math.max(ty1, ty2));

            double tz1 = (min.z - cameraPos.z) * inverseZ;
            double tz2 = (max.z - cameraPos.z) * inverseZ;
            tMin = Math.max(tMin, Math.min(tz1, tz2));
            tMax = Math.min(tMax, Math.max(tz1, tz2));
            if (tMax < 0 || tMin > tMax) continue;

            double distance = cameraPos.distanceTo(min);
            if (distance <= maxReach && distance < minDistance) {
                minDistance = distance;
                minPos = pos;
            }
        }
        return minPos;
    }

    public static BlockPos getNodeRaycast() {
        return getNodeRaycast(Hollower.positions, Hollower.maxReach);
    }

    public static BlockPos getRaycast(int maxReach) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) return null;
        HitResult hit = camera.pick(maxReach, 1.0f, false);
        return hit.getType() == HitResult.Type.BLOCK
                ? ((BlockHitResult) hit).getBlockPos()
                : null;
    }

    public static BlockPos getRaycast() {
        return getRaycast(Hollower.maxReach);
    }

    public static float getDistance(BlockPos first, BlockPos second) {
        return (float) Math.sqrt(
                Math.pow(first.getX() - second.getX(), 2)
                        + Math.pow(first.getY() - second.getY(), 2)
                        + Math.pow(first.getZ() - second.getZ(), 2));
    }

    public static boolean copyRouteToClipboard(RouteExportCodec.Target target) {
        if (Hollower.positions.isEmpty()) {
            Hollower.sendChatMessage("§cAdd at least one route node before exporting");
            return false;
        }
        if (target == RouteExportCodec.Target.WAYPOINTER
                && Hollower.positions.size() > WaypointerV8Codec.MAX_WAYPOINTS) {
            Hollower.sendChatMessage("§cWaypointer exports support up to "
                    + WaypointerV8Codec.MAX_WAYPOINTS + " nodes");
            return false;
        }
        try {
            Hollower.copyToClipboard(RouteExportCodec.encode(Hollower.positions, target));
            Hollower.sendChatMessage(target.displayName() + " route copied to clipboard");
            return true;
        } catch (RuntimeException error) {
            Hollower.LOGGER.error("Could not export route for {}", target.displayName(), error);
            Hollower.sendChatMessage("§cFailed to export route for " + target.displayName());
            return false;
        }
    }

    public static void importRouteFromClipboard() {
        Hollower.positions.clear();
        Hollower.selected = null;
        try {
            String[] nodes = Hollower.getClipboard().split("},");
            for (String node : nodes) {
                String[] parts = node.split(",");
                int x = Integer.parseInt(parts[0].split(":")[1]);
                int y = Integer.parseInt(parts[1].split(":")[1]);
                int z = Integer.parseInt(parts[2].split(":")[1]);
                Hollower.positions.add(new BlockPos(x, y, z));
            }
            Hollower.sendChatMessage("Route imported from clipboard");
        } catch (RuntimeException error) {
            Hollower.positions.clear();
            Hollower.sendChatMessage("§cFailed to import route from clipboard");
        }
    }

    public static void clearRoute() {
        Hollower.positions.clear();
        Hollower.selected = null;
        Hollower.sendChatMessage("Route cleared");
    }

    public static void setBlocksInRoute() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return;
        for (BlockPos pos : Hollower.positions) {
            Hollower.lastCommands.add("Changed");
            client.getConnection().sendCommand(
                    "setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " minecraft:bedrock");
        }
        Hollower.sendChatMessage("Blocks set in route");
    }

    private static double safeInverse(double value) {
        return Math.abs(value) < 1.0E-8 ? Math.copySign(Double.POSITIVE_INFINITY, value) : 1.0 / value;
    }
}
