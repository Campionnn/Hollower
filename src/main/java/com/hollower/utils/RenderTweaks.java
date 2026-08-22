package com.hollower.utils;

import com.hollower.Hollower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class RenderTweaks {
    public static final int PASSTHROUGH = 1024;
    public static int renderDistance = 16;
    public static ChunkPos center;
    private static Level trackedLevel;

    private RenderTweaks() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            trackLevel(client.level);
            if (client.player == null) return;
            BlockPos playerPos = client.player.blockPosition();
            center = new ChunkPos(playerPos.getX() >> 4, playerPos.getZ() >> 4);
            renderDistance = client.options.getEffectiveRenderDistance();
        });
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            trackLevel(level);
            if (!Hollower.renderToggle) return;
            long chunkHash = chunkToLong(chunk.getPos().getMinBlockX() >> 4, chunk.getPos().getMinBlockZ() >> 4);
            ConcurrentHashMap<Long, BlockPos> chunkMap = new ConcurrentHashMap<>();
            Hollower.renderBlacklist.put(chunkHash, chunkMap);
            findBlocksChunk(chunk, chunkMap, Hollower.renderBlacklistID);
            hideBlocksChunk(chunk);
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            if (level == trackedLevel) removeChunkState(chunk);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            clearWorldState();
            trackedLevel = client.level;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Hollower.renderToggle = false;
            clearWorldState();
            trackedLevel = null;
        });
    }

    private static void trackLevel(Level level) {
        if (trackedLevel == level) return;
        clearWorldState();
        trackedLevel = level;
    }

    private static void clearWorldState() {
        Hollower.renderBlacklist.clear();
        Hollower.renderBlacklistState.clear();
        center = null;
    }

    private static void removeChunkState(LevelChunk chunk) {
        int chunkX = chunk.getPos().getMinBlockX() >> 4;
        int chunkZ = chunk.getPos().getMinBlockZ() >> 4;
        ConcurrentHashMap<Long, BlockPos> removed =
                Hollower.renderBlacklist.remove(chunkToLong(chunkX, chunkZ));
        if (removed == null) return;

        for (BlockPos localPos : removed.values()) {
            Hollower.renderBlacklistState.remove(getRealPos(chunk.getPos(), localPos).asLong());
        }
    }

    public static boolean shouldHideBlock(long chunkHash, long blockHash) {
        if (!Hollower.renderToggle) return false;
        ConcurrentHashMap<Long, BlockPos> chunkMap = Hollower.renderBlacklist.get(chunkHash);
        return chunkMap != null && chunkMap.containsKey(blockHash);
    }

    public static void reloadRender() {
        Minecraft.getInstance().execute(RenderTweaks::reloadRenderInterval);
    }

    public static void refreshRender() {
        if (Hollower.renderToggle) {
            showBlocks();
            hideBlocks();
        } else {
            Hollower.renderBlacklist.clear();
            Hollower.renderBlacklistState.clear();
        }
    }

    public static void reloadRenderInterval() {
        if (Hollower.renderToggle) {
            hideBlocks();
        } else {
            showBlocks();
        }
    }

    private static void hideBlocks() {
        findBlocks();
        for (Map.Entry<Long, ConcurrentHashMap<Long, BlockPos>> entry : Hollower.renderBlacklist.entrySet()) {
            hideBlocksChunk(ChunkPos.getX(entry.getKey()), ChunkPos.getZ(entry.getKey()));
        }
    }

    public static void hideBlocksChunk(int chunkX, int chunkZ) {
        Level level = Minecraft.getInstance().level;
        if (level != null) hideBlocksChunk(level.getChunk(chunkX, chunkZ));
    }

    private static void hideBlocksChunk(LevelChunk chunk) {
        Level level = chunk.getLevel();
        int chunkX = chunk.getPos().getMinBlockX() >> 4;
        int chunkZ = chunk.getPos().getMinBlockZ() >> 4;
        ConcurrentHashMap<Long, BlockPos> blocks = Hollower.renderBlacklist.get(chunkToLong(chunkX, chunkZ));
        if (blocks == null) return;

        for (BlockPos pos : blocks.values()) {
            BlockPos realPos = getRealPos(chunk.getPos(), pos);
            BlockState state = chunk.getBlockState(realPos);
            if (!state.isAir()) {
                level.setBlock(realPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE | PASSTHROUGH);
                setFakeBlockState(realPos, state);
            }
        }
    }

    private static void showBlocks() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        for (Map.Entry<Long, ConcurrentHashMap<Long, BlockPos>> entry : Hollower.renderBlacklist.entrySet()) {
            ChunkPos chunkPos = new ChunkPos(ChunkPos.getX(entry.getKey()), ChunkPos.getZ(entry.getKey()));
            for (BlockPos pos : entry.getValue().values()) {
                showBlockAtPos(chunkPos, pos, level);
            }
        }
    }

    public static void showBlockAtPos(ChunkPos chunkPos, BlockPos pos, Level level) {
        BlockPos realPos = getRealPos(chunkPos, pos);
        if (!level.getBlockState(realPos).isAir()) return;

        BlockState originalState = getFakeBlockState(realPos);
        if (originalState == null) return;

        level.setBlock(realPos, originalState, Block.UPDATE_KNOWN_SHAPE | PASSTHROUGH);
        ConcurrentHashMap<Long, BlockPos> chunkMap = Hollower.renderBlacklist.get(
                chunkToLong(chunkPos.getMinBlockX() >> 4, chunkPos.getMinBlockZ() >> 4)
        );
        if (chunkMap != null) chunkMap.remove(pos.asLong());
        Hollower.renderBlacklistState.remove(realPos.asLong());
    }

    private static BlockPos getRealPos(ChunkPos chunkPos, BlockPos pos) {
        return new BlockPos(chunkPos.getMinBlockX() + pos.getX(), pos.getY(), chunkPos.getMinBlockZ() + pos.getZ());
    }

    public static long chunkToLong(int chunkX, int chunkZ) {
        return (long) chunkX & 0xFFFFFFFFL | ((long) chunkZ & 0xFFFFFFFFL) << 32;
    }

    public static void setFakeBlockState(BlockPos pos, BlockState state) {
        Hollower.renderBlacklistState.put(pos.asLong(), state);
    }

    public static BlockState getFakeBlockState(BlockPos pos) {
        return Hollower.renderBlacklistState.get(pos.asLong());
    }

    public static void findBlocks(ConcurrentHashMap<Long, ConcurrentHashMap<Long, BlockPos>> renderBlacklist) {
        if (center == null) return;

        int centerX = center.getMinBlockX() >> 4;
        int centerZ = center.getMinBlockZ() >> 4;
        int padding = renderDistance + 2;
        for (int chunkX = centerX - padding; chunkX <= centerX + padding; chunkX++) {
            for (int chunkZ = centerZ - padding; chunkZ <= centerZ + padding; chunkZ++) {
                long chunkHash = chunkToLong(chunkX, chunkZ);
                renderBlacklist.computeIfAbsent(chunkHash, unused -> new ConcurrentHashMap<>());
                findBlocksChunk(chunkX, chunkZ);
            }
        }
    }

    public static void findBlocks() {
        findBlocks(Hollower.renderBlacklist);
    }

    public static void findBlocksChunk(int chunkX, int chunkZ) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        long chunkHash = chunkToLong(chunkX, chunkZ);
        ConcurrentHashMap<Long, BlockPos> chunkMap =
                Hollower.renderBlacklist.computeIfAbsent(chunkHash, unused -> new ConcurrentHashMap<>());
        findBlocksChunk(level.getChunk(chunkX, chunkZ), chunkMap, Hollower.renderBlacklistID);
    }

    private static void findBlocksChunk(
            LevelChunk chunk,
            ConcurrentHashMap<Long, BlockPos> renderBlacklistChunk,
            ConcurrentHashMap<Integer, String> renderBlacklistIds
    ) {
        if (chunk.isEmpty()) return;

        LevelChunkSection[] sections = chunk.getSections();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir()) continue;

            int startY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos localPos = new BlockPos(x, startY + y, z);
                        BlockPos realPos = new BlockPos(startX + x, startY + y, startZ + z);
                        BlockState state = chunk.getBlockState(realPos);
                        if (!state.isAir()
                                && renderBlacklistIds.containsKey(state.getBlock().getDescriptionId().hashCode())) {
                            renderBlacklistChunk.put(localPos.asLong(), localPos);
                        }
                    }
                }
            }
        }
    }
}
