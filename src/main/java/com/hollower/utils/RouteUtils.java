package com.hollower.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class RouteUtils {
    /**
     * Raycasts to the given positions and returns the position of the closest block hit.
     *
     * @param positions the positions to raycast
     * @return the position of the closest block hit
     */
    public static BlockPos getNodeRaycast(List<BlockPos> positions) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        double yawRadians = Math.toRadians(camera.getYaw() + 90);
        double pitchRadians = Math.toRadians(camera.getPitch() * -1);

        double dirX = 1.0f / (Math.cos(yawRadians) * Math.cos(pitchRadians));
        double dirY = 1.0f / (Math.sin(pitchRadians));
        double dirZ = 1.0f / (Math.sin(yawRadians) * Math.cos(pitchRadians));

        double minDistance = Double.MAX_VALUE;
        BlockPos minPos = null;

        for (BlockPos pos : positions) {
            Vec3d A = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            Vec3d B = new Vec3d(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).multiply(1.0, 1.0, 1.0);

            double tx1 = (A.x - cameraPos.x) * dirX;
            double tx2 = (B.x - cameraPos.x) * dirX;

            double tmin = Math.min(tx1, tx2);
            double tmax = Math.max(tx1, tx2);

            double ty1 = (A.y - cameraPos.y) * dirY;
            double ty2 = (B.y - cameraPos.y) * dirY;

            tmin = Math.max(tmin, Math.min(ty1, ty2));
            tmax = Math.min(tmax, Math.max(ty1, ty2));

            double tz1 = (A.z - cameraPos.z) * dirZ;
            double tz2 = (B.z - cameraPos.z) * dirZ;

            tmin = Math.max(tmin, Math.min(tz1, tz2));
            tmax = Math.min(tmax, Math.max(tz1, tz2));

            if (tmax < 0) {
                continue;
            }

            if (tmin > tmax) {
                continue;
            }

            double distance = cameraPos.distanceTo(new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
            if (distance < minDistance) {
                minDistance = distance;
                minPos = pos;
            }
        }
        return minPos;
    }
}
