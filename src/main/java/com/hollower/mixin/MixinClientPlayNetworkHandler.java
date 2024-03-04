package com.hollower.mixin;

import com.hollower.Hollower;
import com.hollower.tweaks.RenderTweaks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {
    @Shadow
    private ClientWorld world;

    @Shadow
    private int chunkLoadDistance;

    @Shadow
    public abstract DynamicRegistryManager getRegistryManager();

    @Inject(method = "onPlayerRespawn", at=@At(value = "NEW", target="net/minecraft/client/world/ClientWorld"))
    private void onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        RenderTweaks.resetWorld(getRegistryManager(), chunkLoadDistance);
    }

    @Inject(method = "onGameJoin", at=@At(value = "NEW", target="net/minecraft/client/world/ClientWorld"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        RenderTweaks.resetWorld(getRegistryManager(),chunkLoadDistance);
        Hollower.renderBlacklist.clear();
        Hollower.renderBlacklistID.clear();
    }

    @Inject(method = "onChunkData", at=@At("RETURN"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        int cx = packet.getX();
        int cz = packet.getZ();
        RenderTweaks.loadFakeChunk(cx, cz);

        if (!Hollower.keysToggle.get(Hollower.toggleRenderKey)) return;
        WorldChunk worldChunk = this.world.getChunkManager().getWorldChunk(cx, cz);
        if (worldChunk == null) return;

        long chunkHash = ChunkPos.toLong(cx, cz);
        if (!Hollower.renderBlacklist.containsKey(chunkHash)) {
            Hollower.renderBlacklist.put(chunkHash, new ConcurrentHashMap<>());
            RenderTweaks.findAndAddBlocksChunk(cx, cz);
        }

        for (Map.Entry<Long, BlockPos> entry : Hollower.renderBlacklist.get(chunkHash).entrySet()) {
            BlockPos pos = entry.getValue();
            BlockPos realPos = new BlockPos((cx << 4) + pos.getX(), pos.getY(), (cz << 4) + pos.getZ());
            BlockState state = worldChunk.getBlockState(realPos);
            worldChunk.setBlockState(realPos, Blocks.AIR.getDefaultState(), false);
            RenderTweaks.setFakeBlockState(realPos, state);
        }
    }

    @Inject(method = "onUnloadChunk",at=@At("RETURN"))
    private void onUnloadChunk(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        int i = packet.getX();
        int j = packet.getZ();
        RenderTweaks.unloadFakeChunk(i,j);
    }

    @Inject(method = "onChunkLoadDistance",at=@At("RETURN"))
    private void onChunkLoadDistance(ChunkLoadDistanceS2CPacket packet, CallbackInfo ci) {
        RenderTweaks.getFakeWorld().getChunkManager().updateLoadDistance(packet.getDistance());
    }

    @Inject(method = "onChunkRenderDistanceCenter",at=@At("RETURN"))
    private void onChunkRenderDistanceCenter(ChunkRenderDistanceCenterS2CPacket packet, CallbackInfo ci) {
        RenderTweaks.getFakeWorld().getChunkManager().setChunkMapCenter(packet.getChunkX(), packet.getChunkZ());
    }
}
