package com.hollower.utils;

import com.hollower.Hollower;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.List;

public class RenderUtils {
    static Tessellator tessellator = Tessellator.getInstance();
    static BufferBuilder buffer = tessellator.getBuffer();
    static MatrixStack matrixStack = RenderSystem.getModelViewStack();
    static MinecraftClient client = MinecraftClient.getInstance();
    static Camera camera = client.gameRenderer.getCamera();

    /**
     * Multiplies and translates the current model view matrix to be relative to the world instead of the camera.
     */
    private static void correctView() {
        Vec3d cameraPos = camera.getPos();
        matrixStack.push();
        float angleX = camera.getPitch() * (float) (Math.PI / 180.0);
        float angleY = camera.getYaw() * (float) (Math.PI / 180.0);
        Quaternionf rot = new Quaternionf().rotationXYZ(angleX, angleY, 0.0F);
        matrixStack.multiply(rot);
        matrixStack.scale(-1.0F, 1.0F, -1.0F);
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
    }

    /**
     * Pops the model view matrix stack to revert the changes made by {@link #correctView()}.
     */
    private static void revertView() {
        RenderSystem.getModelViewStack().pop();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderFogEnd(96.0f);
    }

    /**
     * Draws a line strip between the given positions with the given color and thickness.
     *
     * @param positions  the positions to draw lines between
     * @param color      the color of the lines
     * @param thickness  the thickness of the lines
     * @param depthTest  whether to use depth testing when drawing the lines
     */
    public static void drawLines(List<BlockPos> positions, Color color, float thickness, boolean depthTest) {
        if (positions.isEmpty()) return;

        correctView();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(thickness);

        if (!depthTest) {
            RenderSystem.disableDepthTest();
        }

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        buffer.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (BlockPos pos : positions) {
            buffer.vertex(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d).color(r, g, b, a).next();
        }
        buffer.vertex(positions.get(0).getX() + 0.5d, positions.get(0).getY() + 0.5d, positions.get(0).getZ() + 0.5d).color(r, g, b, a).next();
        tessellator.draw();

        if (!depthTest) {
            RenderSystem.enableDepthTest();
        }

        revertView();
    }

    /**
     * Draws a line along each edge of the given block positions with the given color and thickness.
     *
     * @param positions the positions to draw lines between
     * @param color     the color of the lines
     * @param thickness the thickness of the lines
     * @param depthTest whether to use depth testing when drawing the lines
     */
    public static void highlightBlocks(List<BlockPos> positions, Color color, float thickness, boolean depthTest) {
        if (positions.isEmpty()) return;

        correctView();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(thickness);

        if (!depthTest) {
            RenderSystem.disableDepthTest();
        }

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        buffer.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        for (BlockPos pos : positions) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            // min corner
            buffer.vertex(x, y, z).color(r, g, b, a).next();
            buffer.vertex(x + 1, y, z).color(r, g, b, a).next();

            buffer.vertex(x, y, z).color(r, g, b, a).next();
            buffer.vertex(x, y + 1, z).color(r, g, b, a).next();

            buffer.vertex(x, y, z).color(r, g, b, a).next();
            buffer.vertex(x, y, z + 1).color(r, g, b, a).next();

            // max corner
            buffer.vertex(x + 1, y + 1, z + 1).color(r, g, b, a).next();
            buffer.vertex(x, y + 1, z + 1).color(r, g, b, a).next();

            buffer.vertex(x + 1, y + 1, z + 1).color(r, g, b, a).next();
            buffer.vertex(x + 1, y, z + 1).color(r, g, b, a).next();

            buffer.vertex(x + 1, y + 1, z + 1).color(r, g, b, a).next();
            buffer.vertex(x + 1, y + 1, z).color(r, g, b, a).next();

            // rest of the edges
            buffer.vertex(x + 1, y, z).color(r, g, b, a).next();
            buffer.vertex(x + 1, y, z + 1).color(r, g, b, a).next();

            buffer.vertex(x, y + 1, z).color(r, g, b, a).next();
            buffer.vertex(x, y + 1, z + 1).color(r, g, b, a).next();

            buffer.vertex(x, y, z + 1).color(r, g, b, a).next();
            buffer.vertex(x + 1, y, z + 1).color(r, g, b, a).next();

            buffer.vertex(x, y + 1, z).color(r, g, b, a).next();
            buffer.vertex(x + 1, y + 1, z).color(r, g, b, a).next();

            buffer.vertex(x, y, z + 1).color(r, g, b, a).next();
            buffer.vertex(x, y + 1, z + 1).color(r, g, b, a).next();

            buffer.vertex(x + 1, y, z).color(r, g, b, a).next();
            buffer.vertex(x + 1, y + 1, z).color(r, g, b, a).next();
        }
        tessellator.draw();

