package com.hollower.tweaks;

import com.hollower.Hollower;
import com.hollower.world.FakeWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.BlockPos;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        if (Hollower.keysToggle.get(Hollower.toggleRenderKey)) {
            for (BlockPos entry : Hollower.renderBlacklist.values()) {
                if (!Hollower.renderBlacklistCache.containsKey(entry.asLong())) {
                    updateSelectiveAtPos(entry);
                }
            }
        }
        else {
            for (BlockPos entry : Hollower.renderBlacklistCache.values()) {
                updateSelectiveAtPos(entry);
            }
            Hollower.renderBlacklistCache.clear();
        }
    }

    public static void updateSelectiveAtPos(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        BlockState state = mc.world.getBlockState(pos);
        if (RenderTweaks.shouldHideBlock(pos)) {
            if (!state.isAir()) {
                mc.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.FORCE_STATE | PASSTHROUGH);
                setFakeBlockState(pos, state);
                Hollower.renderBlacklistCache.put(pos.asLong(), pos);
            }
        } else {
            if (state.isAir()) {
                BlockState originalState = fakeWorld.getBlockState(pos);
                if (!originalState.isAir()) {
                    fakeWorld.setBlockState(pos, Blocks.AIR.getDefaultState());
                    mc.world.setBlockState(pos, originalState, Block.NOTIFY_ALL | Block.FORCE_STATE | PASSTHROUGH);
                    Hollower.renderBlacklistCache.remove(pos.asLong());
                }
            }
        }
    }

    public static void setFakeBlockState(BlockPos pos, BlockState state) {
        fakeWorld.setBlockState(pos, state, 0);
    }
}
