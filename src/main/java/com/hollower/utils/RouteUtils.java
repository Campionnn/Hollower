package com.hollower.utils;

import com.hollower.Hollower;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class RouteUtils {
    public static void addPosition(BlockPos pos) {
        if (pos == null) return;

        if (!Hollower.positions.contains(pos)) {
            if (Hollower.selected != null) {
                Hollower.positions.add(Hollower.positions.indexOf(Hollower.selected) + 1, pos);
            } else {
                Hollower.positions.add(pos);
            }
        }
    }

    public static void removePosition(BlockPos pos) {
        if (pos == null) return;

        if (pos.equals(Hollower.selected)) {
            Hollower.selected = null;
        }
        Hollower.positions.remove(pos);
    }

    public static void selectPosition(BlockPos pos) {
        Hollower.selected = pos;
    }

    public static void swapPositions(BlockPos pos) {
        if (pos == null || Hollower.selected == null || pos == Hollower.selected) return;

        int indexSelected = Hollower.positions.indexOf(Hollower.selected);
        int indexPos = Hollower.positions.indexOf(pos);
        Hollower.positions.set(indexSelected, pos);
        Hollower.positions.set(indexPos, Hollower.selected);
        Hollower.selected = pos;
    }

    public static void clearPositions() {
        Hollower.positions.clear();
        Hollower.selected = null;
    }

    /**
     * Raycasts to the given positions using camera and returns the position of the closest block hit.
     *
     * @param positions the positions to raycast
     * @param maxReach the maximum reach of the raycast
     * @return the pos of the closest block hit in positions
     */
    public static BlockPos getNodeRaycast(List<BlockPos> positions, int maxReach) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        double yawRadians = Math.toRadians(camera.getYaw() + 90);
        double pitchRadians = Math.toRadians(camera.getPitch() * -1);

        double invX = 1.0f / (Math.cos(yawRadians) * Math.cos(pitchRadians));
        double invY = 1.0f / (Math.sin(pitchRadians));
        double invZ = 1.0f / (Math.sin(yawRadians) * Math.cos(pitchRadians));

        double minDistance = Double.MAX_VALUE;
        BlockPos minPos = null;

        for (BlockPos pos : positions) {
            Vec3d A = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            Vec3d B = new Vec3d(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).multiply(1.0, 1.0, 1.0);

            double tx1 = (A.x - cameraPos.x) * invX;
            double tx2 = (B.x - cameraPos.x) * invX;

            double tmin = Math.min(tx1, tx2);
            double tmax = Math.max(tx1, tx2);

            double ty1 = (A.y - cameraPos.y) * invY;
            double ty2 = (B.y - cameraPos.y) * invY;

            tmin = Math.max(tmin, Math.min(ty1, ty2));
            tmax = Math.min(tmax, Math.max(ty1, ty2));

            double tz1 = (A.z - cameraPos.z) * invZ;
            double tz2 = (B.z - cameraPos.z) * invZ;

            tmin = Math.max(tmin, Math.min(tz1, tz2));
            tmax = Math.min(tmax, Math.max(tz1, tz2));

            if (tmax < 0) {
                continue;
            }

            if (tmin > tmax) {
                continue;
            }

            double distance = cameraPos.distanceTo(new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
            if (distance > maxReach) {
                continue;
            }
            if (distance < minDistance) {
                minDistance = distance;
                minPos = pos;
            }
        }
        return minPos;
    }

    /**
     * Raycasts to the given positions using camera and returns the position of the closest block hit.
     *
     * @return the position of the closest block hit
     */
    public static BlockPos getNodeRaycast() {
        return getNodeRaycast(Hollower.positions, Hollower.maxReach);
    }

    /**
     * Raycasts to the given positions using camera and returns the position of the closest block hit.
     *
     * @param maxReach the maximum reach of the raycast
     * @return the position of the closest block hit
     */
    public static BlockPos getRaycast(int maxReach) {
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.cameraEntity != null;
        HitResult hit = client.cameraEntity.raycast(maxReach, 1.0f, false);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            return blockHit.getBlockPos();
        }
        return null;
    }

    /**
     * Raycasts to the given positions using camera and returns the position of the closest block hit.
     *
     * @return the position of the closest block hit
     */
    public static BlockPos getRaycast() {
        return getRaycast(Hollower.maxReach);
    }

    /**
     * Returns the distance between two positions.
     *
     * @param pos1 the first position
     * @param pos2 the second position
     * @return the distance between the two positions
     */
    public static float getDistance(BlockPos pos1, BlockPos pos2) {
        return (float) Math.sqrt(Math.pow(pos1.getX() - pos2.getX(), 2) + Math.pow(pos1.getY() - pos2.getY(), 2) + Math.pow(pos1.getZ() - pos2.getZ(), 2));
    }
}
