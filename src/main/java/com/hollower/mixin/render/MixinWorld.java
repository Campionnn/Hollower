package com.hollower.mixin.render;

import com.hollower.utils.RenderTweaks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.class)
public class MixinWorld {
    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hollower$setBlock(
            BlockPos pos,
            BlockState state,
            int flags,
            int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> callback
    ) {
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        BlockPos localPos = new BlockPos(
                pos.getX() - chunk.getMinBlockX(),
                pos.getY(),
                pos.getZ() - chunk.getMinBlockZ()
        );
        if (!RenderTweaks.shouldHideBlock(
                RenderTweaks.chunkToLong(chunk.getMinBlockX() >> 4, chunk.getMinBlockZ() >> 4),
                localPos.asLong()
        )) {
            return;
        }
        if ((flags & RenderTweaks.PASSTHROUGH) != 0) return;

        RenderTweaks.setFakeBlockState(pos, state);
        callback.setReturnValue(false);
    }
}
