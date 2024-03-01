package com.hollower.mixin;

import com.hollower.Hollower;
import com.hollower.utils.PlayerUtils;
import com.hollower.utils.RouteUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class PlayerMixin {
    @Shadow
    static MinecraftClient instance;

    @Inject(at = @At("HEAD"), method = "doAttack", cancellable = true)
    private void doAttack(CallbackInfoReturnable<Boolean> cir) {
        if (PlayerUtils.isHoldingTool()) {
            BlockPos pos = RouteUtils.getNodeRaycast();
            if (pos != null) {
                if (pos.equals(Hollower.selected)) {
                    Hollower.selected = null;
                }
                Hollower.positions.remove(pos);
            }

            cir.setReturnValue(false);
        }
    }

    @Inject(at = @At("HEAD"), method = "doItemUse", cancellable = true)
    private void doItemUse(CallbackInfo ci) {
        if (PlayerUtils.isHoldingTool()) {
            // prevent spam if use key is held down
            if (instance.world.getTime() - Hollower.lastToolUseTick > 2) {
                if (Hollower.keysHold.get(Hollower.etherwarpKey)) {
                    BlockPos pos = RouteUtils.getRaycast(61);
                    if (pos != null) {
                        BlockPos teleportPos = pos.offset(Direction.UP);
                        if (instance.world.getBlockState(pos.offset(Direction.UP)).isAir() && instance.world.getBlockState(pos.offset(Direction.UP).offset(Direction.UP)).isAir()) {
                            instance.getNetworkHandler().sendChatCommand("tp " + teleportPos.getX() + " " + teleportPos.getY() + " " + teleportPos.getZ());
                            instance.world.playSound(teleportPos.getX() + 0.5d, teleportPos.getY(), teleportPos.getZ() + 0.5d, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f, false);
                        }
                        else {
                            instance.player.sendMessage(Text.of("§cCannot teleport to that location"), false);
                        }
                    }
                }
                else {
                    BlockPos pos = RouteUtils.getRaycast();
                    if (pos != null) {
                        if (!Hollower.positions.contains(pos)) {
                            if (Hollower.selected != null) {
                                Hollower.positions.add(Hollower.positions.indexOf(Hollower.selected) + 1, pos);
                            } else {
                                Hollower.positions.add(pos);
                            }
                        }
                    }
                }
            }
            Hollower.lastToolUseTick = instance.world.getTime();
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "doItemPick", cancellable = true)
    private void doItemPick(CallbackInfo ci) {
        if (PlayerUtils.isHoldingTool()) {
            if (!Hollower.keysHold.get(Hollower.swapOrderKey)) {
                Hollower.selected = RouteUtils.getNodeRaycast();
            }
            else if (Hollower.selected != null)	{
                BlockPos pos = RouteUtils.getNodeRaycast();
                if (pos != null) {
                    int index1 = Hollower.positions.indexOf(Hollower.selected);
                    int index2 = Hollower.positions.indexOf(pos);
                    if (index1 != -1 && index2 != -1) {
                        Hollower.positions.set(index1, pos);
                        Hollower.positions.set(index2, Hollower.selected);
                    }
                    Hollower.selected = pos;
                }
            }
            ci.cancel();
        }
    }
}