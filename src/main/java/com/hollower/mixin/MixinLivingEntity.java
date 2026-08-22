package com.hollower.mixin;

import com.hollower.utils.NoClipController;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void hollower$handleNoClipTravel(Vec3 input, CallbackInfo callbackInfo) {
        if (NoClipController.handleTravel((LivingEntity) (Object) this)) {
            callbackInfo.cancel();
        }
    }
}
