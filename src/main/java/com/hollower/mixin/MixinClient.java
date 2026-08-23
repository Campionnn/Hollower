package com.hollower.mixin;

import com.hollower.Hollower;
import com.hollower.utils.PlayerUtils;
import com.hollower.utils.RouteUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public abstract class MixinClient {
    @Inject(at = @At("HEAD"), method = "startAttack", cancellable = true)
    private void hollower$startAttack(CallbackInfoReturnable<Boolean> callback) {
        if (!PlayerUtils.isHoldingTool()) return;
        RouteUtils.removePosition(RouteUtils.getNodeRaycast());
        callback.setReturnValue(false);
    }

    @Inject(at = @At("HEAD"), method = "startUseItem", cancellable = true)
    private void hollower$startUseItem(CallbackInfo callback) {
        if (!PlayerUtils.isHoldingTool()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        if (client.level.getGameTime() - Hollower.lastToolUseTick > 2) {
            if (Hollower.isKeyPressed(Hollower.etherwarpKey)) {
                PlayerUtils.etherwarp();
            } else {
                RouteUtils.addPosition(RouteUtils.getRaycast());
            }
            Hollower.lastToolUseTick = client.level.getGameTime();
        }
        callback.cancel();
    }

    @Inject(at = @At("HEAD"), method = "pickBlockOrEntity", cancellable = true)
    private void hollower$pickBlock(CallbackInfo callback) {
        if (!PlayerUtils.isHoldingTool()) return;

        if (Hollower.isKeyPressed(Hollower.swapOrderKey)) {
            RouteUtils.swapPositions(RouteUtils.getNodeRaycast());
        } else {
            RouteUtils.selectPosition(RouteUtils.getNodeRaycast());
        }
        callback.cancel();
    }
}
