package com.hollower.utils;

import com.hollower.Hollower;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.OptionalDouble;

@Environment(EnvType.CLIENT)
public final class RenderUtils {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final RenderType THROUGH_WALLS_LINES = RenderType.create(
            "hollower_through_walls_lines",
            RenderSetup.builder(RenderPipelines.register(
                            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                                    .withLocation(Identifier.fromNamespaceAndPath(
                                            Hollower.MOD_ID,
                                            "pipeline/through_walls_lines"
                                    ))
                                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                                    .build()
                    ))
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
    );
    private static final RenderType THROUGH_WALLS_QUADS = RenderType.create(
            "hollower_through_walls_quads",
            RenderSetup.builder(RenderPipelines.register(
                            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                                    .withLocation(Identifier.fromNamespaceAndPath(
                                            Hollower.MOD_ID,
                                            "pipeline/through_walls_quads"
                                    ))
                                    .withCull(false)
                                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                                    .build()
                    ))
                    .sortOnUpload()
                    .createRenderSetup()
    );
    private static final Identifier BARRIER_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/item/barrier.png");
    // GUI_TEXTURED_SNIPPET already wires up a textured, tinted, translucent quad (position + uv +
    // colour, Sampler0 bound) - only depth testing and culling need to change for a through-walls quad.
    private static final RenderPipeline REGION_BORDER_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(Hollower.MOD_ID, "pipeline/region_borders"))
                    .withCull(false)
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .build()
    );
    private static GpuSampler regionBorderSampler;
    private static final RenderType REGION_BORDERS = RenderType.create(
            "hollower_region_borders",
            RenderSetup.builder(REGION_BORDER_PIPELINE)
                    .withTexture("Sampler0", BARRIER_TEXTURE, RenderUtils::regionBorderSampler)
                    .sortOnUpload()
                    .createRenderSetup()
    );

    private RenderUtils() {
    }

    public static void initialize() {
        // Static initialization registers the custom render pipelines.
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

        if (Hollower.renderRegionBorders) {
            renderRegionBorders(collector, poseStack);
        }

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
            poseStack.scale(scale, -scale, scale);
            collector.submitText(
                    poseStack,
                    -Hollower.client.font.width(label) / 2.0f,
                    0.0f,
                    Component.literal(label).getVisualOrderText(),
                    false,
                    Font.DisplayMode.SEE_THROUGH,
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
            int color,
            float width
    ) {
        if (positions.size() < 2) return;

        submit(collector, poseStack, THROUGH_WALLS_LINES, (pose, consumer) -> {
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
            int color,
            float width
    ) {
        if (positions.isEmpty()) return;

        submit(collector, poseStack, THROUGH_WALLS_LINES, (pose, consumer) -> {
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
            int color
    ) {
        if (pos == null) return;

        submit(collector, poseStack, THROUGH_WALLS_QUADS, (pose, consumer) -> {
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

    // World bounds run from (201, 30, 201) to (824, 189, 824). The border planes mark the horizontal
    // extent at sea level, plus two crossing vertical planes near the center that only rise above it.
    private static final float WORLD_MIN = 201.0f;
    private static final float WORLD_MAX = 824.0f;
    private static final float WORLD_BORDER_Y = 64.0f;
    private static final float WORLD_TOP_Y = 189.0f;
    // The X-fixed plane isn't perfectly centered: past the Z-fixed plane (in the positive Z direction)
    // it sits one block further out, so the crossing seam splits it into two quads.
    private static final float CROSS_X_NEAR = 512.0f;
    private static final float CROSS_X_FAR = 513.0f;
    private static final float CROSS_Z = 514.0f;
    // One barrier icon per block, so the tiled texture reads as a clear grid along the boundary.
    private static final float TILE = 1.0f;

    private static GpuSampler regionBorderSampler() {
        if (regionBorderSampler == null) {
            regionBorderSampler = RenderSystem.getDevice().createSampler(
                    AddressMode.REPEAT, AddressMode.REPEAT,
                    FilterMode.NEAREST, FilterMode.NEAREST,
                    1, OptionalDouble.empty());
        }
        return regionBorderSampler;
    }

    private static void renderRegionBorders(
            SubmitNodeCollector collector,
            PoseStack poseStack
    ) {
        // Always white - only the opacity is configurable.
        int alpha = Math.clamp(Math.round(Hollower.regionBorderOpacity * 255.0f), 0, 255);
        int color = (alpha << 24) | 0xFFFFFF;
        submit(collector, poseStack, REGION_BORDERS, (pose, consumer) -> {
            // Horizontal plane at y=64, spanning the whole world footprint.
            texturedQuad(pose, consumer, color,
                    WORLD_MIN, WORLD_BORDER_Y, WORLD_MIN, WORLD_MIN / TILE, WORLD_MIN / TILE,
                    WORLD_MAX, WORLD_BORDER_Y, WORLD_MIN, WORLD_MAX / TILE, WORLD_MIN / TILE,
                    WORLD_MAX, WORLD_BORDER_Y, WORLD_MAX, WORLD_MAX / TILE, WORLD_MAX / TILE,
                    WORLD_MIN, WORLD_BORDER_Y, WORLD_MAX, WORLD_MIN / TILE, WORLD_MAX / TILE);

            // Vertical plane fixed at z=514, spanning x and rising from y=64 to the top of the world.
            texturedQuad(pose, consumer, color,
                    WORLD_MIN, WORLD_BORDER_Y, CROSS_Z, WORLD_MIN / TILE, WORLD_BORDER_Y / TILE,
                    WORLD_MAX, WORLD_BORDER_Y, CROSS_Z, WORLD_MAX / TILE, WORLD_BORDER_Y / TILE,
                    WORLD_MAX, WORLD_TOP_Y, CROSS_Z, WORLD_MAX / TILE, WORLD_TOP_Y / TILE,
                    WORLD_MIN, WORLD_TOP_Y, CROSS_Z, WORLD_MIN / TILE, WORLD_TOP_Y / TILE);

            // Vertical X-fixed plane, split at the z=514 seam: x=512 on the near side, x=513 beyond it.
            texturedQuad(pose, consumer, color,
                    CROSS_X_NEAR, WORLD_BORDER_Y, WORLD_MIN, WORLD_MIN / TILE, WORLD_BORDER_Y / TILE,
                    CROSS_X_NEAR, WORLD_BORDER_Y, CROSS_Z, CROSS_Z / TILE, WORLD_BORDER_Y / TILE,
                    CROSS_X_NEAR, WORLD_TOP_Y, CROSS_Z, CROSS_Z / TILE, WORLD_TOP_Y / TILE,
                    CROSS_X_NEAR, WORLD_TOP_Y, WORLD_MIN, WORLD_MIN / TILE, WORLD_TOP_Y / TILE);

            texturedQuad(pose, consumer, color,
                    CROSS_X_FAR, WORLD_BORDER_Y, CROSS_Z, CROSS_Z / TILE, WORLD_BORDER_Y / TILE,
                    CROSS_X_FAR, WORLD_BORDER_Y, WORLD_MAX, WORLD_MAX / TILE, WORLD_BORDER_Y / TILE,
                    CROSS_X_FAR, WORLD_TOP_Y, WORLD_MAX, WORLD_MAX / TILE, WORLD_TOP_Y / TILE,
                    CROSS_X_FAR, WORLD_TOP_Y, CROSS_Z, CROSS_Z / TILE, WORLD_TOP_Y / TILE);
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
            int color,
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
            int color,
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

    private static void texturedQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int color,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float x4, float y4, float z4, float u4, float v4
    ) {
        texturedVertex(pose, consumer, x1, y1, z1, u1, v1, color);
        texturedVertex(pose, consumer, x2, y2, z2, u2, v2, color);
        texturedVertex(pose, consumer, x3, y3, z3, u3, v3, color);
        texturedVertex(pose, consumer, x4, y4, z4, u4, v4, color);
    }

    private static void texturedVertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            int color
    ) {
        vertex(pose, consumer, x, y, z, color).setUv(u, v);
    }

    private static VertexConsumer vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            int color
    ) {
        return consumer.addVertex(pose, x, y, z)
                .setColor(
                        (color >> 16) & 0xFF,
                        (color >> 8) & 0xFF,
                        color & 0xFF,
                        (color >>> 24) & 0xFF);
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
