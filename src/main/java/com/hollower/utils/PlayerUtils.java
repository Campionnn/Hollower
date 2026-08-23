package com.hollower.utils;

import com.hollower.Hollower;
import com.hollower.render.SelectiveRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public final class PlayerUtils implements AttackBlockCallback {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    @Override
    public InteractionResult interact(
            Player player, Level level, InteractionHand hand, BlockPos pos, Direction direction) {
        return level.isClientSide() && isHoldingTool()
                ? InteractionResult.FAIL
                : InteractionResult.PASS;
    }

    public static boolean isHoldingTool() {
        return CLIENT.player != null && CLIENT.player.getMainHandItem().is(Items.WOODEN_PICKAXE);
    }

    public static Direction getClosestLookingDirection() {
        Entity entity = CLIENT.getCameraEntity();
        if (entity == null) return Direction.NORTH;
        if (entity.getXRot() > 60.0f) return Direction.DOWN;
        if (entity.getXRot() < -60.0f) return Direction.UP;
        return Direction.fromYRot(entity.getYRot());
    }

    public static BlockPos getEtherwarpBlock() {
        return RouteUtils.getRaycast(Hollower.etherwarpRange);
    }

    private static boolean isClear(BlockPos pos) {
        BlockState state = CLIENT.level.getBlockState(pos);
        return state.isAir() || SelectiveRender.isPassable(state);
    }

    public static void etherwarp() {
        BlockPos pos = getEtherwarpBlock();
        if (pos == null || CLIENT.level == null || CLIENT.getConnection() == null) return;

        BlockPos feet = pos.above();
        BlockPos head = feet.above();
        if (!isClear(feet) || !isClear(head)) {
            Hollower.sendChatMessage("§cCannot teleport to that location");
            return;
        }
        Hollower.lastCommands.add("Teleported");
        CLIENT.getConnection().sendCommand(
                "tp " + feet.getX() + " " + feet.getY() + " " + feet.getZ());
        CLIENT.level.playLocalSound(
                feet.getX() + 0.5,
                feet.getY(),
                feet.getZ() + 0.5,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0f,
                1.0f,
                false);
    }
}
