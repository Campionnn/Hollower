package com.hollower.mixin;

import com.hollower.utils.NoClipController;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MixinPlayerNoClip {
    @Inject(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void hollower$restoreNoClipAfterVanillaReset(CallbackInfo callbackInfo) {
        NoClipController.onPlayerTick((Player) (Object) this);
    }

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void hollower$keepNoClipPoseUpright(CallbackInfo callbackInfo) {
        Player player = (Player) (Object) this;
        if (!NoClipController.isNoClipping(player)) return;

        player.setPose(Pose.STANDING);
        callbackInfo.cancel();
    }
}
