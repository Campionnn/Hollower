package com.hollower.mixin;

import com.hollower.Hollower;
import com.hollower.utils.PlayerUtils;
import com.hollower.utils.RouteUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MixinPlayer {
    @Shadow
    static MinecraftClient instance;

    @Inject(at = @At("HEAD"), method = "doAttack", cancellable = true)
    private void doAttack(CallbackInfoReturnable<Boolean> cir) {
        if (PlayerUtils.isHoldingTool()) {
            RouteUtils.removePosition(RouteUtils.getNodeRaycast());
            cir.setReturnValue(false);
        }
    }

    @Inject(at = @At("HEAD"), method = "doItemUse", cancellable = true)
    private void doItemUse(CallbackInfo ci) {
        if (PlayerUtils.isHoldingTool()) {
            // prevent spam if the use key is held down
            if (instance.world.getTime() - Hollower.lastToolUseTick > 2) {
                if (Hollower.isKeyPressed(Hollower.etherwarpKey)) {
                    PlayerUtils.etherwarp();
                }
                else {
                    RouteUtils.addPosition(RouteUtils.getRaycast());
                }
            }
            Hollower.lastToolUseTick = instance.world.getTime();
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "doItemPick", cancellable = true)
    private void doItemPick(CallbackInfo ci) {
        if (PlayerUtils.isHoldingTool()) {
            if (Hollower.isKeyPressed(Hollower.swapOrderKey)) {
                RouteUtils.swapPositions(RouteUtils.getNodeRaycast());
            }
            else {
                RouteUtils.selectPosition(RouteUtils.getNodeRaycast());
            }
            ci.cancel();
        }
    }
}