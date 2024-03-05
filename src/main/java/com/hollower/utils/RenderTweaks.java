package com.hollower.utils;

import com.hollower.Hollower;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RenderTweaks {
    public static final int PASSTHROUGH = 1024;
    public static int renderDistance = 16;
    public static ChunkPos center;

    public static boolean shouldHideBlock(long chunkHash, long blockHash) {
        if (Hollower.renderToggle) {
            return Hollower.renderBlacklist.get(chunkHash).containsKey(blockHash);
        }
        return false;
    }

    public static void reloadRender() {
        MinecraftClient.getInstance().execute(RenderTweaks::reloadRenderInterval);
    }

    public static void refreshRender() {
        if (Hollower.renderToggle) {
            showBlocks();
            hideBlocks();
        }
        else {
            Hollower.renderBlacklist.clear();
            Hollower.renderBlacklistState.clear();
        }
    }

    public static void reloadRenderInterval() {
        if (Hollower.renderToggle) {
            hideBlocks();
        }
        else {
            showBlocks();
        }
    }

    private static void hideBlocks() {
        RenderTweaks.findBlocks();
        for (Map.Entry<Long, ConcurrentHashMap<Long, BlockPos>> entry : Hollower.renderBlacklist.entrySet()) {
            ChunkPos chunkPos = new ChunkPos(entry.getKey());
            hideBlocksChunk(chunkPos.x, chunkPos.z);
        }
    }

    public static void hideBlocksChunk(int cx, int cz) {
        World world = MinecraftClient.getInstance().world;
        if (world != null) {
            WorldChunk chunk = world.getChunk(cx, cz);
            ConcurrentHashMap<Long, BlockPos> blocks = Hollower.renderBlacklist.get(chunkToLong(cx, cz));
            if (blocks != null) {
                for (BlockPos pos : blocks.values()) {
                    hideBlockAtPos(chunk, pos, world);
                }
            }
        }
    }

    public static void hideBlockAtPos(WorldChunk chunk, BlockPos pos, World world) {
        BlockPos realPos = getRealPos(chunk.getPos(), pos);
        BlockState state = chunk.getBlockState(realPos);
        if (!state.isAir()) {
            world.setBlockState(realPos, Blocks.AIR.getDefaultState(), Block.FORCE_STATE | PASSTHROUGH);
            setFakeBlockState(realPos, state);
        }
    }

    private static void showBlocks() {
        World world = MinecraftClient.getInstance().world;
        if (world != null) {
            for (Map.Entry<Long, ConcurrentHashMap<Long, BlockPos>> entry : Hollower.renderBlacklist.entrySet()) {
                ChunkPos chunkPos = new ChunkPos(entry.getKey());
                ConcurrentHashMap<Long, BlockPos> chunk = entry.getValue();
                for (BlockPos pos : chunk.values()) {
                    showBlockAtPos(chunkPos, pos, world);
                }
            }
        }
    }

    public static void showBlockAtPos(ChunkPos chunkPos, BlockPos pos, World world) {
        BlockPos realPos = getRealPos(chunkPos, pos);
        BlockState state = world.getBlockState(realPos);
        if (!state.isAir()) return;
        BlockState originalState = getFakeBlockState(realPos);
        if (originalState == null) return;
        world.setBlockState(realPos, originalState, Block.FORCE_STATE | PASSTHROUGH);
        Hollower.renderBlacklist.get(chunkToLong(chunkPos.x, chunkPos.z)).remove(pos.asLong());
        Hollower.renderBlacklistState.remove(realPos.asLong());
    }

    private static BlockPos getRealPos(ChunkPos chunkPos, BlockPos pos) {
        return new BlockPos(chunkPos.getStartX() + pos.getX(), pos.getY(), chunkPos.getStartZ() + pos.getZ());
    }

    public static long chunkToLong(int chunkX, int chunkZ) {
        return (long)chunkX & 0xFFFFFFFFL | ((long)chunkZ & 0xFFFFFFFFL) << 32;
    }

    public static void setFakeBlockState(BlockPos pos, BlockState state) {
        Hollower.renderBlacklistState.put(pos.asLong(), state);
    }

    public static BlockState getFakeBlockState(BlockPos pos) {
        return Hollower.renderBlacklistState.get(pos.asLong());
    }

    public static void findBlocks(ConcurrentHashMap<Long, ConcurrentHashMap<Long, BlockPos>> renderBlacklist) {
        for (int cx = center.x - (renderDistance+2); cx <= center.x + (renderDistance+2); cx++) {
            for (int cz = center.z - (renderDistance+2); cz <= center.z + (renderDistance+2); cz++) {
                long chunkHash = ChunkPos.toLong(cx, cz);
                if (!renderBlacklist.containsKey(chunkHash)) {
                    renderBlacklist.put(chunkHash, new ConcurrentHashMap<>());
                }
                findBlocksChunk(cx, cz);
            }
        }
    }

    public static void findBlocks() {
        findBlocks(Hollower.renderBlacklist);
    }

    public static void findBlocksChunk(int cx, int cz, ConcurrentHashMap<Long, BlockPos> renderBlacklistChunk, ConcurrentHashMap<Integer, String> renderBlacklistID) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null) return;
        WorldChunk chunk = world.getChunk(cx, cz);
        if (chunk == null || chunk.isEmpty()) return;
        findBlocksSections(chunk, renderBlacklistChunk, renderBlacklistID);
    }

    public static void findBlocksChunk(int cx, int cz) {
        findBlocksChunk(cx, cz, Hollower.renderBlacklist.get(ChunkPos.toLong(cx, cz)), Hollower.renderBlacklistID);
    }

    private static void findBlocksSections(WorldChunk chunk, ConcurrentHashMap<Long, BlockPos> renderBlacklistChunk, ConcurrentHashMap<Integer, String> renderBlacklistID) {
        ChunkSection[] sections = chunk.getSectionArray();
        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section.isEmpty()) continue;
            int startY = chunk.sectionIndexToCoord(i) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = new BlockPos(x, y + startY, z);
                        BlockPos realPos = new BlockPos(startX + x, y + startY, startZ + z);
                        BlockState state = chunk.getBlockState(realPos);
                        if (!state.isAir() && renderBlacklistID.containsKey(state.getBlock().getTranslationKey().hashCode())) {
                            renderBlacklistChunk.put(pos.asLong(), pos);
                        }
                    }
                }
            }
        }
    }
}
