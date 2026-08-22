package com.hollower.mixin;

import com.hollower.utils.FullBrightController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.LightmapRenderStateExtractor")
public class MixinFullBrightLightmap {
    @Redirect(
            method = "extract",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z",
                    ordinal = 0
            )
    )
    private boolean hollower$enableFullBright(LocalPlayer player, Holder<MobEffect> effect) {
        return FullBrightController.isActive() || player.hasEffect(effect);
    }
}
