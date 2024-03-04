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

    public static boolean shouldHideBlock(long key) {
        if (Hollower.keysToggle.get(Hollower.toggleRenderKey)) {
            return Hollower.renderBlacklist.containsKey(key);
        }
        return false;
    }

    public static boolean shouldHideBlock(BlockPos pos) {
        return shouldHideBlock(pos.asLong());
    }

    public static void reloadSelective() {
        MinecraftClient.getInstance().execute(RenderTweaks::reloadSelectiveInternal);
    }

    public static void reloadSelectiveInternal() {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        if (Hollower.keysToggle.get(Hollower.toggleRenderKey)) {
            for (BlockPos entry : Hollower.renderBlacklist.values()) {
                hideRenderAtPos(entry, world);
            }
        }
        else {
            for (BlockPos entry : Hollower.renderBlacklist.values()) {
                showRenderAtPos(entry, world);
            }
        }
    }

    public static void hideRenderAtPos(BlockPos pos, World world) {
        BlockState state = world.getBlockState(pos);
        if (!state.isAir()) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.FORCE_STATE | PASSTHROUGH);
            setFakeBlockState(pos, state);
        }
    }

    public static void showRenderAtPos(BlockPos pos, World world) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            BlockState originalState = fakeWorld.getBlockState(pos);
            if (!originalState.isAir()) {
                fakeWorld.setBlockState(pos, Blocks.AIR.getDefaultState());
                world.setBlockState(pos, originalState, Block.NOTIFY_ALL | Block.FORCE_STATE | PASSTHROUGH);
            }
        }
    }

    public static void setFakeBlockState(BlockPos pos, BlockState state) {
        fakeWorld.setBlockState(pos, state, 0);
    }

    public static void findAndAddBlocks(ConcurrentHashMap<Long, BlockPos> renderBlacklist, ConcurrentHashMap<Integer, String> renderBlacklistID, ConcurrentHashMap<Long, ChunkPos> renderBlacklistCacheChunk) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        ChunkPos center = fakeWorld.getChunkManager().getChunkMapCenter();
        int radius = fakeWorld.getChunkManager().getRadius();
        if (world == null) return;
        for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
            for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
                WorldChunk chunk = (WorldChunk) world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                ChunkPos chunkPos = chunk.getPos();
                processSections(renderBlacklist, renderBlacklistID, world, chunk, chunkPos);
                renderBlacklistCacheChunk.put(chunkPos.toLong(), chunkPos);
            }
        }
    }

    public static void findAndAddBlocks() {
        findAndAddBlocks(Hollower.renderBlacklist, Hollower.renderBlacklistID, Hollower.renderBlacklistChunk);
    }

    public static void findAndAddBlocksChunk(int cx, int cz, ConcurrentHashMap<Long, BlockPos> renderBlacklist, ConcurrentHashMap<Integer, String> renderBlacklistID, ConcurrentHashMap<Long, ChunkPos> renderBlacklistCacheChunk) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null) return;
        WorldChunk chunk = (WorldChunk) world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
        if (chunk == null) return;
        ChunkPos chunkPos = chunk.getPos();
        processSections(renderBlacklist, renderBlacklistID, world, chunk, chunkPos);
        renderBlacklistCacheChunk.put(chunkPos.toLong(), chunkPos);
    }

    public static void findAndAddBlocksChunk(int cx, int cz) {
        findAndAddBlocksChunk(cx, cz, Hollower.renderBlacklist, Hollower.renderBlacklistID, Hollower.renderBlacklistChunk);
    }

    private static void processSections(ConcurrentHashMap<Long, BlockPos> renderBlacklist, ConcurrentHashMap<Integer, String> renderBlacklistID, World world, WorldChunk chunk, ChunkPos chunkPos) {
        ChunkSection[] sections = chunk.getSectionArray();
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section.isEmpty()) continue;
            int startY = world.sectionIndexToCoord(i) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = new BlockPos(x + chunkPos.getStartX(), y + startY, z + chunkPos.getStartZ());
                        BlockState state = world.getBlockState(pos);
                        if (!state.isAir() && renderBlacklistID.containsKey(state.getBlock().getTranslationKey().hashCode())) {
                            renderBlacklist.put(pos.asLong(), pos);
                        }
                    }
                }
            }
        }
    }
}
