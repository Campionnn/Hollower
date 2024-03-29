package com.hollower.mixin.render;

import com.hollower.utils.RenderTweaks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(World.class)
public class MixinWorld {
    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> ci) {
        ChunkPos chunk = new ChunkPos(pos);
        if (RenderTweaks.shouldHideBlock(chunk.toLong(), pos.subtract(chunk.getStartPos()).asLong())) {
            if ((flags & RenderTweaks.PASSTHROUGH) != 0) {
                return;
            }
            RenderTweaks.setFakeBlockState(pos, state);
            ci.setReturnValue(false);
        }
    }
}
