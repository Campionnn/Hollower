package com.hollower.mixin;

import com.hollower.Hollower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Keyboard.class)
public abstract class MixinKeyboard {
    @Inject(at = @At("TAIL"), method = "onKey")
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        Hollower.onKeyEvent(action);
    }
}
