package com.hollower.mixin.render;

import com.hollower.render.SelectiveRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Hides blocks that draw themselves outside the chunk mesh (signs, skulls, brewing stands, etc.),
// which MixinBlockStateBase alone can't hide since they aren't drawn from their model.
@Environment(EnvType.CLIENT)
@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
    @Inject(
            method = "getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)"
                    + "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hollower$hideBlockEntity(
            BlockEntity blockEntity,
            CallbackInfoReturnable<BlockEntityRenderer<?, ?>> callback
    ) {
        if (SelectiveRender.isHidden(blockEntity.getBlockState())) {
            callback.setReturnValue(null);
        }
    }
}
