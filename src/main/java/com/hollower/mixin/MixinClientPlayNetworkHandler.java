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
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    }

    @Inject(method = "onChunkData", at=@At("RETURN"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        int cx = packet.getX();
        int cz = packet.getZ();
        RenderTweaks.loadFakeChunk(cx, cz);

        if (!Hollower.keysToggle.get(Hollower.toggleRenderKey)) return;
        WorldChunk worldChunk = this.world.getChunkManager().getWorldChunk(cx, cz);
        if (worldChunk == null) return;

        if (!Hollower.renderBlacklistChunk.containsKey(ChunkPos.toLong(cx, cz))) {
            RenderTweaks.findAndAddBlocksChunk(cx, cz);
        }

        ChunkPos chunkPos = worldChunk.getPos();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        ChunkSection[] sections = worldChunk.getSectionArray();
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section == null || section.isEmpty()) continue;
            int startY = this.world.sectionIndexToCoord(i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        pos.set(x + chunkPos.getStartX(), y + startY, z + chunkPos.getStartZ());
                        if (RenderTweaks.shouldHideBlock(pos)) {
                            BlockState state = section.getBlockState(x, y, z);
                            worldChunk.setBlockState(pos, Blocks.AIR.getDefaultState(), false);
                            RenderTweaks.setFakeBlockState(pos, state);
                        }
                    }
                }
            }
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
