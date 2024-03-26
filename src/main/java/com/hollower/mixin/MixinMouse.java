package com.hollower.mixin;

import com.hollower.Hollower;
import com.hollower.utils.PlayerUtils;
import com.hollower.utils.RouteUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Mouse.class)
public abstract class MixinMouse {
    @Final
    @Shadow
    private MinecraftClient client;

    @Inject(at = @At("HEAD"), method = "onMouseScroll", cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (client.getWindow().getHandle() != window) return;
        if (PlayerUtils.isHoldingTool()) {
            int amount = vertical > 0 ? 1 : -1;
            if (Hollower.isKeyPressed(Hollower.config.nudgeKey)) {
                RouteUtils.nudgePosition(Hollower.selected, amount);
                ci.cancel();
            }
            if (Hollower.isKeyPressed(Hollower.config.swapOrderKey) && Hollower.positions.size() > 1) {
                if (amount == -1) {
                    BlockPos last = Hollower.positions.remove(Hollower.positions.size() - 1);
                    Hollower.positions.add(0, last);
                }
                else {
                    BlockPos first = Hollower.positions.remove(0);
                    Hollower.positions.add(first);
                }
                ci.cancel();
            }
        }
    }
}
