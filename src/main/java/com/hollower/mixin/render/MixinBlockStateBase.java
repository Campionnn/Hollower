package com.hollower.mixin.render;

import com.hollower.render.SelectiveRender;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// All of selective render, expressed as answers a hidden block gives about itself, which is what makes
// it work under Sodium's replacement chunk mesher too. Model, occlusion and solidity all have to be
// dropped together, or a hidden wall would look like a hole into the void instead of an opening.
// Collision and outline are dropped more narrowly: collision only for a Player in a local world, and
// outline only for queries from the ClientLevel, so server-side physics and picking are unaffected.
@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinBlockStateBase {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void hollower$hideModel(CallbackInfoReturnable<RenderShape> callback) {
        if (SelectiveRender.isHidden((BlockState) (Object) this)) {
            callback.setReturnValue(RenderShape.INVISIBLE);
        }
    }

    // Without this, the visible blocks touching a hidden one cull the faces against it.
    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void hollower$stopOccludingFaces(CallbackInfoReturnable<Boolean> callback) {
        if (SelectiveRender.isHidden((BlockState) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    // Without this, the sections behind a hidden wall stay culled and you see void through it.
    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true)
    private void hollower$stopOccludingSections(CallbackInfoReturnable<Boolean> callback) {
        if (SelectiveRender.isHidden((BlockState) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/phys/shapes/CollisionContext;)"
                    + "Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hollower$passThroughCollision(
            BlockGetter blockGetter,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> callback
    ) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player
                && SelectiveRender.isPassable((BlockState) (Object) this)) {
            callback.setReturnValue(Shapes.empty());
        }
    }

    @Inject(
            method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/phys/shapes/CollisionContext;)"
                    + "Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hollower$passThroughOutline(
            BlockGetter blockGetter,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> callback
    ) {
        if (blockGetter instanceof ClientLevel && SelectiveRender.isHidden((BlockState) (Object) this)) {
            callback.setReturnValue(Shapes.empty());
        }
    }
}
