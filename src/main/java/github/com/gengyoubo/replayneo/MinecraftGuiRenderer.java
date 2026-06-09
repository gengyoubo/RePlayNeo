/*
 * This file is part of jGui API, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package github.com.gengyoubo.replayneo;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.replayneo.api.render.GuiRenderer;
import github.com.gengyoubo.replayneo.api.NonNull;
import de.johni0702.minecraft.gui.utils.lwjgl.Color;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import de.johni0702.minecraft.gui.utils.lwjgl.WritableDimension;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.*;
import static org.lwjgl.opengl.GL11.*;

public class MinecraftGuiRenderer implements GuiRenderer {

    private final Minecraft mc = getMinecraft();

    private final GuiGraphics context;

    private final PoseStack matrixStack;

    @NonNull
    private final int scaledWidth = newScaledResolution(mc).getGuiScaledWidth();
    private final int scaledHeight = newScaledResolution(mc).getGuiScaledHeight();
    private final double scaleFactor = newScaledResolution(mc).getGuiScale();

    public MinecraftGuiRenderer(GuiGraphics context) {
        this.context = context;
        this.matrixStack = context.pose();
    }

    @Override
    public ReadablePoint getOpenGlOffset() {
        return new Point(0, 0);
    }

    @Override
    public GuiGraphics getContext() {
        return context;
    }

    @Override
    public PoseStack getMatrixStack() {
        return matrixStack;
    }

    @Override
    public ReadableDimension getSize() {
        return new ReadableDimension() {
            @Override
            public int getWidth() {
                return scaledWidth;
            }

            @Override
            public int getHeight() {
                return scaledHeight;
            }

            @Override
            public void getSize(WritableDimension dest) {
                dest.setSize(getWidth(), getHeight());
            }
        };
    }

    @Override
    public void setDrawingArea(int x, int y, int width, int height) {
        // glScissor origin is bottom left corner whereas otherwise it's top left
        y = scaledHeight - y - height;

        int f = (int) scaleFactor;
        MCVer.setScissorBounds(x * f, y * f, width * f, height * f);
    }

    private ResourceLocation boundTexture;
    private int boundTextureGpu;

    @Override
    public void bindTexture(Object location) {
        if (!(location instanceof ResourceLocation resourceLocation)) {
            throw new IllegalArgumentException("Expected ResourceLocation texture, got " + location);
        }
        boundTexture = resourceLocation;
        boundTextureGpu = 0;
    }

    @Override
    public void bindTexture(int glId) {
        boundTexture = null;
        boundTextureGpu = glId;
    }


    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height) {
        drawTexturedRect(x, y, u, v, width, height, width, height, 256, 256);
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        color(1, 1);

        if (boundTexture != null) {
            MCVer.bindTexture(boundTexture);
        } else {
            RenderSystem.setShaderTexture(0, boundTextureGpu);
        }

        drawTexturedRect(x, x + width, y, y + height, u / (float) textureWidth, (u + uWidth) / (float) textureWidth, v / (float) textureHeight, (v + vHeight) / (float) textureHeight);
    }

    private void drawTexturedRect(int x1, int x2, int y1, int y2, float u1, float u2, float v1, float v2) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = matrixStack.last().pose();
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, x1, y1, 0).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix, x1, y2, 0).uv(u1, v2).endVertex();
        bufferBuilder.vertex(matrix, x2, y2, 0).uv(u2, v2).endVertex();
        bufferBuilder.vertex(matrix, x2, y1, 0).uv(u2, v1).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + height, color);
        color(1, 1);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
    }

    @Override
    public void drawRect(int x, int y, int width, int height, ReadableColor color) {
        drawRect(x, y, width, height, color(color));
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        drawRect(x, y, width, height, color(topLeftColor), color(topRightColor), color(bottomLeftColor), color(bottomRightColor));
    }

    @Override
    public void drawRect(int x, int y, int width, int height, ReadableColor tl, ReadableColor tr, ReadableColor bl, ReadableColor br) {
        drawRect(x, y, width, height, tl, tr, bl, br, false);
    }

    private void drawRect(int x, int y, int width, int height, ReadableColor tl, ReadableColor tr, ReadableColor bl, ReadableColor br, boolean highlight) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuilder();
        vertexBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        vertexBuffer.vertex(x, y + height, 0).color(bl.getRed(), bl.getGreen(), bl.getBlue(), bl.getAlpha()).endVertex();
        vertexBuffer.vertex(x + width, y + height, 0).color(br.getRed(), br.getGreen(), br.getBlue(), br.getAlpha()).endVertex();
        vertexBuffer.vertex(x + width, y, 0).color(tr.getRed(), tr.getGreen(), tr.getBlue(), tr.getAlpha()).endVertex();
        vertexBuffer.vertex(x, y, 0).color(tl.getRed(), tl.getGreen(), tl.getBlue(), tl.getAlpha()).endVertex();
        tessellator.end();

    }

    @Override
    public int drawString(int x, int y, int color, String text) {
        return drawString(x, y, color, text, false);
    }

    @Override
    public int drawString(int x, int y, ReadableColor color, String text) {
        return drawString(x, y, color(color), text);
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text) {
        return drawCenteredString(x, y, color, text, false);
    }

    @Override
    public int drawCenteredString(int x, int y, ReadableColor color, String text) {
        return drawCenteredString(x, y, color(color), text);
    }

    @Override
    public int drawString(int x, int y, int color, String text, boolean shadow) {
        Font fontRenderer = MCVer.getFontRenderer();
        try {
            return context.drawString(fontRenderer, text, x, y, color, shadow);
        } finally {
            color(1, 1);
        }
    }

    @Override
    public int drawString(int x, int y, ReadableColor color, String text, boolean shadow) {
        return drawString(x, y, color(color), text, shadow);
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text, boolean shadow) {
        Font fontRenderer = MCVer.getFontRenderer();
        x-=fontRenderer.width(text) / 2;
        return drawString(x, y, color, text, shadow);
    }

    @Override
    public int drawCenteredString(int x, int y, ReadableColor color, String text, boolean shadow) {
        return drawCenteredString(x, y, color(color), text, shadow);
    }

    private int color(ReadableColor color) {
        return color.getAlpha() << 24
                | color.getRed() << 16
                | color.getGreen() << 8
                | color.getBlue();
    }

    private ReadableColor color(int color) {
        return new Color((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff, (color >> 24) & 0xff);
    }

    private void color(float r, float g) {
        RenderSystem.setShaderColor(r, g, (float) 1, 1);
    }

    @Override
    public void invertColors(int right, int bottom, int left, int top) {
        if (left >= right || top >= bottom) return;

        color(0, 0);
        com.mojang.blaze3d.systems.RenderSystem.enableColorLogicOp();
        com.mojang.blaze3d.systems.RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);

        drawRect(right, bottom, right - left, bottom - top, ReadableColor.WHITE, ReadableColor.WHITE, ReadableColor.WHITE, ReadableColor.WHITE, true);

        com.mojang.blaze3d.systems.RenderSystem.disableColorLogicOp();
        color(1, 1);
    }
}
