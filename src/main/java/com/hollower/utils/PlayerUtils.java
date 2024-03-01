package com.hollower.utils;

import com.hollower.Hollower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PlayerUtils implements AttackBlockCallback {
    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (world.isClient) {
            if (isHoldingTool()) {
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }

    private static final MinecraftClient client = MinecraftClient.getInstance();

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
}
