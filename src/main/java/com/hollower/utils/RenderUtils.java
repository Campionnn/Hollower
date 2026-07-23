package com.hollower.utils;

import com.hollower.Hollower;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.shedaniel.math.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class RenderUtils {
    private static final int FULL_BRIGHT = 0x00F000F0;

    private RenderUtils() {
    }

    public static void render(Object context) {
        if (Hollower.client.level == null) return;

        PoseStack poseStack = getContextValue(context, PoseStack.class, "poseStack", "matrices");
        SubmitNodeCollector collector =
                getContextValue(context, SubmitNodeCollector.class, "submitNodeCollector", "commandQueue");
        Camera camera = ClientCompat.mainCamera(Hollower.client.gameRenderer);
        if (poseStack == null || collector == null || camera == null) return;

        Vec3 cameraPosition = camera.position();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        drawLines(collector, poseStack, Hollower.positions, Hollower.routeLineColor, Hollower.routeLineWidth);
        outlineBlocks(collector, poseStack, Hollower.positions, Hollower.outlineBlockColor, Hollower.outlineBlockWidth);
        selectBlock(collector, poseStack, Hollower.selected, Hollower.selectBlockColor);
        renderOrder(collector, poseStack, camera, Hollower.positions, Hollower.orderScale);

        if (PlayerUtils.isHoldingTool() && Hollower.isKeyPressed(Hollower.etherwarpKey)) {
            selectBlock(collector, poseStack, PlayerUtils.getEtherwarpBlock(), Hollower.etherwarpBlockColor);
        }

        poseStack.popPose();
    }

    private static void renderOrder(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            Camera camera,
            List<BlockPos> positions,
            float scale
    ) {
        if (positions.isEmpty() || scale <= 0.0f) return;

        for (int index = 0; index < positions.size(); index++) {
            BlockPos pos = positions.get(index);
            String label = Integer.toString(index + 1);
            poseStack.pushPose();
            poseStack.translate(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
            poseStack.mulPose(camera.rotation());
            poseStack.scale(-scale, -scale, scale);
            collector.submitText(
                    poseStack,
                    -Hollower.client.font.width(label) / 2.0f,
                    0.0f,
                    Component.literal(label).getVisualOrderText(),
                    false,
                    Font.DisplayMode.NORMAL,
                    FULL_BRIGHT,
                    0xFFFFFFFF,
                    0x46000000,
                    0
            );
            poseStack.popPose();
        }
    }

    private static void drawLines(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            List<BlockPos> positions,
            Color color,
            float width
    ) {
        if (positions.size() < 2) return;

        submit(collector, poseStack, RenderTypes.linesTranslucent(), (pose, consumer) -> {
            for (int index = 0; index < positions.size(); index++) {
                BlockPos start = positions.get(index);
                BlockPos end = positions.get((index + 1) % positions.size());
                line(
                        pose,
                        consumer,
                        start.getX() + 0.5f,
                        start.getY() + 0.5f,
                        start.getZ() + 0.5f,
                        end.getX() + 0.5f,
                        end.getY() + 0.5f,
                        end.getZ() + 0.5f,
                        color,
                        width
                );
            }
        });
    }

    private static void outlineBlocks(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            List<BlockPos> positions,
            Color color,
            float width
    ) {
        if (positions.isEmpty()) return;

        submit(collector, poseStack, RenderTypes.linesTranslucent(), (pose, consumer) -> {
            for (BlockPos pos : positions) {
                float minX = pos.getX();
                float minY = pos.getY();
                float minZ = pos.getZ();
                float maxX = minX + 1.0f;
                float maxY = minY + 1.0f;
                float maxZ = minZ + 1.0f;

                line(pose, consumer, minX, minY, minZ, maxX, minY, minZ, color, width);
                line(pose, consumer, minX, minY, minZ, minX, maxY, minZ, color, width);
                line(pose, consumer, minX, minY, minZ, minX, minY, maxZ, color, width);
                line(pose, consumer, maxX, maxY, maxZ, minX, maxY, maxZ, color, width);
                line(pose, consumer, maxX, maxY, maxZ, maxX, minY, maxZ, color, width);
                line(pose, consumer, maxX, maxY, maxZ, maxX, maxY, minZ, color, width);
                line(pose, consumer, maxX, minY, minZ, maxX, minY, maxZ, color, width);
                line(pose, consumer, minX, maxY, minZ, minX, maxY, maxZ, color, width);
                line(pose, consumer, minX, minY, maxZ, maxX, minY, maxZ, color, width);
                line(pose, consumer, minX, maxY, minZ, maxX, maxY, minZ, color, width);
                line(pose, consumer, minX, minY, maxZ, minX, maxY, maxZ, color, width);
                line(pose, consumer, maxX, minY, minZ, maxX, maxY, minZ, color, width);
            }
        });
    }

    private static void selectBlock(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            BlockPos pos,
            Color color
    ) {
        if (pos == null) return;

        submit(collector, poseStack, RenderTypes.debugQuads(), (pose, consumer) -> {
            float minX = pos.getX();
            float minY = pos.getY();
            float minZ = pos.getZ();
            float maxX = minX + 1.0f;
            float maxY = minY + 1.0f;
            float maxZ = minZ + 1.0f;

            quad(pose, consumer, color, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
            quad(pose, consumer, color, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ);
            quad(pose, consumer, color, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ);
            quad(pose, consumer, color, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
            quad(pose, consumer, color, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
            quad(pose, consumer, color, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ);
        });
    }

    private static void submit(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer renderer
    ) {
        collector.submitCustomGeometry(poseStack, renderType, renderer);
    }

    private static void line(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float startX,
            float startY,
            float startZ,
            float endX,
            float endY,
            float endZ,
            Color color,
            float width
    ) {
        float normalX = endX - startX;
        float normalY = endY - startY;
        float normalZ = endZ - startZ;
        float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (length > 0.0f) {
            normalX /= length;
            normalY /= length;
            normalZ /= length;
        }

        vertex(pose, consumer, startX, startY, startZ, color)
                .setNormal(pose, normalX, normalY, normalZ)
                .setLineWidth(width);
        vertex(pose, consumer, endX, endY, endZ, color)
                .setNormal(pose, normalX, normalY, normalZ)
                .setLineWidth(width);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Color color,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4
    ) {
        vertex(pose, consumer, x1, y1, z1, color);
        vertex(pose, consumer, x2, y2, z2, color);
        vertex(pose, consumer, x3, y3, z3, color);
        vertex(pose, consumer, x4, y4, z4, color);
    }

    private static VertexConsumer vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            Color color
    ) {
        return consumer.addVertex(pose, x, y, z)
                .setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private static <T> T getContextValue(Object context, Class<T> type, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = context.getClass().getMethod(methodName);
                method.setAccessible(true);
                Object value = method.invoke(context);
                if (type.isInstance(value)) return type.cast(value);
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException | InvocationTargetException exception) {
                Hollower.LOGGER.error("Could not read world render context method {}", methodName, exception);
                return null;
            }
        }

        Hollower.LOGGER.error("World render context does not expose any of {}", String.join(", ", methodNames));
        return null;
    }
}