        if (!depthTest) {
            RenderSystem.enableDepthTest();
        }

        revertView();
    }

    /**
     * Highlight the sides of the block at the given position with the given color.
     *
     * @param pos       the position of the block
     * @param color     the color of the block
     * @param depthTest whether to use depth testing when drawing the block
     */
    public static void selectBlock(BlockPos pos, Color color, boolean depthTest) {
        if (pos == null) return;

        correctView();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        if (!depthTest) {
            RenderSystem.disableDepthTest();
        }

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        // west side
        buffer.vertex(pos.getX(), pos.getY(), pos.getZ()).color(r, g, b, a).next();
        buffer.vertex(pos.getX(), pos.getY(), pos.getZ() + 1).color(r, g, b, a).next();
        buffer.vertex(pos.getX(), pos.getY() + 1, pos.getZ() + 1).color(r, g, b, a).next();
        buffer.vertex(pos.getX(), pos.getY() + 1, pos.getZ()).color(r, g, b, a).next();

        // east side
        buffer.vertex(pos.getX() + 1, pos.getY(), pos.getZ()).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY(), pos.getZ() + 1).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY() + 1, pos.getZ()).color(r, g, b, a).next();

        // north side
        buffer.vertex(pos.getX(), pos.getY(), pos.getZ()).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY(), pos.getZ()).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY() + 1, pos.getZ()).color(r, g, b, a).next();
        buffer.vertex(pos.getX(), pos.getY() + 1, pos.getZ()).color(r, g, b, a).next();

        // south side
        buffer.vertex(pos.getX(), pos.getY(), pos.getZ() + 1).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY(), pos.getZ() + 1).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).color(r, g, b, a).next();
        buffer.vertex(pos.getX(), pos.getY() + 1, pos.getZ() + 1).color(r, g, b, a).next();

        // top side
        buffer.vertex(pos.getX(), pos.getY() + 1, pos.getZ()).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY() + 1, pos.getZ()).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).color(r, g, b, a).next();
        buffer.vertex(pos.getX(), pos.getY() + 1, pos.getZ() + 1).color(r, g, b, a).next();

        // bottom side
        buffer.vertex(pos.getX(), pos.getY(), pos.getZ()).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY(), pos.getZ()).color(r, g, b, a).next();
        buffer.vertex(pos.getX() + 1, pos.getY(), pos.getZ() + 1).color(r, g, b, a).next();
        buffer.vertex(pos.getX(), pos.getY(), pos.getZ() + 1).color(r, g, b, a).next();
        tessellator.draw();

        if (!depthTest) {
            RenderSystem.enableDepthTest();
        }

        revertView();
    }

    /**
     * Renders the order of the given positions above them.
     *
     * @param positions the positions to render the order of
     */
    public static void renderOrder(List<BlockPos> positions) {
        if (positions.isEmpty()) return;

        TextRenderer textRenderer = client.textRenderer;
        Vec3d cameraPos = camera.getPos();

        float angleX = camera.getPitch() * (float) (Math.PI / 180.0);
        float angleY = (camera.getYaw() + 180) * (float) (Math.PI / 180.0);
        Quaternionf rot = new Quaternionf().rotationXYZ(angleX, angleY, 0.0F);

        for (BlockPos pos : positions) {
            matrixStack.push();
            matrixStack.multiply(rot);
            matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            matrixStack.translate(pos.getX()+0.5, pos.getY()+1.5, pos.getZ()+0.5);
            matrixStack.multiply(camera.getRotation());
            matrixStack.scale(-0.04F, -0.04f, 0.04f);

            Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
            VertexConsumerProvider consumers = client.getBufferBuilders().getOutlineVertexConsumers();
            textRenderer.draw(String.valueOf(positions.indexOf(pos)+1), -textRenderer.getWidth(String.valueOf(positions.indexOf(pos)+1)) / 2.0f, 0f, new Color(255, 255, 255).getRGB(), false, positionMatrix, consumers, TextRenderer.TextLayerType.NORMAL, new Color(0, 0, 0, 70).getRGB(), LightmapTextureManager.MAX_LIGHT_COORDINATE);

            matrixStack.pop();
        }
    }
}
