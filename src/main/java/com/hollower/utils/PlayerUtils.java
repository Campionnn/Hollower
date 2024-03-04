package com.hollower.utils;

import com.hollower.Hollower;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PlayerUtils implements AttackBlockCallback {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (world.isClient) {
            if (isHoldingTool()) {
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }

    /**
     * Check if the player is holding a wooden pickaxe
     * @return true if the player is holding a wooden pickaxe
     */
    public static boolean isHoldingTool() {
        if (client.player == null) return false;
        return client.player.getMainHandStack().getItem() == Items.WOODEN_PICKAXE;
    }

    /**
     * Get the closest looking direction of the player
     * @return the closest looking direction of the player
     */
    public static Direction getClosestLookingDirection() {
        Entity entity = client.getCameraEntity();
        if (entity.getPitch() > 60.0f)
        {
            return Direction.DOWN;
        }
        else if (-entity.getPitch() > 60.0f)
        {
            return Direction.UP;
        }

        return Direction.fromRotation(entity.getYaw());
    }

    public static void etherwarp() {
        BlockPos pos = RouteUtils.getRaycast(61);
        if (pos != null) {
            if (!client.world.getBlockState(pos.offset(Direction.UP)).isAir() || !client.world.getBlockState(pos.offset(Direction.UP).offset(Direction.UP)).isAir()) {
                client.player.sendMessage(Text.of("§cCannot teleport to that location"), false);
                return;
            }
            else if (Hollower.keysToggle.get(Hollower.toggleRenderKey)) {
                long pos1 = pos.offset(Direction.UP).asLong();
                long pos2 = pos.offset(Direction.UP).offset(Direction.UP).asLong();
                if (Hollower.renderBlacklistState.containsKey(pos1) || Hollower.renderBlacklistState.containsKey(pos2)) {
                    client.player.sendMessage(Text.of("§cCannot teleport to that location due to unrendered blocks"), false);
                    return;
                }
            }
            BlockPos teleportPos = pos.offset(Direction.UP);
            Hollower.lastTeleportPos = teleportPos;
            client.getNetworkHandler().sendChatCommand("tp " + teleportPos.getX() + " " + teleportPos.getY() + " " + teleportPos.getZ());
            client.world.playSound(teleportPos.getX() + 0.5d, teleportPos.getY(), teleportPos.getZ() + 0.5d, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f, false);
        }
    }
}
