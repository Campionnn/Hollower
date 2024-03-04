package com.hollower.tweaks;

import com.hollower.Hollower;
import com.hollower.world.FakeWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RenderTweaks {
    public static final int PASSTHROUGH = 1024;
    private static FakeWorld fakeWorld = null;

    public static FakeWorld getFakeWorld() {
        return fakeWorld;
    }

    public static void resetWorld(DynamicRegistryManager registryManager, int loadDistance) {
        fakeWorld = new FakeWorld(registryManager, loadDistance);
    }

    public static void loadFakeChunk(int x, int z) {
        fakeWorld.getChunkManager().loadChunk(x, z);
    }

    public static void unloadFakeChunk(int x, int z) {
        fakeWorld.getChunkManager().unloadChunk(x, z);
    }

    public static boolean shouldHideBlock(long chunkHash, long blockHash) {
        if (Hollower.keysToggle.get(Hollower.toggleRenderKey)) {
            return Hollower.renderBlacklist.get(chunkHash).containsKey(blockHash);
        }
        return false;
    }

    public static boolean shouldHideBlock(ChunkPos chunkPos, BlockPos pos) {
        return shouldHideBlock(chunkPos.toLong(), pos.asLong());
    }

    public static void reloadSelective() {
        RenderTweaks.findAndAddBlocks();
        MinecraftClient.getInstance().execute(RenderTweaks::reloadSelectiveInternal);
    }

    public static void reloadSelectiveInternal() {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        if (Hollower.keysToggle.get(Hollower.toggleRenderKey)) {
            for (Map.Entry<Long, ConcurrentHashMap<Long, BlockPos>> entry : Hollower.renderBlacklist.entrySet()) {
                ChunkPos chunkPos = new ChunkPos(entry.getKey());
                ConcurrentHashMap<Long, BlockPos> chunk = entry.getValue();
                for (BlockPos pos : chunk.values()) {
                    hideRenderAtPos(chunkPos, pos, world);
                }
            }
        }
        else {
            for (Map.Entry<Long, ConcurrentHashMap<Long, BlockPos>> entry : Hollower.renderBlacklist.entrySet()) {
                ChunkPos chunkPos = new ChunkPos(entry.getKey());
                ConcurrentHashMap<Long, BlockPos> chunk = entry.getValue();
                for (BlockPos pos : chunk.values()) {
                    showRenderAtPos(chunkPos, pos, world);
                }
            }
        }
    }

    public static void hideRenderAtPos(ChunkPos chunkPos, BlockPos pos, World world) {
        BlockPos realPos = new BlockPos(pos.getX() + chunkPos.getStartX(), pos.getY(), pos.getZ() + chunkPos.getStartZ());
        BlockState state = world.getBlockState(realPos);
        if (!state.isAir()) {
            world.setBlockState(realPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.FORCE_STATE | PASSTHROUGH);
            setFakeBlockState(realPos, state);
        }
    }

    public static void showRenderAtPos(ChunkPos chunkPos, BlockPos pos, World world) {
        BlockPos realPos = new BlockPos(pos.getX() + chunkPos.getStartX(), pos.getY(), pos.getZ() + chunkPos.getStartZ());
        BlockState state = world.getBlockState(realPos);
        if (state.isAir()) {
            BlockState originalState = fakeWorld.getBlockState(realPos);
            if (!originalState.isAir()) {
                fakeWorld.setBlockState(realPos, Blocks.AIR.getDefaultState());
                world.setBlockState(realPos, originalState, Block.NOTIFY_ALL | Block.FORCE_STATE | PASSTHROUGH);
            }
        }
    }

    public static void setFakeBlockState(BlockPos pos, BlockState state) {
        fakeWorld.setBlockState(pos, state, 0);
    }

    public static void findAndAddBlocks(ConcurrentHashMap<Long, ConcurrentHashMap<Long, BlockPos>> renderBlacklist, ConcurrentHashMap<Integer, String> renderBlacklistID) {
        ChunkPos center = fakeWorld.getChunkManager().getChunkMapCenter();
        int radius = fakeWorld.getChunkManager().getRadius();
        for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
            for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
                if (Math.sqrt(Math.pow(cx - center.x, 2) + Math.pow(cz - center.z, 2)) < radius+1) {
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
        findAndAddBlocks(Hollower.renderBlacklist, Hollower.renderBlacklistID);
    }

    public static void findAndAddBlocksChunk(int cx, int cz, ConcurrentHashMap<Long, BlockPos> renderBlacklistChunk, ConcurrentHashMap<Integer, String> renderBlacklistID) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null) return;
        WorldChunk chunk = (WorldChunk) world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
        if (chunk == null || chunk.isEmpty()) return;
        processSections(chunk, renderBlacklistChunk, renderBlacklistID);
    }

    public static void findAndAddBlocksChunk(int cx, int cz) {
        findAndAddBlocksChunk(cx, cz, Hollower.renderBlacklist.get(ChunkPos.toLong(cx, cz)), Hollower.renderBlacklistID);
    }

    private static void processSections(WorldChunk chunk, ConcurrentHashMap<Long, BlockPos> renderBlacklistChunkRel, ConcurrentHashMap<Integer, String> renderBlacklistID) {
        ChunkSection[] sections = chunk.getSectionArray();
        int count = 0;
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
                            renderBlacklistChunkRel.put(pos.asLong(), pos);
                            count++;
                        }
                    }
                }
            }
        }
//        Hollower.LOGGER.info("Found " + count + " blocks in chunk " + chunk.getPos().x + " " + chunk.getPos().z);
    }
}
