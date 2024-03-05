package com.hollower.mixin;

import com.hollower.Hollower;
import com.hollower.utils.RenderTweaks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ConcurrentHashMap;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {
    @Shadow
    private ClientWorld world;
    @Final
    @Shadow
    private MinecraftClient client;

    @Shadow
    private int chunkLoadDistance;

    @Inject(method = "onGameJoin", at=@At(value = "NEW", target="net/minecraft/client/world/ClientWorld"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        RenderTweaks.renderDistance = chunkLoadDistance;
        Hollower.renderBlacklist.clear();
        Hollower.renderBlacklistState.clear();
        Hollower.window = client.getWindow().getHandle();
    }

    @Inject(method = "onChunkData", at=@At("RETURN"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        int cx = packet.getX();
        int cz = packet.getZ();

        if (!Hollower.renderToggle) return;
        WorldChunk worldChunk = this.world.getChunkManager().getWorldChunk(cx, cz);
        if (worldChunk == null) return;

        long chunkHash = ChunkPos.toLong(cx, cz);
        if (!Hollower.renderBlacklist.containsKey(chunkHash)) {
            Hollower.renderBlacklist.put(chunkHash, new ConcurrentHashMap<>());
        }
        RenderTweaks.findBlocksChunk(cx, cz);
        RenderTweaks.hideBlocksChunk(cx, cz);
    }

    @Inject(method = "onChunkLoadDistance",at=@At("RETURN"))
    private void onChunkLoadDistance(ChunkLoadDistanceS2CPacket packet, CallbackInfo ci) {
        RenderTweaks.renderDistance = packet.getDistance();
    }

    @Inject(method = "onChunkRenderDistanceCenter",at=@At("RETURN"))
    private void onChunkRenderDistanceCenter(ChunkRenderDistanceCenterS2CPacket packet, CallbackInfo ci) {
        RenderTweaks.center = new ChunkPos(packet.getChunkX(), packet.getChunkZ());
    }
}
