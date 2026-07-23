package com.hollower.mixin;

import com.hollower.Hollower;
import com.hollower.utils.PlayerUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public abstract class MixinMouse {
    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(at = @At("HEAD"), method = "onScroll", cancellable = true)
    private void hollower$onScroll(long window, double horizontal, double vertical, CallbackInfo callback) {
        if (minecraft.getWindow().handle() != window || !PlayerUtils.isHoldingTool()) return;

        int amount = vertical > 0 ? 1 : -1;
        if (Hollower.isKeyPressed(Hollower.nudgeKey) && Hollower.selected != null) {
            int index = Hollower.positions.indexOf(Hollower.selected);
            Hollower.selected = Hollower.selected.relative(PlayerUtils.getClosestLookingDirection(), amount);
            Hollower.positions.set(index, Hollower.selected);
            callback.cancel();
        }

        if (Hollower.isKeyPressed(Hollower.swapOrderKey) && Hollower.positions.size() > 1) {
            if (amount == -1) {
                BlockPos last = Hollower.positions.removeLast();
                Hollower.positions.addFirst(last);
            } else {
                BlockPos first = Hollower.positions.removeFirst();
                Hollower.positions.addLast(first);
            }
            callback.cancel();
        }
    }
}
