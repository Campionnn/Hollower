package com.hollower.tweaks;

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
        if (Hollower.keysToggle.get(Hollower.toggleRenderKey)) {
            return Hollower.renderBlacklist.get(chunkHash).containsKey(blockHash);
        }
        return false;
    }

    public static void reloadRender() {
        MinecraftClient.getInstance().execute(RenderTweaks::reloadRenderInterval);
    }

    public static void refreshRender() {
        if (Hollower.keysToggle.get(Hollower.toggleRenderKey)) {
            showAllBlocks();
            hideAllBlocks();
        }
        else {
            Hollower.renderBlacklist.clear();
            Hollower.renderBlacklistState.clear();
        }
    }

    public static void reloadRenderInterval() {
        if (Hollower.keysToggle.get(Hollower.toggleRenderKey)) {
            hideAllBlocks();
        }
        else {
            showAllBlocks();
        }

    }

    private static void hideAllBlocks() {
        RenderTweaks.findAndAddBlocks();
        for (Map.Entry<Long, ConcurrentHashMap<Long, BlockPos>> entry : Hollower.renderBlacklist.entrySet()) {
            ChunkPos chunkPos = new ChunkPos(entry.getKey());
            hideAllBlocksChunk(chunkPos.x, chunkPos.z);
        }
    }

    public static void hideAllBlocksChunk(int cx, int cz) {
        World world = MinecraftClient.getInstance().world;
        if (world != null) {
            WorldChunk chunk = world.getChunk(cx, cz);
            ConcurrentHashMap<Long, BlockPos> blocks = Hollower.renderBlacklist.get(chunkToLong(cx, cz));
            if (blocks != null) {
                for (BlockPos pos : blocks.values()) {
                    hideRenderAtPos(chunk, pos, world);
                }
            }
        }
    }

    public static void hideRenderAtPos(WorldChunk chunk, BlockPos pos, World world) {
        BlockPos realPos = getRealPos(chunk.getPos(), pos);
        BlockState state = chunk.getBlockState(realPos);
        if (!state.isAir()) {
            world.setBlockState(realPos, Blocks.AIR.getDefaultState(), Block.FORCE_STATE | PASSTHROUGH);
            setFakeBlockState(realPos, state);
        }
    }

    private static void showAllBlocks() {
        World world = MinecraftClient.getInstance().world;
        if (world != null) {
            for (Map.Entry<Long, ConcurrentHashMap<Long, BlockPos>> entry : Hollower.renderBlacklist.entrySet()) {
                ChunkPos chunkPos = new ChunkPos(entry.getKey());
                ConcurrentHashMap<Long, BlockPos> chunk = entry.getValue();
                for (BlockPos pos : chunk.values()) {
                    showRenderAtPos(chunkPos, pos, world);
                }
            }
        }
    }

    public static void showRenderAtPos(ChunkPos chunkPos, BlockPos pos, World world) {
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

    public static void findAndAddBlocks(ConcurrentHashMap<Long, ConcurrentHashMap<Long, BlockPos>> renderBlacklist) {
        for (int cx = center.x - renderDistance; cx <= center.x + renderDistance; cx++) {
            for (int cz = center.z - renderDistance; cz <= center.z + renderDistance; cz++) {
                if (Math.sqrt(Math.pow(cx - center.x, 2) + Math.pow(cz - center.z, 2)) < renderDistance+1) {
                    long chunkHash = ChunkPos.toLong(cx, cz);
                    if (!renderBlacklist.containsKey(chunkHash)) {
                        renderBlacklist.put(chunkHash, new ConcurrentHashMap<>());
                    }
                    findAndAddBlocksChunk(cx, cz);
                }
            }
        }
    }

    public static void findAndAddBlocks() {
        findAndAddBlocks(Hollower.renderBlacklist);
    }

    public static void findAndAddBlocksChunk(int cx, int cz, ConcurrentHashMap<Long, BlockPos> renderBlacklistChunk, ConcurrentHashMap<Integer, String> renderBlacklistID) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null) return;
        WorldChunk chunk = world.getChunk(cx, cz);
        if (chunk == null || chunk.isEmpty()) return;
        processSections(chunk, renderBlacklistChunk, renderBlacklistID);
    }

    public static void findAndAddBlocksChunk(int cx, int cz) {
        findAndAddBlocksChunk(cx, cz, Hollower.renderBlacklist.get(ChunkPos.toLong(cx, cz)), Hollower.renderBlacklistID);
    }

    private static void processSections(WorldChunk chunk, ConcurrentHashMap<Long, BlockPos> renderBlacklistChunk, ConcurrentHashMap<Integer, String> renderBlacklistID) {
        ChunkSection[] sections = chunk.getSectionArray();
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section.isEmpty()) continue;
            int startY = chunk.sectionIndexToCoord(i) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = new BlockPos(x, y + startY, z);
                        BlockState state = chunk.getBlockState(pos);
                        if (!state.isAir() && renderBlacklistID.containsKey(state.getBlock().getTranslationKey().hashCode())) {
                            renderBlacklistChunk.put(pos.asLong(), pos);
                        }
                    }
                }
            }
        }
    }
}
