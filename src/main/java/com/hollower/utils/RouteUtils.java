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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
        orderBeforeOptimize = null;
        try {
            List<BlockPos> imported = RouteExportCodec.decode(Hollower.getClipboard());
            if (imported.isEmpty()) throw new IllegalArgumentException("No waypoints found");
            Hollower.positions.addAll(imported);
            Hollower.sendChatMessage("Route imported from clipboard");
        } catch (RuntimeException error) {
            Hollower.positions.clear();
            Hollower.LOGGER.error("Could not import route from clipboard", error);
            Hollower.sendChatMessage("§cFailed to import route from clipboard");
        }
    }

    public static void loadRoute(RouteStorage.SavedRoute route) {
        Hollower.positions.clear();
        Hollower.selected = null;
        orderBeforeOptimize = null;
        Hollower.positions.addAll(route.positions());
        Hollower.sendChatMessage("Route '" + route.name() + "' loaded");
    }

    public static void clearRoute() {
        Hollower.positions.clear();
        Hollower.selected = null;
        orderBeforeOptimize = null;
        Hollower.sendChatMessage("Route cleared");
    }

    // ---------------------------------------------------------------- optimizing

    private static final AtomicBoolean optimizing = new AtomicBoolean();
    // The order the route had before the last optimize that actually changed something, so the
    // player can get their hand-tuned ordering back.
    private static List<BlockPos> orderBeforeOptimize;

    public static boolean isOptimizing() {
        return optimizing.get();
    }

    public static boolean canUndoOptimize() {
        return orderBeforeOptimize != null;
    }

    public static void undoOptimize() {
        if (orderBeforeOptimize == null) return;
        Hollower.positions.clear();
        Hollower.positions.addAll(orderBeforeOptimize);
        orderBeforeOptimize = null;
        Hollower.sendChatMessage("Route order restored");
    }

    // Reorders the route in place. The search runs on a worker thread so a large route can't stall
    // the client, and the result is applied back on the client thread because Hollower.positions is
    // a plain list that RenderUtils walks every frame. `onDone` runs on the client thread too, with
    // the result, or with null if nothing was applied.
    public static void optimizeRoute(RouteOptimizer.Options options,
                                     Consumer<RouteOptimizer.Result> onDone) {
        List<BlockPos> snapshot = List.copyOf(Hollower.positions);
        if (snapshot.size() < 4) {
            Hollower.sendChatMessage("§cAdd at least four route nodes before optimizing");
            onDone.accept(null);
            return;
        }
        if (snapshot.size() > RouteOptimizer.MAX_NODES) {
            Hollower.sendChatMessage("§cRoutes longer than " + RouteOptimizer.MAX_NODES
                    + " nodes are too large to optimize");
            onDone.accept(null);
            return;
        }
        if (!optimizing.compareAndSet(false, true)) {
            Hollower.sendChatMessage("§cAlready optimizing this route");
            return;
        }

        Thread worker = new Thread(() -> {
            RouteOptimizer.Result result;
            try {
                result = RouteOptimizer.optimize(snapshot, options);
            } catch (RuntimeException error) {
                Hollower.LOGGER.error("Route optimization failed", error);
                Minecraft.getInstance().execute(() -> {
                    optimizing.set(false);
                    Hollower.sendChatMessage("§cFailed to optimize route");
                    onDone.accept(null);
                });
                return;
            }
            Minecraft.getInstance().execute(() -> applyOptimized(snapshot, result, onDone));
        }, "Hollower Route Optimizer");
        worker.setDaemon(true);
        worker.start();
    }

    private static void applyOptimized(List<BlockPos> snapshot, RouteOptimizer.Result result,
                                       Consumer<RouteOptimizer.Result> onDone) {
        optimizing.set(false);

        // The player can keep editing while the search runs; a reordering of a route that no longer
        // exists would silently resurrect deleted nodes.
        if (!Hollower.positions.equals(snapshot)) {
            Hollower.sendChatMessage("§cRoute changed while optimizing, so the new order was discarded");
            onDone.accept(null);
            return;
        }
        if (!result.changed()) {
            Hollower.sendChatMessage("Route order is already optimal for these settings");
            onDone.accept(result);
            return;
        }

        orderBeforeOptimize = snapshot;
        Hollower.positions.clear();
        Hollower.positions.addAll(result.order());
        // Optimizing is a permutation, so whatever was selected is still in the route.
        Hollower.sendChatMessage(String.format("Route optimized: %.0f → %.0f (-%.1f%%)",
                result.costBefore(), result.costAfter(), result.improvementPercent()));
        onDone.accept(result);
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
