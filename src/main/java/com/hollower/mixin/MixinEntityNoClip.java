package com.hollower.mixin;

import com.hollower.utils.NoClipController;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntityNoClip {
    @Inject(method = "getGravity", at = @At("HEAD"), cancellable = true)
    private void hollower$removeNoClipGravity(CallbackInfoReturnable<Double> callbackInfo) {
        if (NoClipController.isNoClipping((Entity) (Object) this)) {
            callbackInfo.setReturnValue(0.0);
        }
    }
}
