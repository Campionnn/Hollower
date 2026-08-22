package com.hollower.mixin;

import com.hollower.utils.FullBrightController;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void hollower$setFullBrightScale(
            LivingEntity entity,
            float partialTick,
            CallbackInfoReturnable<Float> callbackInfo
    ) {
        if (FullBrightController.isActive()) {
            callbackInfo.setReturnValue(1.0F);
        }
    }
}
