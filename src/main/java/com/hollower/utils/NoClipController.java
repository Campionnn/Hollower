package com.hollower.utils;

import com.hollower.Hollower;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class NoClipController {
    private static final double NORMAL_SPEED = 0.32;
    private static final double FAST_SPEED = 0.75;

    private static LocalPlayer trackedClientPlayer;
    private static boolean savedClientNoPhysics;

    private static ServerPlayer trackedServerPlayer;
    private static MinecraftServer trackedServer;
    private static boolean savedServerNoPhysics;

    private static volatile MinecraftServer requestedServer;
    private static volatile UUID requestedPlayerId;

    private NoClipController() {
    }

    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(NoClipController::tickClient);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (server == trackedServer) restoreServerPlayer();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Hollower.noClip = false;
            reset();
        });
    }

    public static boolean isAvailable(Minecraft client) {
        return client.hasSingleplayerServer()
                && client.getSingleplayerServer() != null
                && client.level != null
                && client.player != null;
    }

    public static boolean canDisable(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return true;
        // Hidden blocks are passable in their own right now, so noCollision already accounts for them.
        return client.level.noCollision(player);
    }

    private static void tickClient(Minecraft client) {
        LocalPlayer player = client.player;
        if (!Hollower.noClip) {
            resetClientPlayer();
            requestedServer = null;
            requestedPlayerId = null;
            return;
        }
        if (!isAvailable(client) || player == null || !player.isAlive()) {
            Hollower.noClip = false;
            reset();
            return;
        }

        if (trackedClientPlayer != player) {
            resetClientPlayer();
            trackedClientPlayer = player;
            savedClientNoPhysics = player.noPhysics;
        }

        requestedServer = client.getSingleplayerServer();
        requestedPlayerId = player.getUUID();
        applyNoClip(player);
    }

    public static void onPlayerTick(Player player) {
        if (player == trackedClientPlayer) {
            if (Hollower.noClip) applyNoClip(player);
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        MinecraftServer server = serverPlayer.level().getServer();
        UUID playerId = requestedPlayerId;
        boolean shouldEnable = Hollower.noClip
                && server == requestedServer
                && playerId != null
                && playerId.equals(serverPlayer.getUUID());
        if (!shouldEnable || !serverPlayer.isAlive()) {
            if (serverPlayer == trackedServerPlayer) restoreServerPlayer();
            return;
        }

        if (trackedServerPlayer != serverPlayer) {
            restoreServerPlayer();
            trackedServer = server;
            trackedServerPlayer = serverPlayer;
            savedServerNoPhysics = serverPlayer.noPhysics;
        }
        applyNoClip(serverPlayer);
    }

    public static boolean isNoClipping(Entity entity) {
        if (!Hollower.noClip) return false;
        if (entity == trackedClientPlayer) return true;
        if (!(entity instanceof ServerPlayer serverPlayer)) return false;

        UUID playerId = requestedPlayerId;
        return serverPlayer.level().getServer() == requestedServer
                && playerId != null
                && playerId.equals(serverPlayer.getUUID());
    }

    public static boolean handleTravel(LivingEntity entity) {
        LocalPlayer player = trackedClientPlayer;
        if (!Hollower.noClip || player == null || entity != player) return false;

        Minecraft client = Minecraft.getInstance();
        double forward = (client.options.keyUp.isDown() ? 1.0 : 0.0)
                - (client.options.keyDown.isDown() ? 1.0 : 0.0);
        double left = (client.options.keyLeft.isDown() ? 1.0 : 0.0)
                - (client.options.keyRight.isDown() ? 1.0 : 0.0);
        double vertical = (client.options.keyJump.isDown() ? 1.0 : 0.0)
                - (client.options.keyShift.isDown() ? 1.0 : 0.0);

        double yaw = Math.toRadians(player.getYRot());
        double x = -Math.sin(yaw) * forward + Math.cos(yaw) * left;
        double z = Math.cos(yaw) * forward + Math.sin(yaw) * left;
        double horizontalLength = Math.sqrt(x * x + z * z);
        if (horizontalLength > 1.0) {
            x /= horizontalLength;
            z /= horizontalLength;
        }

        double speed = client.options.keySprint.isDown() ? FAST_SPEED : NORMAL_SPEED;
        Vec3 movement = new Vec3(x * speed, vertical * speed, z * speed);
        applyNoClip(player);
        player.setDeltaMovement(Vec3.ZERO);
        player.move(MoverType.SELF, movement);
        return true;
    }

    private static void applyNoClip(Player player) {
        player.noPhysics = true;
        player.setOnGround(false);
        player.resetFallDistance();
    }

    public static void reset() {
        resetClientPlayer();
        requestedServer = null;
        requestedPlayerId = null;
    }

    private static void resetClientPlayer() {
        if (trackedClientPlayer != null) {
            trackedClientPlayer.noPhysics = savedClientNoPhysics || trackedClientPlayer.isSpectator();
            trackedClientPlayer.setDeltaMovement(Vec3.ZERO);
            trackedClientPlayer.resetFallDistance();
        }
        trackedClientPlayer = null;
        savedClientNoPhysics = false;
    }

    private static void restoreServerPlayer() {
        if (trackedServerPlayer != null) {
            trackedServerPlayer.noPhysics = savedServerNoPhysics || trackedServerPlayer.isSpectator();
            trackedServerPlayer.setDeltaMovement(Vec3.ZERO);
            trackedServerPlayer.resetFallDistance();
        }
        trackedServerPlayer = null;
        trackedServer = null;
        savedServerNoPhysics = false;
    }
}
